package chuyong.springspigot

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.EnableAspectJAutoProxy
import org.springframework.context.annotation.Primary
import org.springframework.scheduling.annotation.EnableAsync

@SpringBootApplication
@EnableAspectJAutoProxy
@EnableAsync
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class EnableSpringSpigotSupport(
    val contextEscalation: Boolean = false,
)
