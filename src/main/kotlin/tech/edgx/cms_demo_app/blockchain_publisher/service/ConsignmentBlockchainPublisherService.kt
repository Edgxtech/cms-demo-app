package tech.edgx.cms_demo_app.blockchain_publisher.service

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import tech.edgx.cms_demo_app.blockchain_publisher.domain.entity.consignments.Consignment
import tech.edgx.cms_demo_app.blockchain_publisher.repository.ConsignmentEntityRepositoryGateway
import tech.edgx.cms_demo_app.blockchain_publisher.service.event_publish.ConsignmentLedgerUpdatedEventPublisher

@Service("cms_demo_app.blockchainPublisherService")
class ConsignmentBlockchainPublisherService(
    private val consignmentEntityRepositoryGateway: ConsignmentEntityRepositoryGateway,
    private val consignmentConverter: ConsignmentConverter,
    @Qualifier("cms-demo-app.ledgerUpdatedEventPublisher") private val ledgerUpdatedEventPublisher: ConsignmentLedgerUpdatedEventPublisher
) {

    private val logger = LoggerFactory.getLogger(ConsignmentBlockchainPublisherService::class.java)

    @Transactional
    fun storeConsignmentsForDispatchLater(organisationId: String, consignments: MutableSet<Consignment>) {
        logger.info("storeConsignmentsForDispatchLater..., orgId:{}", organisationId)

        val consignmentEntities = consignments.map { consignment ->
            consignmentConverter.convertToDbDetached(consignment)
        }.toSet()

        val storedConsignments = consignmentEntityRepositoryGateway.storeOnlyNew(consignmentEntities)

        ledgerUpdatedEventPublisher.sendConsignmentLedgerUpdatedEvents(organisationId, storedConsignments)
    }

}