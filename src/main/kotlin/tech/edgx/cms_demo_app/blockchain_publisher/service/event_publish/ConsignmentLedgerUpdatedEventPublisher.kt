package tech.edgx.cms_demo_app.blockchain_publisher.service.event_publish

import org.cardanofoundation.lob.app.accounting_reporting_core.domain.core.BlockchainReceipt
import org.cardanofoundation.lob.app.blockchain_publisher.service.BlockchainPublishStatusMapper
import org.cardanofoundation.lob.app.support.collections.Partitions
import org.cardanofoundation.lob.app.support.modulith.EventMetadata
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import tech.edgx.cms_demo_app.blockchain_publisher.domain.event.ConsignmentStatusUpdate
import tech.edgx.cms_demo_app.blockchain_publisher.domain.event.ConsignmentsLedgerUpdatedEvent
import tech.edgx.cms_demo_app.blockchain_publisher.domain.entity.consignments.ConsignmentEntity

@Service("cms-demo-app.ledgerUpdatedEventPublisher")
class ConsignmentLedgerUpdatedEventPublisher(
    private val applicationEventPublisher: ApplicationEventPublisher,
    private val blockchainPublishStatusMapper: BlockchainPublishStatusMapper
) {
    private val logger = LoggerFactory.getLogger(ConsignmentLedgerUpdatedEventPublisher::class.java)

    @Value("\${lob.blockchain_publisher.send.batch.size:100}")
    private var dispatchBatchSize: Int = 100

    private val BLOCKCHAIN_TYPE = "CARDANO_L1"

    @Transactional
    fun sendConsignmentLedgerUpdatedEvents(organisationId: String, consignments: Set<ConsignmentEntity>) {
        logger.debug("Sending consignment ledger updated event for organisation: {}, consignments: {}", organisationId, consignments.size)

        val partitions = Partitions.partition(consignments, dispatchBatchSize)
        for (partition in partitions) {
            val consignmentStatuses = partition.asSet().map { consignmentEntity ->
                val publishStatusM = consignmentEntity.getL1SubmissionData().flatMap { it.publishStatus }
                val finalityScoreM = consignmentEntity.getL1SubmissionData().flatMap { it.finalityScore }
                val ledgerDispatchStatus = blockchainPublishStatusMapper.convert(publishStatusM, finalityScoreM)
                val consignmentId = consignmentEntity.consignmentId

                val blockchainHashM = consignmentEntity.getL1SubmissionData().flatMap { it.getTransactionHash() }
                if (blockchainHashM.isEmpty) {
                    ConsignmentStatusUpdate(consignmentId, ledgerDispatchStatus, setOf())
                } else {
                    val blockchainReceipts = setOf<BlockchainReceipt>(
                        BlockchainReceipt().apply {
                            type = BLOCKCHAIN_TYPE
                            hash = blockchainHashM.get()
                        }
                    )
                    ConsignmentStatusUpdate(consignmentId, ledgerDispatchStatus, blockchainReceipts)
                }
            }.toSet()

            logger.debug("Sending consignments ledger updated event for organisation: {}, statuses: {}", organisationId, consignmentStatuses)

            val event = ConsignmentsLedgerUpdatedEvent(
                metadata = EventMetadata.create("1.0"),
                organisationId = organisationId,
                statusUpdates = consignmentStatuses
            )

            applicationEventPublisher.publishEvent(event)
        }
    }
}