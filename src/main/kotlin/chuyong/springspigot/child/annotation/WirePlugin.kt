package chuyong.springspigot.child.annotation

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class WirePlugin(
    val value: String = "",
    val pluginName: String = "",
)
