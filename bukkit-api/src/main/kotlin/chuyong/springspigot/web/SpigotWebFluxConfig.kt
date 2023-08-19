package chuyong.springspigot.web

import io.netty.channel.EventLoopGroup
import io.netty.channel.epoll.Epoll
import org.slf4j.Logger
import org.springframework.boot.web.embedded.netty.NettyReactiveWebServerFactory
import org.springframework.boot.web.embedded.netty.NettyServerCustomizer
import org.springframework.boot.web.reactive.server.ReactiveWebServerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import reactor.netty.http.server.HttpServer

@Configuration
class SpigotWebFluxConfig(
    private val logger: Logger,
) {
    @Bean
    fun reactiveWebServerFactory(): ReactiveWebServerFactory {
        val nmsClass = Class.forName("net.minecraft.server.network.ServerConnectionListener")
        val lazyValue = if (Epoll.isAvailable()) {
            nmsClass.getDeclaredField("SERVER_EPOLL_EVENT_GROUP").get(null)
        } else {
            nmsClass.getDeclaredField("SERVER_EVENT_GROUP").get(null)
        }

        logger.info("Utilizing Minecraft Native EventLoopGroup")

        val elg = lazyValue::class.java.getDeclaredMethod("get").invoke(lazyValue) as EventLoopGroup

        val factory = NettyReactiveWebServerFactory()
        factory.addServerCustomizers(NettyServerCustomizer { builder: HttpServer ->
            builder.runOn(elg)
        })
        return factory
    }
}
