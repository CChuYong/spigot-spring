package chuyong.springspigot.scheduler

import org.springframework.scheduling.TaskScheduler
import org.springframework.scheduling.Trigger
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ScheduledFuture

class SpigotScheduler(
    private val scheduler: SchedulerService
) : TaskScheduler {
    private val taskScheduler = ThreadPoolTaskScheduler()

    init {
        taskScheduler.poolSize = 1
        taskScheduler.initialize()
    }

    private fun wrapSync(task: Runnable): Runnable {
        return WrappedRunnable(scheduler, task)
    }

    override fun schedule(task: Runnable, trigger: Trigger): ScheduledFuture<*>? {
        return taskScheduler.schedule(wrapSync(task), trigger)
    }

    override fun schedule(task: Runnable, startTime: Instant): ScheduledFuture<*> {
        return taskScheduler.schedule(wrapSync(task), startTime)
    }

    override fun scheduleAtFixedRate(task: Runnable, startTime: Instant, period: Duration): ScheduledFuture<*> {
        return taskScheduler.scheduleAtFixedRate(wrapSync(task), startTime, period)
    }

    override fun scheduleAtFixedRate(task: Runnable, period: Duration): ScheduledFuture<*> {
        return taskScheduler.scheduleAtFixedRate(wrapSync(task), period)
    }

    override fun scheduleWithFixedDelay(task: Runnable, startTime: Instant, delay: Duration): ScheduledFuture<*> {
        return taskScheduler.scheduleWithFixedDelay(wrapSync(task), startTime, delay)
    }

    override fun scheduleWithFixedDelay(task: Runnable, delay: Duration): ScheduledFuture<*> {
        return taskScheduler.scheduleWithFixedDelay(wrapSync(task), delay)
    }
}
