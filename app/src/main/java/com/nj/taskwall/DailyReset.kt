package com.nj.taskwall

import android.content.Context
import androidx.work.*
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

object DailyReset {
    // New day: finished tasks (and finished subtasks) are cleared, unfinished ones are carried over.
    suspend fun runIfNeeded(ctx: Context) {
        val p = ctx.getSharedPreferences("settings", Context.MODE_PRIVATE)
        val today = LocalDate.now().toString()
        val last = p.getString("lastReset", null)
        if (last == null) { p.edit().putString("lastReset", today).apply(); return }
        if (last == today) return
        val dao = AppDb.get(ctx).dao()
        dao.getAll().filter { it.done }.forEach { dao.delete(it.task) }
        dao.deleteDoneSubtasks()
        dao.markAllCarried()
        p.edit().putString("lastReset", today).apply()
    }

    fun schedule(ctx: Context) {
        val now = LocalDateTime.now()
        val next = now.toLocalDate().plusDays(1).atStartOfDay()
        val delay = Duration.between(now, next).toMillis() + 60_000
        val req = PeriodicWorkRequestBuilder<MidnightWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(delay, TimeUnit.MILLISECONDS).build()
        WorkManager.getInstance(ctx)
            .enqueueUniquePeriodicWork("midnight-reset", ExistingPeriodicWorkPolicy.KEEP, req)
    }
}

class MidnightWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        DailyReset.runIfNeeded(applicationContext)
        if (SettingsStore.load(applicationContext).autoUpdate) WallpaperRenderer.refreshFromStorage(applicationContext)
        return Result.success()
    }
}
