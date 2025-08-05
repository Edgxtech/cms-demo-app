package tech.edgx.cms_demo_app.blockchain_reader.domain

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class IndexerConsignmentEntity(
    val consignmentId: String,
    val organisationId: String?,
    val l1AbsoluteSlot: Long,
    val l1TransactionHash: String
)