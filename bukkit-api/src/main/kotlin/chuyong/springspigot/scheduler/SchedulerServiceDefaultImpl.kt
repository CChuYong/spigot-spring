package chuyong.springspigot.scheduler

import org.bukkit.plugin.Plugin
import org.bukkit.scheduler.BukkitScheduler
import org.springframework.stereotype.Service

@Service
internal class SchedulerServiceDefaultImpl(
    private val scheduler: BukkitScheduler,
    private val plugin: Plugin,
) : SchedulerService {
    override fun scheduleSyncDelayedTask(task: Runnable?, delay: Long): Int {
        return scheduler.scheduleSyncDelayedTask(plugin, task!!, delay)
    }

    override fun scheduleSyncDelayedTask(task: Runnable?): Int {
        return scheduler.scheduleSyncDelayedTask(plugin, task!!)
    }

    override fun scheduleSyncRepeatingTask(task: Runnable?, delay: Long, period: Long): Int {
        return scheduler.scheduleSyncRepeatingTask(plugin, task!!, delay, period)
    }

    override fun cancelTask(taskId: Int) {
        scheduler.cancelTask(taskId)
    }

    override fun isCurrentlyRunning(taskId: Int): Boolean {
        return scheduler.isCurrentlyRunning(taskId)
    }

    override fun isQueued(taskId: Int): Boolean {
        return scheduler.isQueued(taskId)
    }
}
