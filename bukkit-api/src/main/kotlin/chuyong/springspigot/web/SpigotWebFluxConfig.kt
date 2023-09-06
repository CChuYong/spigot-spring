package chuyong.springspigot.web

import org.springframework.context.annotation.Configuration


@Configuration
class SpigotWebFluxConfig(

) {
//    @Bean
//    fun reactiveWebServerFactory(): ReactiveWebServerFactory {
//        val nmsClass = Class.forName("net.minecraft.server.network.ServerConnectionListener")
//        val lazyValue = if (Epoll.isAvailable()) {
//            nmsClass.getDeclaredField("SERVER_EPOLL_EVENT_GROUP").get(null)
//        } else {
//            nmsClass.getDeclaredField("SERVER_EVENT_GROUP").get(null)
//        }
//
//        logger.info("Utilizing Minecraft Native EventLoopGroup")
//
//        val elg = lazyValue::class.java.getDeclaredMethod("get").invoke(lazyValue) as EventLoopGroup
//
//        val factory = NettyReactiveWebServerFactory()
//        factory.addServerCustomizers(NettyServerCustomizer { builder: HttpServer ->
//            builder.runOn(elg)
//        })
//        return factory
//    }
//}

}
