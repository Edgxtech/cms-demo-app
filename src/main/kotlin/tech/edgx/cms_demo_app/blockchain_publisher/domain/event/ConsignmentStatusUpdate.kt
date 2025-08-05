package tech.edgx.cms_demo_app.blockchain_publisher.domain.event

import org.cardanofoundation.lob.app.accounting_reporting_core.domain.core.BlockchainReceipt
import org.cardanofoundation.lob.app.accounting_reporting_core.domain.core.LedgerDispatchStatus

data class ConsignmentStatusUpdate(
    val consignmentId: String,
    val status: LedgerDispatchStatus,
    val blockchainReceipts: Set<BlockchainReceipt>
)