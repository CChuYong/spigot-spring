package chuyong.springspigot.jpa

import org.slf4j.Logger
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary

@Configuration
class DataSourceConfiguration(
    private val logger: Logger,
) {
    @ConditionalOnProperty(name = ["spring.datasource.url"], matchIfMissing = true)
    @Bean
    @Primary
    fun defaultH2DataSource(): DataSourceProperties {
        logger.info("Using default Mem H2 DataSource")
        return DataSourceProperties().apply {
            url = "jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE"
            username = "sa"
            password = ""
        }
    }
}
