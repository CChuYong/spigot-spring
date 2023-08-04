package chuyong.springspigot.jpa

//@Configuration
//class DataSourceConfiguration(
//    private val logger: Logger,
//) {
//    @ConditionalOnProperty(name = ["spring.datasource.url"], matchIfMissing = true)
//    @Bean
//    @Primary
//    fun defaultH2DataSource(): DataSourceProperties {
//        logger.info("Using default Mem H2 DataSource")
//        return DataSourceProperties().apply {
//            url = "jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE"
//            username = "sa"
//            password = ""
//        }
//    }
//}
