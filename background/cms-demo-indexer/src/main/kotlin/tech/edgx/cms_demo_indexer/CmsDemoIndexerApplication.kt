package tech.edgx.cms_demo_indexer

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.boot.runApplication
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.FilterType
import org.springframework.data.jpa.repository.config.EnableJpaAuditing
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication()
@ComponentScan(
	basePackages = ["tech.edgx.cms_demo_indexer", "org.cardano.foundation.lob"],
	excludeFilters = [
		ComponentScan.Filter(
			type = FilterType.ASSIGNABLE_TYPE,
			classes = [org.cardano.foundation.lob.service.LOBOnChainBatchProcessor::class]
		)
	]
)
@EnableJpaRepositories(basePackages = ["tech.edgx.cms_demo_indexer", "org.cardano.foundation.lob"])
@EntityScan(basePackages = ["tech.edgx.cms_demo_indexer", "org.cardano.foundation.lob"])
@EnableJpaAuditing
@EnableScheduling
class CmsDemoIndexerApplication

fun main(args: Array<String>) {
	runApplication<CmsDemoIndexerApplication>(*args)
}