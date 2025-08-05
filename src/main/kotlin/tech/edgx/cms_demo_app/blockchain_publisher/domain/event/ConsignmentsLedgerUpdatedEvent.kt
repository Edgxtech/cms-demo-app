package tech.edgx.cms_demo_app.blockchain_publisher.domain.event

import org.cardanofoundation.lob.app.support.modulith.EventMetadata

data class ConsignmentsLedgerUpdatedEvent(
    val metadata: EventMetadata,
    val organisationId: String,
    val statusUpdates: Set<ConsignmentStatusUpdate>
)