package chuyong.springspigot.synchronize

import chuyong.springspigot.command.CommandContext
import org.springframework.core.task.TaskDecorator

class CommandContextDecorator : TaskDecorator {
    override fun decorate(task: Runnable): Runnable {
        val context = CommandContext.currentContext
        return Runnable {
            try {
                CommandContext.currentContext = context
                task.run()
            } catch (e: Throwable) {
                e.printStackTrace()
            } finally {
                CommandContext.clearContext()
            }
        }
    }
}
