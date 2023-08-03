package chuyong.springspigot.scheduler

/**
 * Wrapper around [BukkitScheduler][org.bukkit.scheduler.BukkitScheduler] to remove the need of the plugin reference
 * as well as keep the context during the tasks.
 */
interface SchedulerService {
    /**
     * Schedules a once off task to occur after a delay.
     *
     *
     * This task will be executed by the main server thread.
     *
     * @param task  Task to be executed
     * @param delay Delay in server ticks before executing task
     * @return Task id number (-1 if scheduling failed)
     */
    fun scheduleSyncDelayedTask(task: Runnable?, delay: Long): Int

    /**
     * Schedules a once off task to occur as soon as possible.
     *
     *
     * This task will be executed by the main server thread.
     *
     * @param task Task to be executed
     * @return Task id number (-1 if scheduling failed)
     */
    fun scheduleSyncDelayedTask(task: Runnable?): Int

    /**
     * Schedules a repeating task.
     *
     *
     * This task will be executed by the main server thread.
     *
     * @param task   Task to be executed
     * @param delay  Delay in server ticks before executing first repeat
     * @param period Period in server ticks of the task
     * @return Task id number (-1 if scheduling failed)
     */
    fun scheduleSyncRepeatingTask(task: Runnable?, delay: Long, period: Long): Int

    /**
     * Removes task from scheduler.
     *
     * @param taskId Id number of task to be removed
     */
    fun cancelTask(taskId: Int)

    /**
     * Check if the task currently running.
     *
     *
     * A repeating task might not be running currently, but will be running in
     * the future. A task that has finished, and does not repeat, will not be
     * running ever again.
     *
     *
     * Explicitly, a task is running if there exists a thread for it, and that
     * thread is alive.
     *
     * @param taskId The task to check.
     *
     *
     * @return If the task is currently running.
     */
    fun isCurrentlyRunning(taskId: Int): Boolean

    /**
     * Check if the task queued to be run later.
     *
     *
     * If a repeating task is currently running, it might not be queued now
     * but could be in the future. A task that is not queued, and not running,
     * will not be queued again.
     *
     * @param taskId The task to check.
     *
     *
     * @return If the task is queued to be run.
     */
    fun isQueued(taskId: Int): Boolean
}
