package tech.edgx.cms_demo_app.blockchain.domain.event

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import org.cardanofoundation.lob.app.support.modulith.EventMetadata
import org.jmolecules.event.annotation.DomainEvent
import tech.edgx.cms_demo_app.blockchain_publisher.domain.entity.consignments.Consignment

@DomainEvent
data class ConsignmentLedgerUpdateCommand(
    @NotNull val metadata: EventMetadata,
    @NotBlank val organisationId: String,
    @NotNull @Size(min = 1) val consignments: Set<Consignment>
) {
    companion object {
        const val VERSION = "1.0"

        fun create(metadata: EventMetadata, organisationId: String, consignments: Set<Consignment>): ConsignmentLedgerUpdateCommand {
            return ConsignmentLedgerUpdateCommand(metadata, organisationId, consignments)
        }
    }
}