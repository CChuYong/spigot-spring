package chuyong.springspigot.external.dependency

interface ExternalDependencyProvider {
    operator fun <T : Any> get(clazz: Class<T>): T
    fun <T : Any> getNamed(clazz: Class<T>, qualifier: String): T
}
