package chuyong.springspigot

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class EnableSpringSpigotSupport(
    val contextEscalation: Boolean = false,
)
