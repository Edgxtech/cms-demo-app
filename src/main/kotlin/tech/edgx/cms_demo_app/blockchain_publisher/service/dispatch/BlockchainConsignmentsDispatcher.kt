package tech.edgx.cms_demo_app.blockchain_publisher.service.dispatch

import com.bloxbean.cardano.client.api.exception.ApiException
import org.cardanofoundation.lob.app.blockchain_publisher.domain.core.BlockchainPublishStatus
import org.cardanofoundation.lob.app.blockchain_publisher.domain.entity.txs.L1SubmissionData
import org.cardanofoundation.lob.app.blockchain_publisher.service.dispatch.DispatchingStrategy
import org.cardanofoundation.lob.app.blockchain_publisher.service.dispatch.ImmediateDispatchingStrategy
import org.cardanofoundation.lob.app.blockchain_publisher.service.transation_submit.TransactionSubmissionService
import org.cardanofoundation.lob.app.organisation.OrganisationPublicApi
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import tech.edgx.cms_demo_app.blockchain_publisher.domain.core.ConsignmentBlockchainTransactions
import tech.edgx.cms_demo_app.blockchain_publisher.repository.ConsignmentEntityRepositoryGateway
import tech.edgx.cms_demo_app.blockchain_publisher.service.tx.ConsignmentL1TransactionCreator
import tech.edgx.cms_demo_app.blockchain_publisher.service.event_publish.ConsignmentLedgerUpdatedEventPublisher
import tech.edgx.cms_demo_app.blockchain_publisher.domain.entity.consignments.ConsignmentEntity
import java.util.Optional

@Service
class BlockchainConsignmentsDispatcher(
    private val organisationPublicApi: OrganisationPublicApi,
    private val consignmentEntityRepositoryGateway: ConsignmentEntityRepositoryGateway,
    private val dispatchingStrategy: DispatchingStrategy<ConsignmentEntity> = ImmediateDispatchingStrategy(),
    private val consignmentL1TransactionCreator: ConsignmentL1TransactionCreator,
    private val transactionSubmissionService: TransactionSubmissionService,
    private val ledgerUpdatedEventPublisher: ConsignmentLedgerUpdatedEventPublisher,
    @Value("\${lob.blockchain_publisher.dispatcher.consignment.pullBatchSize:50}") private val pullConsignmentsBatchSize: Int = 50
) {

    private val log = LoggerFactory.getLogger(BlockchainConsignmentsDispatcher::class.java)

    @Transactional
    fun dispatchConsignments() {
        log.info("Polling for blockchain consignments to be sent to the blockchain...")

        for (organisation in organisationPublicApi.listAll()) {
            val organisationId = organisation.id.trim()
            val consignments = consignmentEntityRepositoryGateway.findConsignmentsByStatus(organisationId, pullConsignmentsBatchSize)
            val consignmentsCount = consignments.size

            log.debug("Dispatching consignments for organisationId: {}, consignment count: {}", organisationId, consignmentsCount)

            if (consignmentsCount > 0) {
                val toDispatch = dispatchingStrategy.apply(organisationId, consignments)
                dispatchConsignments(organisationId, toDispatch)
            }
        }

        log.info("Polling for blockchain consignments to be sent to the blockchain...done")
    }

    @Transactional
    fun dispatchConsignments(organisationId: String, consignmentEntities: MutableSet<ConsignmentEntity>) {
        log.info("Dispatching consignments for organisation: {}", organisationId)

        for (consignmentEntity in consignmentEntities) {
            dispatchConsignment(organisationId, consignmentEntity)
        }
    }

    @Transactional
    fun dispatchConsignment(organisationId: String, consignmentEntity: ConsignmentEntity) {
        log.info("Dispatching consignment for organisation: {}", organisationId)

        val consignmentBlockchainTransactionE = createAndSendBlockchainTransactions(consignmentEntity)
        if (consignmentBlockchainTransactionE.isEmpty) {
            log.info("No more consignments to dispatch for organisationId, success or error?, organisationId: {}", organisationId)
        }
    }

    @Transactional
    fun createAndSendBlockchainTransactions(consignmentEntity: ConsignmentEntity): Optional<ConsignmentBlockchainTransactions> {
        log.info("Creating and sending blockchain transactions for consignment: {}", consignmentEntity.consignmentId)

        val serialisedTxE = consignmentL1TransactionCreator.pullBlockchainTransaction(
            consignmentEntity.sender.id, setOf(consignmentEntity)
        )

        if (serialisedTxE.isLeft) {
            log.error("Error pulling blockchain transaction, problem: {}", serialisedTxE.left.detail)
            return Optional.empty()
        }

        val serialisedTx = serialisedTxE.get().orElse(null) ?: return Optional.empty()
        try {
            sendTransactionOnChainAndUpdateDb(serialisedTx)
            return	Optional.of(serialisedTx)
        } catch (e: ApiException) {
            log.error("Error sending transaction on chain and/or updating db", e)
        }

        return Optional.empty()
    }

    @Transactional
    @Throws(ApiException::class)
    fun sendTransactionOnChainAndUpdateDb(consignmentBlockchainTransaction: ConsignmentBlockchainTransactions) {
        val consignmentTxData = consignmentBlockchainTransaction.txBytes
        val l1SubmissionData = transactionSubmissionService.submitTransactionWithPossibleConfirmation(
            consignmentTxData, consignmentBlockchainTransaction.organiserAddress
        )

        val txHash = l1SubmissionData.txHash
        val txAbsoluteSlotM = l1SubmissionData.absoluteSlot
        val creationSlot = consignmentBlockchainTransaction.creationSlot

        consignmentBlockchainTransaction.processedConsignments.forEach { consignment ->
            updateTransactionStatuses(txHash, txAbsoluteSlotM, creationSlot, consignment)
            ledgerUpdatedEventPublisher.sendConsignmentLedgerUpdatedEvents(
                consignment.sender.id, setOf(consignment)
            )
        }

        log.info("Blockchain transaction submitted (consignment), l1SubmissionData: {}", l1SubmissionData)
    }

    @Transactional
    fun updateTransactionStatuses(
        txHash: String,
        absoluteSlot: Optional<Long>,
        creationSlot: Long,
        consignmentEntity: ConsignmentEntity
    ) {
        consignmentEntity.setL1SubmissionData(
            Optional.of(
                L1SubmissionData.builder()
                    .transactionHash(txHash)
                    .absoluteSlot(absoluteSlot.orElse(null))
                    .creationSlot(creationSlot)
                    .publishStatus(BlockchainPublishStatus.SUBMITTED)
                    .build()
            )
        )

        consignmentEntityRepositoryGateway.storeConsignment(consignmentEntity)
    }
}