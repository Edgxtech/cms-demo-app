package tech.edgx.cms_demo_indexer.service

import com.bloxbean.cardano.client.metadata.cbor.CBORMetadata
import com.bloxbean.cardano.client.metadata.cbor.CBORMetadataMap
import tech.edgx.cms_demo_indexer.domain.entity.ConsignmentEntity
import com.bloxbean.cardano.client.util.HexUtil
import com.bloxbean.cardano.yaci.store.metadata.domain.TxMetadataEvent
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
import java.math.BigInteger

@Service("consignment.lOBOnChainBatchProcessor")
class ConsignmentOnChainBatchProcessor(
    private val consignmentService: ConsignmentService,
    private val consignmentMetadataDeserialiser: ConsignmentMetadataDeserialiser,
    @Value("\${lob.transaction.metadata_label:1448}") private val metadataLabel: Int
) {
    private val log = LoggerFactory.getLogger(this::class.java)

    @EventListener
    fun metadataEvent(event: TxMetadataEvent) {
        val txMetadataList = event.txMetadataList
        for (txEvent in txMetadataList) {
            log.debug("txEvent: {}", txEvent)
            if (txEvent.label.equals(metadataLabel.toString(), ignoreCase = true)) {
                log.debug("Processing TxMetadataEvent for metadata label: {}, event: {}", metadataLabel, event)
                val cborBytes = HexUtil.decodeHexString(txEvent.cbor.replace("\\x", ""))
                val cborMetadata = CBORMetadata.deserialize(cborBytes)
                val envelopeCborMap = cborMetadata.get(BigInteger.valueOf(metadataLabel.toLong())) as? CBORMetadataMap
                    ?: throw IllegalStateException("Invalid metadata structure for label $metadataLabel")

                val lobBatch = consignmentMetadataDeserialiser.decode(envelopeCborMap)

                for (lobConsignment in lobBatch.consignments) {
                    val consignment = ConsignmentEntity(
                        consignmentId = lobConsignment.id,
                        organisationId = lobBatch.organisationId, // Nullable
                        l1TransactionHash = txEvent.txHash,
                        l1AbsoluteSlot = txEvent.slot
                    )
                    log.info("Processing TxMetadataEvent for metadata label: {}, Storing: {}", metadataLabel, consignment)
                    consignmentService.storeIfNew(consignment)
                }
            }
        }
    }
}