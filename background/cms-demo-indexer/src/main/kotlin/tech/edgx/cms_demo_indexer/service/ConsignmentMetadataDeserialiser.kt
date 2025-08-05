package tech.edgx.cms_demo_indexer.service

import com.bloxbean.cardano.client.metadata.cbor.CBORMetadataList
import com.bloxbean.cardano.client.metadata.cbor.CBORMetadataMap
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import tech.edgx.cms_demo_indexer.domain.LOBOnChainBatch
import tech.edgx.cms_demo_indexer.domain.LOBOnChainConsignment

@Service
class ConsignmentMetadataDeserialiser {
    private val log = LoggerFactory.getLogger(this::class.java)

    fun decode(payload: CBORMetadataMap): LOBOnChainBatch {
        val org = payload.get("org") as? CBORMetadataMap
        val orgId = org?.get("id") as? String
        val type = payload.get("type") as? String

        val consignments = if (type == "CONSIGNMENTS") {
            readConsignments(payload.get("data") as? CBORMetadataList)
        } else {
            log.warn("Skipping unsupported metadata type: {}", type)
            emptySet()
        }

        return LOBOnChainBatch(
            organisationId = orgId,
            consignments = consignments
        )
    }

    private fun readConsignment(cborMetadataMap: CBORMetadataMap): LOBOnChainConsignment {
        return LOBOnChainConsignment(
            id = cborMetadataMap.get("id") as String
        )
    }

    private fun readConsignments(cborMetadataList: CBORMetadataList?): Set<LOBOnChainConsignment> {
        if (cborMetadataList == null) return emptySet()
        return (0 until cborMetadataList.size())
            .map { readConsignment(cborMetadataList.getValueAt(it) as CBORMetadataMap) }
            .toSet()
    }
}