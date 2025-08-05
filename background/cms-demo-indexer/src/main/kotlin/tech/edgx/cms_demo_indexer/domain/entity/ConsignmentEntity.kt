package tech.edgx.cms_demo_indexer.domain.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.springframework.data.domain.Persistable
import org.springframework.data.jpa.domain.support.AuditingEntityListener

@Entity
@Table(name = "blockchain_reader_consignment", schema = "lob_follower_service")
@EntityListeners(AuditingEntityListener::class)
data class ConsignmentEntity(
    @Id
    @Column(name = "consignment_id", nullable = false)
    val consignmentId: String,

    @Column(name = "organisation_id")
    val organisationId: String?,

    @Column(name = "l1_absolute_slot", nullable = false)
    val l1AbsoluteSlot: Long,

    @Column(name = "l1_transaction_hash", nullable = false)
    val l1TransactionHash: String
) : Persistable<String> {
    override fun getId(): String = consignmentId
    override fun isNew(): Boolean = true // Adjust based on auditing logic
}