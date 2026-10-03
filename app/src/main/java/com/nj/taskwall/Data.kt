package com.nj.taskwall

import android.content.Context
import androidx.compose.runtime.Immutable
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Immutable
@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val isDone: Boolean = false,
    val position: Long = 0,
    val carried: Boolean = false
)

@Immutable
@Entity(
    tableName = "subtasks",
    foreignKeys = [ForeignKey(entity = Task::class, parentColumns = ["id"],
        childColumns = ["taskId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("taskId")]
)
data class Subtask(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val taskId: Long,
    val title: String,
    val isDone: Boolean = false,
    val position: Long = 0
)

@Immutable
data class TaskWithSubs(
    @Embedded val task: Task,
    @Relation(parentColumn = "id", entityColumn = "taskId") val subs: List<Subtask>
) {
    val sortedSubs: List<Subtask> get() = subs.sortedBy { it.position }
    // A task with subtasks is done when all of them are; otherwise it uses its own flag.
    val done: Boolean get() = if (subs.isEmpty()) task.isDone else subs.all { it.isDone }
}

@Dao
interface TaskDao {
    @Transaction @Query("SELECT * FROM tasks ORDER BY position ASC, id ASC")
    fun observeAll(): Flow<List<TaskWithSubs>>
    @Transaction @Query("SELECT * FROM tasks ORDER BY position ASC, id ASC")
    suspend fun getAll(): List<TaskWithSubs>
    @Insert suspend fun insert(t: Task): Long
    @Update suspend fun update(t: Task)
    @Delete suspend fun delete(t: Task)
    @Insert suspend fun insertSub(s: Subtask)
    @Update suspend fun updateSub(s: Subtask)
    @Delete suspend fun deleteSub(s: Subtask)
    @Query("DELETE FROM subtasks WHERE isDone = 1") suspend fun deleteDoneSubtasks()
    @Query("UPDATE tasks SET carried = 1") suspend fun markAllCarried()
}

@Database(entities = [Task::class, Subtask::class], version = 2, exportSchema = false)
abstract class AppDb : RoomDatabase() {
    abstract fun dao(): TaskDao

    companion object {
        @Volatile private var inst: AppDb? = null
        fun get(c: Context): AppDb = inst ?: synchronized(this) {
            inst ?: Room.databaseBuilder(c.applicationContext, AppDb::class.java, "tasks.db")
                .fallbackToDestructiveMigration()
                .build().also { inst = it }
        }
    }
}
