package tech.edgx.cms_demo_app.blockchain_publisher.service.event_handle

import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import tech.edgx.cms_demo_app.blockchain.domain.event.ConsignmentLedgerUpdateCommand
import tech.edgx.cms_demo_app.blockchain_publisher.domain.entity.consignments.Consignment
import tech.edgx.cms_demo_app.blockchain_publisher.service.ConsignmentBlockchainPublisherService

@Service
class ConsignmentBlockchainEventHandler(
    private val blockchainPublisherService: ConsignmentBlockchainPublisherService
) {

    private val logger = LoggerFactory.getLogger(ConsignmentBlockchainEventHandler::class.java)

    @EventListener
    @Async
    fun handleConsignmentLedgerUpdateCommand(command: ConsignmentLedgerUpdateCommand) {
        logger.info("Received ConsignmentLedgerUpdateCommand: organisationId={}, consignmentCount={}",
            command.organisationId, command.consignments.size)

        if (command.consignments.isEmpty()) {
            logger.info("No consignments to process for organisation: {}", command.organisationId)
            return
        }

        logger.info("Processing consignments: {}", command.consignments.map { it.id })
        blockchainPublisherService.storeConsignmentsForDispatchLater(command.organisationId,
            command.consignments as MutableSet<Consignment>
        )
        logger.info("Triggered blockchain storage for organisation: {}", command.organisationId)
    }
}