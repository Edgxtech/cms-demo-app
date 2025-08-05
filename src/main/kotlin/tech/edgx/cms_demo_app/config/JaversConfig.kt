package tech.edgx.cms_demo_app.config

import org.javers.core.Javers
import org.javers.core.JaversBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class JaversConfig {
    @Bean
    fun javers(): Javers {
        return JaversBuilder.javers().build()
    }
}