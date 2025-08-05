package tech.edgx.cms_demo_indexer.config

import org.cardano.foundation.lob.service.LOBOnChainBatchProcessor
import org.cardano.foundation.lob.service.MetadataDeserialiser
import org.cardano.foundation.lob.service.TransactionService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Conditional

@Configuration
class LOBProcessorConfig {

    @Bean("lOBOnChainBatchProcessor")
    @Conditional(DisableLOBOnChainBatchProcessorCondition::class)
    fun lobOnChainBatchProcessor(
        transactionService: TransactionService,
        metadataDeserialiser: MetadataDeserialiser
    ): LOBOnChainBatchProcessor? {
        // This bean will not be created because the condition returns false
        return null
    }
}