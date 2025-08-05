package tech.edgx.cms_demo_app.config

import org.cardanofoundation.lob.app.blockchain_common.domain.CardanoNetwork
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import java.util.*

/*
    Only reason needed this is to override cardano:network with default as PREPROD
 */

@Configuration
@Primary
class CardanoConfig {
    @Bean
    fun cardanoNetwork(@Value("\${cardano.network:PREPROD}") network: String): CardanoNetwork {
        println("Configured backend network: $network")
        try {
            return CardanoNetwork.valueOf(network.uppercase(Locale.getDefault()))
        } catch (e: IllegalArgumentException) {
            println("Invalid cardano.network value: $network, defaulting to PREPROD")
            return CardanoNetwork.PREPROD
        }
    }
}