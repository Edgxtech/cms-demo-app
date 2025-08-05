package tech.edgx.cms_demo_app.blockchain_reader.service

import io.vavr.control.Either
import jakarta.annotation.PostConstruct
import org.cardanofoundation.lob.app.blockchain_common.domain.ChainTip
import org.cardanofoundation.lob.app.blockchain_common.domain.FinalityScore
import org.cardanofoundation.lob.app.blockchain_common.domain.OnChainTxDetails
import org.cardanofoundation.lob.app.blockchain_publisher.domain.core.BlockchainPublishStatus
import org.cardanofoundation.lob.app.blockchain_publisher.domain.core.OnChainStatus
import org.cardanofoundation.lob.app.blockchain_publisher.domain.entity.txs.L1SubmissionData
import org.cardanofoundation.lob.app.blockchain_publisher.service.BlockchainPublishStatusMapper
import org.cardanofoundation.lob.app.blockchain_reader.BlockchainReaderPublicApiIF
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.Limit
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.zalando.problem.Problem
import tech.edgx.cms_demo_app.blockchain_publisher.repository.ConsignmentEntityRepositoryGateway
import tech.edgx.cms_demo_app.blockchain_publisher.service.event_publish.ConsignmentLedgerUpdatedEventPublisher
import java.util.Optional

@Service("cms_demo_app.watchDogService")
class WatchDogService(
    private val blockchainPublishStatusMapper: BlockchainPublishStatusMapper,
    private val blockchainReaderPublicApi: BlockchainReaderPublicApiIF,
    private val consignmentEntityRepositoryGateway: ConsignmentEntityRepositoryGateway,
    private val ledgerUpdatedEventPublisher: ConsignmentLedgerUpdatedEventPublisher
) {
    private val log = LoggerFactory.getLogger(WatchDogService::class.java)

    @Value("\${lob.blockchain_publisher.watchdog.rollback.grace.period.minutes:15}")
    private val rollbackGracePeriodMinutes = 15

    @PostConstruct
    fun init() {
        log.info("ConsignmentWatchDogService configuration: rollbackGracePeriodMinutes={}", rollbackGracePeriodMinutes)
        log.info("ConsignmentWatchDogService started")
    }

    @Transactional
    fun checkConsignmentStatusForOrganisations(txStatusInspectionLimitPerOrgPullSize: Int) {
        val chainTip: ChainTip = this.chainTip
        if (!chainTip.isSynced()) {
            log.info("Chain is not synced, skipping consignment status check")
            return
        }

        val successfullyUpdatedConsignmentEntities =
            consignmentEntityRepositoryGateway.findAllDispatchedConsignmentsThatAreNotFinalizedYet(
                Limit.of(txStatusInspectionLimitPerOrgPullSize)
            )

        successfullyUpdatedConsignmentEntities.forEach { consignment ->
            log.debug("Checking consignment status for consignment: {}", consignment.consignmentId)
            val l1SubmissionData = consignment.getL1SubmissionData()
                .orElseThrow { RuntimeException("Failed to get L1 submission data") }
            consignment.setL1SubmissionData(
                Optional.of(
                    updateL1SubmissionData(
                        l1SubmissionData,
                        chainTip
                    )
                )
            )
        }

        consignmentEntityRepositoryGateway.storeConsignments(successfullyUpdatedConsignmentEntities)

        log.info("Status updated for {} consignments", successfullyUpdatedConsignmentEntities.size)

        // Group consignments by organisationId for event publishing
        val consignmentsByOrg = successfullyUpdatedConsignmentEntities.groupBy { it.sender.id }
        consignmentsByOrg.forEach { (orgId, consignments) ->
            log.debug("Publishing ledger updated events for organisation: {}", orgId)
            ledgerUpdatedEventPublisher.sendConsignmentLedgerUpdatedEvents(orgId, consignments.toSet())
        }
    }

    private fun getOnChainStatus(
        onChainTxDetails: Optional<OnChainTxDetails>,
        txCreationSlot: Long,
        chainTip: ChainTip
    ): OnChainStatus {
        if (onChainTxDetails.isPresent) {
            log.debug("onchaintxdetails is present, finality score: {}", onChainTxDetails.get().finalityScore)
            return OnChainStatus(
                blockchainPublishStatusMapper.convert(onChainTxDetails.get().finalityScore),
                Optional.of(onChainTxDetails.get().finalityScore)
            )
        } else {
            log.debug("onchaintxdetails is NOT present")
            val txAgeInSlots: Long = chainTip.absoluteSlot - txCreationSlot
            val isRollbackReadyTimewise = txAgeInSlots > (rollbackGracePeriodMinutes * 60L)
            if (isRollbackReadyTimewise) {
                return OnChainStatus(BlockchainPublishStatus.ROLLBACKED, Optional.empty())
            } else {
                return OnChainStatus(
                    BlockchainPublishStatus.SUBMITTED,
                    Optional.of(FinalityScore.VERY_LOW)
                )
            }
        }
    }

    private fun updateL1SubmissionData(submissionData: L1SubmissionData, chainTip: ChainTip): L1SubmissionData {
        val txCreationSlot = submissionData.creationSlot
            .orElseThrow { RuntimeException("Failed to get consignment creation slot") }
        val txHash = submissionData.transactionHash
            .orElseThrow { RuntimeException("Failed to get consignment hash") }
        log.debug("Checking consignment status changes for txHash:{}", txHash)
        val txDetails: Either<Problem?, Optional<OnChainTxDetails?>> = blockchainReaderPublicApi.getTxDetails(txHash)

        val onChainTxDetails: Optional<OnChainTxDetails> = txDetails.fold(
            { problem: Problem? ->
                log.error("Failed to get consignment details for txHash:{}", txHash)
                throw RuntimeException("Failed to get consignment details for txHash:$txHash, problem: $problem")
            },
            { optionalDetails ->
                if (optionalDetails.isPresent) {
                    Optional.of(optionalDetails.get())
                } else {
                    Optional.empty()
                }
            }
        )
        log.debug("Onchain Tx Details: {}", onChainTxDetails)
        val onChainStatus = getOnChainStatus(onChainTxDetails, txCreationSlot, chainTip)

        if (onChainStatus.status() == BlockchainPublishStatus.ROLLBACKED) {
            submissionData.setPublishStatus(BlockchainPublishStatus.ROLLBACKED)
            submissionData.setCreationSlot(null)
            submissionData.setAbsoluteSlot(null)
            submissionData.setTransactionHash(null)
            submissionData.setFinalityScore(null)
        } else {
            submissionData.setFinalityScore(onChainStatus.finalityScore().get())
            submissionData.setPublishStatus(onChainStatus.status())
        }
        return submissionData
    }

    private val chainTip: ChainTip
        get() = blockchainReaderPublicApi.chainTip
            .getOrElseThrow { it ->
                log.error("Failed to get chain tip")
                RuntimeException("Failed to get chain tip")
            }
}