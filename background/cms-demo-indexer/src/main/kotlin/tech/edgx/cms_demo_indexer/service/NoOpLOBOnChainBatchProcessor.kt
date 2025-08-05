package tech.edgx.cms_demo_indexer.service

import org.cardano.foundation.lob.service.TransactionService
import org.cardano.foundation.lob.service.MetadataDeserialiser
import org.cardano.foundation.lob.service.LOBOnChainBatchProcessor
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service
import com.bloxbean.cardano.yaci.store.metadata.domain.TxMetadataEvent
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener

@Service("lOBOnChainBatchProcessor") // Match the exact bean name
@Primary
class NoOpLOBOnChainBatchProcessor(
    transactionService: TransactionService,
    metadataDeserialiser: MetadataDeserialiser
) : LOBOnChainBatchProcessor(transactionService, metadataDeserialiser) {

    private val log = LoggerFactory.getLogger(this::class.java)

    @EventListener
    override fun metadataEvent(event: TxMetadataEvent) {
        log.info("No-op LOBOnChainBatchProcessor: Ignoring TxMetadataEvent: {}", event)
        // Do nothing
    }
}