package tech.edgx.cms_demo_app.blockchain_reader.job

import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import tech.edgx.cms_demo_app.blockchain_reader.service.WatchDogService

@Service
class WatchDogJob(
    @Qualifier("cms_demo_app.watchDogService") private val watchDogService: WatchDogService
) {
    private val log = LoggerFactory.getLogger(WatchDogJob::class.java)

    @Value("\${lob.blockchain_publisher.watchdog.tx_limit_per_org_pull_size:1000}")
    private val txStatusInspectionLimitPerOrgPullSize = 1000 // limit per org in one go as in, per one job run

    @PostConstruct
    fun init() {
        log.info("WatchDogJob is enabled.")
    }

    @Scheduled(
        fixedDelayString = "\${lob.blockchain_publisher.watchdog.transaction.fixed_delay:PT1M}",
        initialDelayString = "\${lob.blockchain_publisher.watchdog.transaction.initial_delay:PT1M}"
    )
    fun executeConsignmentStatusCheck() {
        log.info("Inspecting all organisations for on chain transaction status changes...")
        watchDogService.checkConsignmentStatusForOrganisations(txStatusInspectionLimitPerOrgPullSize)
    }
}