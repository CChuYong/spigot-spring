package chuyong.springspigot.synchronize

import chuyong.springspigot.command.BaseCommandExceptionHandler
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.AsyncConfigurer
import java.util.concurrent.Executor

@Configuration
class SpringAsyncConfigurer(
    private val executor: Executor,
    private val commandExceptionHandler: BaseCommandExceptionHandler,
) : AsyncConfigurer {
    override fun getAsyncExecutor(): Executor {
        return executor
    }

    override fun getAsyncUncaughtExceptionHandler(): AsyncUncaughtExceptionHandler {
        return commandExceptionHandler
    }
}
