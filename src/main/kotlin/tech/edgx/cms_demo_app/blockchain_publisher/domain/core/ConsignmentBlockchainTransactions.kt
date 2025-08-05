package tech.edgx.cms_demo_app.blockchain_publisher.domain.core

import tech.edgx.cms_demo_app.blockchain_publisher.domain.entity.consignments.ConsignmentEntity

data class ConsignmentBlockchainTransactions(
    val organisationId: String,
    val processedConsignments: Set<ConsignmentEntity>,
    val remainingConsignments: Set<ConsignmentEntity>,
    val creationSlot: Long,
    val txBytes: ByteArray,
    val organiserAddress: String
)