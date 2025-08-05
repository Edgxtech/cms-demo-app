package tech.edgx.cms_demo_app.config

import com.bloxbean.cardano.client.api.UtxoSupplier
import com.bloxbean.cardano.client.backend.api.BackendService
import com.bloxbean.cardano.client.backend.api.DefaultUtxoSupplier
import org.cardanofoundation.lob.app.blockchain_publisher.service.transation_submit.BackendServiceBlockchainTransactionSubmissionService
import org.cardanofoundation.lob.app.blockchain_publisher.service.transation_submit.BlockchainTransactionSubmissionService
import org.cardanofoundation.lob.app.blockchain_publisher.service.transation_submit.DefaultTransactionSubmissionService
import org.cardanofoundation.lob.app.blockchain_publisher.service.transation_submit.TransactionSubmissionService
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Clock

@Configuration
class BlockchainConfig {

    @Bean
    fun blockchainTransactionSubmissionService(
        @Qualifier("yaci_blockfrost") backendService: BackendService
    ): BlockchainTransactionSubmissionService {
        return BackendServiceBlockchainTransactionSubmissionService(backendService)
    }

    @Bean
    fun utxoSupplier(
        @Qualifier("yaci_blockfrost") backendService: BackendService
    ): UtxoSupplier {
        return DefaultUtxoSupplier(backendService.getUtxoService())
    }

    @Bean
    fun transactionSubmissionService(
        blockchainTransactionSubmissionService: BlockchainTransactionSubmissionService,
        @Qualifier("yaci_blockfrost") backendService: BackendService,
        utxoSupplier: UtxoSupplier,
        clock: Clock,
        @Value("\${lob.transaction.submission.sleep.seconds:5}") sleepTimeSeconds: Int,
        @Value("\${lob.transaction.submission.timeout.in.seconds:300}") timeoutInSeconds: Int
    ): TransactionSubmissionService {
        return DefaultTransactionSubmissionService(
            blockchainTransactionSubmissionService,
            backendService,
            utxoSupplier,
            clock,
            sleepTimeSeconds,
            timeoutInSeconds
        )
    }

    @Bean
    fun clock(): Clock {
        return Clock.systemUTC()
    }
}