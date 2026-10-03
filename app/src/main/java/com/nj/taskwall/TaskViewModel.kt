package com.nj.taskwall

import android.app.Application
import android.content.Context
import android.os.Process
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Redraws the wallpaper in the background, outliving the screen, at low CPU priority. */
object WallpaperJob {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val mutex = Mutex()
    @Volatile private var job: Job? = null

    fun request(ctx: Context, delayMs: Long, onDone: ((Boolean) -> Unit)? = null) {
        val app = ctx.applicationContext
        job?.cancel()
        job = scope.launch {
            if (delayMs > 0) delay(delayMs)
            var ok = true
            mutex.withLock {
                Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND)
                try { ok = WallpaperRenderer.refreshFromStorage(app) }
                catch (e: CancellationException) { throw e }
                catch (e: Exception) { e.printStackTrace(); ok = false }
                finally { Process.setThreadPriority(Process.THREAD_PRIORITY_DEFAULT) }
            }
            if (onDone != null) withContext(Dispatchers.Main) { onDone(ok) }
        }
    }
}

class TaskViewModel(app: Application) : AndroidViewModel(app) {
    private val dao = AppDb.get(app).dao()

    val tasks = dao.observeAll().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    private val _settings = MutableStateFlow(SettingsStore.load(app))
    val settings = _settings.asStateFlow()
    private var lastDeleted: TaskWithSubs? = null

    init {
        DailyReset.schedule(app)
        viewModelScope.launch(Dispatchers.Default) { DailyReset.runIfNeeded(app) }
    }

    fun update(block: (AppSettings) -> AppSettings) {
        val n = block(_settings.value)
        _settings.value = n
        SettingsStore.save(getApplication(), n)
    }

    /** Called when you leave the app: the wallpaper is only visible then anyway. */
    fun flushWallpaper() { if (_settings.value.autoUpdate) WallpaperJob.request(getApplication(), 0) }

    fun applyNow(onDone: (Boolean) -> Unit) = WallpaperJob.request(getApplication(), 0, onDone)

    private fun find(id: Long) = tasks.value.firstOrNull { it.task.id == id }

    fun addTasks(lines: List<String>) = viewModelScope.launch {
        var p = tasks.value.maxOfOrNull { it.task.position } ?: 0L
        lines.map { it.trim() }.filter { it.isNotEmpty() }.forEach { p += 1; dao.insert(Task(title = it, position = p)) }
    }

    fun addSubtasks(taskId: Long, lines: List<String>) = viewModelScope.launch {
        var p = find(taskId)?.subs?.maxOfOrNull { it.position } ?: 0L
        lines.map { it.trim() }.filter { it.isNotEmpty() }
            .forEach { p += 1; dao.insertSub(Subtask(taskId = taskId, title = it, position = p)) }
    }

    fun toggleTask(id: Long) = viewModelScope.launch {
        val t = find(id) ?: return@launch
        if (t.subs.isEmpty()) dao.update(t.task.copy(isDone = !t.task.isDone))
        else { val target = !t.done; t.subs.forEach { dao.updateSub(it.copy(isDone = target)) } }
    }

    fun toggleSub(taskId: Long, subId: Long) = viewModelScope.launch {
        val s = find(taskId)?.subs?.firstOrNull { it.id == subId } ?: return@launch
        dao.updateSub(s.copy(isDone = !s.isDone))
    }

    fun deleteSub(taskId: Long, subId: Long) = viewModelScope.launch {
        find(taskId)?.subs?.firstOrNull { it.id == subId }?.let { dao.deleteSub(it) }
    }

    fun deleteTask(id: Long) {
        lastDeleted = find(id)
        viewModelScope.launch { find(id)?.let { dao.delete(it.task) } }
    }

    fun undoDelete() = viewModelScope.launch {
        lastDeleted?.let { d -> dao.insert(d.task); d.subs.forEach { dao.insertSub(it) } }
        lastDeleted = null
    }

    fun saveOrder(ids: List<Long>) = viewModelScope.launch {
        ids.forEachIndexed { i, id ->
            find(id)?.let { if (it.task.position != i.toLong()) dao.update(it.task.copy(position = i.toLong())) }
        }
    }
}
