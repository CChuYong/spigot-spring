package chuyong.springspigot.synchronize


import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.AsyncConfigurer
import java.util.concurrent.Executor

@Configuration
class SpringAsyncConfigurer(
    private val executor: Executor,
    private val commandExceptionHandler: SpringAsyncExceptionHandler,
) : AsyncConfigurer {
    override fun getAsyncExecutor(): Executor {
        return executor
    }

    override fun getAsyncUncaughtExceptionHandler(): AsyncUncaughtExceptionHandler {
        return commandExceptionHandler
    }
}
