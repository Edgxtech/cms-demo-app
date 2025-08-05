package tech.edgx.cms_demo_app.blockchain_publisher.job

import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import tech.edgx.cms_demo_app.blockchain_publisher.service.dispatch.BlockchainConsignmentsDispatcher

@Service("ims_demo_app.ConsignmentsDispatcherJob")
class ConsignmentsDispatcherJob(
    private val blockchainConsignmentsDispatcher: BlockchainConsignmentsDispatcher
) {

    private val logger = LoggerFactory.getLogger(ConsignmentsDispatcherJob::class.java)

    @PostConstruct
    fun init() {
        logger.info("ims_demo_app.ConsignmentsDispatcherJob is enabled.")
    }

    @Scheduled(
        fixedDelayString = "\${lob.blockchain_publisher.dispatcher.consignment.fixed_delay:PT10S}",
        initialDelayString = "\${lob.blockchain_publisher.dispatcher.consignment.initial_delay:PT15S}"
    )
    fun execute() {
        logger.info("Polling for consignment blockchain transactions to be sent to the blockchain...")

        blockchainConsignmentsDispatcher.dispatchConsignments()

        logger.info("Polling for consignment blockchain transactions to be sent to the blockchain...done")
    }
}