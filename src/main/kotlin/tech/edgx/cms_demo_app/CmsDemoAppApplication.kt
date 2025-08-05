package tech.edgx.cms_demo_app

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration
import org.springframework.boot.runApplication
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.FilterType
import org.springframework.data.jpa.repository.config.EnableJpaAuditing
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication(exclude = [OAuth2ClientAutoConfiguration::class])
@ComponentScan(
	basePackages = ["tech.edgx.cms_demo_app", "org.cardanofoundation.lob.app"],
	excludeFilters = [ComponentScan.Filter(
		type = FilterType.ASSIGNABLE_TYPE,
		classes = [org.cardanofoundation.lob.app.blockchain_reader.config.CardanoConfig::class]
	)]
)
@EnableJpaRepositories(basePackages = ["tech.edgx.cms_demo_app", "org.cardanofoundation.lob.app"])
@EntityScan(basePackages = ["tech.edgx.cms_demo_app", "org.cardanofoundation.lob.app"])
@EnableJpaAuditing
@EnableScheduling
class CmsDemoAppApplication

fun main(args: Array<String>) {
	runApplication<CmsDemoAppApplication>(*args)
}
