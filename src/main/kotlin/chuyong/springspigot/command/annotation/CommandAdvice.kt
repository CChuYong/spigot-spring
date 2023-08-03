package chuyong.springspigot.command.annotation

import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.AliasFor

@Target(AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Configuration
annotation class CommandAdvice(
    @get:AliasFor(annotation = Configuration::class) val value: String = "",
)
