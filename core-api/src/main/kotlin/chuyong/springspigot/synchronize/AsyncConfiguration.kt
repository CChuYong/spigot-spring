package chuyong.springspigot.synchronize

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.EnableAspectJAutoProxy
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import java.util.concurrent.Executor

@Configuration
@EnableAspectJAutoProxy
@EnableAsync
class AsyncConfiguration {
    @Bean
    fun executor(): Executor {
        val executor = ThreadPoolTaskExecutor()
        executor.corePoolSize = 7
        executor.maxPoolSize = 42
        executor.queueCapacity = 11
        executor.setTaskDecorator(CommandContextDecorator())
        executor.setThreadNamePrefix("SpringSpigot-")
        executor.initialize()
        return executor
    }
}
