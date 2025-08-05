package tech.edgx.cms_demo_indexer.config

import org.springframework.context.annotation.Condition
import org.springframework.context.annotation.ConditionContext
import org.springframework.core.type.AnnotatedTypeMetadata

class DisableLOBOnChainBatchProcessorCondition : Condition {
    override fun matches(context: ConditionContext, metadata: AnnotatedTypeMetadata): Boolean {
        // Return false to prevent the original LOBOnChainBatchProcessor from being registered
        return false
    }
}