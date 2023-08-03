package chuyong.springspigot.synchronize

import chuyong.springspigot.scheduler.SchedulerService
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.bukkit.Server
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component

@Aspect
@Component
class SynchronizeHandler(
    private val schedulerService: SchedulerService,
    private val server: Server,
) {

    @Order(0)
    @Around(
        "within(@(@chuyong.springspigot.synchronize.annotation.Synchronize *) *) " +
                "|| execution(@(@chuyong.springspigot.synchronize.annotation.Synchronize *) * *(..)) " +
                "|| @within(chuyong.springspigot.synchronize.annotation.Synchronize)" +
                "|| execution(@chuyong.springspigot.synchronize.annotation.Synchronize * *(..))"
    )
    @Throws(Throwable::class)
    fun synchronizeCall(joinPoint: ProceedingJoinPoint): Any? {
        if (server.isPrimaryThread) {
            return joinPoint.proceed()
        }
        schedulerService.scheduleSyncDelayedTask({
            try {
                joinPoint.proceed()
            } catch (throwable: Throwable) {
                throwable.printStackTrace()
            }
        }, 0)
        return null
    }
}
