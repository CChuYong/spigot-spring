package chuyong.springspigot.scheduler

data class WrappedRunnable(
    private val scheduler: SchedulerService,
    private val runnable: Runnable,
) : Runnable {
    override fun run() {
        scheduler.scheduleSyncDelayedTask(runnable)
    }
}
