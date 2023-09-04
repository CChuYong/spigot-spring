package chuyong.springspigot.command

interface CommandRegistry {
    fun registerAdvices(beanObject: Any)
    fun registerCommands(beanObject: Any, pluginName: String)
    fun unregisterCommands(pluginName: String)
}
