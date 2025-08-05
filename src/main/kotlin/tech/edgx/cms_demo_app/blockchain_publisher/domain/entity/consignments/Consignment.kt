package tech.edgx.cms_demo_app.blockchain_publisher.domain.entity.consignments

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import org.cardanofoundation.lob.app.blockchain_publisher.domain.entity.txs.L1SubmissionData
import org.cardanofoundation.lob.app.organisation.domain.entity.Organisation
import java.time.LocalDateTime

@JsonIgnoreProperties(ignoreUnknown = true)
data class Consignment(
    val id: String? = null, // Nullable for server-side generation
    val idControl: String? = null,
    val ver: Long? = null, // Add ver field
    val goods: Map<String, Int>,
    val sender: Organisation?,
    val receiver: Organisation?,
    val l1SubmissionData: L1SubmissionData? = null,
    val trackingStatus: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val createdAt: LocalDateTime? = null,
    val updatedAt: LocalDateTime? = null,
    val dispatchedAt: LocalDateTime? = null // New field
)