package chuyong.springspigot.command
abstract class CommandContext{
    companion object {
        private val COMMAND_CONTEXT = ThreadLocal<CommandContext>()

        @JvmStatic
        var currentContext: CommandContext?
            get() = COMMAND_CONTEXT.get()
            set(context) {
                COMMAND_CONTEXT.set(context)
            }

        @JvmStatic
        fun clearContext() {
            COMMAND_CONTEXT.remove()
        }
    }
}
