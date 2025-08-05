package tech.edgx.cms_demo_indexer.domain

data class LOBOnChainBatch(
    val organisationId: String?,
    val consignments: Set<LOBOnChainConsignment> = emptySet()
)

data class LOBOnChainConsignment(
    val id: String
)