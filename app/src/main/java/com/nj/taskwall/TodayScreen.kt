package com.nj.taskwall

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayScreen(vm: TaskViewModel, onStyle: () -> Unit, onPreview: () -> Unit) {
    val tasks by vm.tasks.collectAsState()
    val settings by vm.settings.collectAsState()
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val snack = remember { SnackbarHostState() }
    val listState = rememberLazyListState()

    var subFor by remember { mutableStateOf<Long?>(null) }
    var ordered by remember { mutableStateOf(tasks) }
    var dragId by remember { mutableStateOf<Long?>(null) }
    var dragDy by remember { mutableFloatStateOf(0f) }
    val lock = remember { booleanArrayOf(false) }
    LaunchedEffect(tasks) { if (dragId == null) ordered = tasks }
    LaunchedEffect(ordered) { withFrameNanos { }; lock[0] = false }

    fun trySwap(id: Long) {
        if (lock[0]) return
        val info = listState.layoutInfo.visibleItemsInfo
        val cur = info.firstOrNull { it.key == id } ?: return
        val center = cur.offset + cur.size / 2f + dragDy
        val tgt = info.firstOrNull { it.key != id && center >= it.offset && center <= it.offset + it.size } ?: return
        val from = ordered.indexOfFirst { it.task.id == id }
        val to = ordered.indexOfFirst { it.task.id == tgt.key }
        if (from < 0 || to < 0) return
        val newOffset = if (to > from) tgt.offset + tgt.size - cur.size else tgt.offset
        ordered = ordered.toMutableList().apply { add(to, removeAt(from)) }
        dragDy += (cur.offset - newOffset)
        lock[0] = true
    }

    val doneN = tasks.count { it.done }
    val status = when {
        tasks.isEmpty() -> "Nothing planned yet"
        doneN == tasks.size -> "All done"
        else -> "$doneN of ${tasks.size} done"
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Today", color = Ob.Text, fontSize = 32.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.02).em)
                    Text(status, color = Ob.TextSecondary, fontSize = 16.sp)
                }
                Row(
                    Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onStyle).padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Outlined.Palette, null, tint = Ob.Primary, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Style", color = Ob.Primary, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                }
            }
            Spacer(Modifier.height(12.dp))
            QuickField("Add a task, or paste a list", { vm.addTasks(it) }, leading = Icons.Outlined.EditNote)
            Spacer(Modifier.height(16.dp))

            if (ordered.isEmpty()) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("Nothing here yet. Add your first task above.", color = Ob.Muted, fontSize = 14.sp)
                }
            } else {
                LazyColumn(
                    Modifier.weight(1f), state = listState,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 12.dp)
                ) {
                    items(ordered, key = { it.task.id }) { t ->
                        val id = t.task.id
                        TaskCard(
                            t = t, isDragging = dragId == id, dragDy = dragDy, addingSub = subFor == id,
                            onToggle = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                vm.toggleTask(id)
                            },
                            onDelete = {
                                vm.deleteTask(id)
                                scope.launch {
                                    snack.currentSnackbarData?.dismiss()
                                    val r = snack.showSnackbar("Task deleted", "Undo", duration = SnackbarDuration.Short)
                                    if (r == SnackbarResult.ActionPerformed) vm.undoDelete()
                                }
                            },
                            onToggleSub = { sid ->
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                vm.toggleSub(id, sid)
                            },
                            onDeleteSub = { sid -> vm.deleteSub(id, sid) },
                            onAddSubClick = { subFor = if (subFor == id) null else id },
                            onAddSubs = { lines -> vm.addSubtasks(id, lines) },
                            handleModifier = Modifier.pointerInput(id) {
                                detectDragGestures(
                                    onDragStart = { dragId = id; dragDy = 0f },
                                    onDragEnd = {
                                        vm.saveOrder(ordered.map { it.task.id })
                                        dragId = null; dragDy = 0f
                                    },
                                    onDragCancel = { dragId = null; dragDy = 0f; ordered = tasks },
                                    onDrag = { change, amount ->
                                        change.consume()
                                        dragDy += amount.y
                                        trySwap(id)
                                    }
                                )
                            }
                        )
                    }
                }
            }

            WallpaperStrip(settings.autoUpdate, onPreview)
            Spacer(Modifier.height(12.dp))
        }
        SnackbarHost(snack, Modifier.align(Alignment.BottomCenter).padding(bottom = 72.dp))
    }
}

@Composable
fun WallpaperStrip(auto: Boolean, onPreview: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().obCard().padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Outlined.AutoAwesome, null, tint = Ob.Accent, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Text(
            if (auto) "Wallpaper updates when you leave the app" else "Auto-update is off",
            Modifier.weight(1f), color = Ob.TextSecondary, fontSize = 14.sp
        )
        QuietButton("Preview", onPreview)
    }
}

@Composable
fun TickCircle(done: Boolean, doneSubs: Int, totalSubs: Int, diameter: Dp) {
    Canvas(Modifier.size(diameter)) {
        val r = size.minDimension / 2f - 1.dp.toPx()
        val c = center
        if (done) {
            drawCircle(Ob.Accent, r, c)
            val p = Path().apply {
                moveTo(c.x - r * 0.45f, c.y)
                lineTo(c.x - r * 0.1f, c.y + r * 0.38f)
                lineTo(c.x + r * 0.5f, c.y - r * 0.35f)
            }
            drawPath(p, Ob.Canvas, style = Stroke(1.6.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        } else {
            drawCircle(Ob.Muted, r, c, style = Stroke(1.5.dp.toPx()))
            if (totalSubs > 0 && doneSubs > 0) {
                drawArc(
                    Ob.Accent, -90f, 360f * doneSubs / totalSubs, false,
                    topLeft = Offset(c.x - r, c.y - r), size = Size(2 * r, 2 * r),
                    style = Stroke(2.dp.toPx(), cap = StrokeCap.Round)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskCard(
    t: TaskWithSubs, isDragging: Boolean, dragDy: Float, addingSub: Boolean,
    onToggle: () -> Unit, onDelete: () -> Unit,
    onToggleSub: (Long) -> Unit, onDeleteSub: (Long) -> Unit,
    onAddSubClick: () -> Unit, onAddSubs: (List<String>) -> Unit,
    handleModifier: Modifier
) {
    val done = t.done
    val subs = t.sortedSubs
    val doneSubs = subs.count { it.isDone }
    val shape = RoundedCornerShape(12.dp)
    val titleAlpha by animateFloatAsState(if (done) 0.5f else 1f, label = "title")

    val toggle by rememberUpdatedState(onToggle)
    val delete by rememberUpdatedState(onDelete)
    val state = rememberSwipeToDismissBoxState(confirmValueChange = { v ->
        when (v) {
            SwipeToDismissBoxValue.StartToEnd -> { toggle(); false }
            SwipeToDismissBoxValue.EndToStart -> { delete(); true }
            else -> false
        }
    })

    SwipeToDismissBox(
        state = state,
        modifier = Modifier.zIndex(if (isDragging) 1f else 0f)
            .graphicsLayer { translationY = if (isDragging) dragDy else 0f },
        backgroundContent = {
            val dir = state.dismissDirection
            Box(
                Modifier.fillMaxSize().padding(horizontal = 20.dp),
                contentAlignment = if (dir == SwipeToDismissBoxValue.StartToEnd) Alignment.CenterStart else Alignment.CenterEnd
            ) {
                if (dir == SwipeToDismissBoxValue.StartToEnd) Text(if (done) "Undo" else "Done", color = Ob.Accent, fontSize = 14.sp)
                if (dir == SwipeToDismissBoxValue.EndToStart) Text("Delete", color = Ob.Error, fontSize = 14.sp)
            }
        }
    ) {
        Column(
            Modifier.fillMaxWidth().clip(shape).background(Ob.Subtle).border(1.dp, Ob.Hair06, shape)
                .padding(start = 14.dp, end = 6.dp, top = 2.dp, bottom = 4.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(
                    Modifier.weight(1f).clickable(onClick = onToggle).padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TickCircle(done, doneSubs, subs.size, 20.dp)
                    Spacer(Modifier.width(12.dp))
                    Text(
                        t.task.title, color = if (done) Ob.Muted else Ob.Text.copy(alpha = titleAlpha),
                        fontSize = 16.sp, fontWeight = FontWeight.Medium,
                        textDecoration = if (done) TextDecoration.LineThrough else null
                    )
                }
                if (!done && subs.isNotEmpty()) {
                    Text(
                        "$doneSubs/${subs.size}",
                        style = TextStyle(
                            color = Ob.Accent, fontSize = 11.sp, fontWeight = FontWeight.Medium,
                            letterSpacing = 0.06.em, fontFeatureSettings = "tnum"
                        )
                    )
                } else if (!done && t.task.carried) {
                    Text("carried", color = Ob.Muted, fontSize = 11.sp, letterSpacing = 0.06.em)
                }
                IconButton(onClick = onAddSubClick, modifier = Modifier.size(44.dp)) {
                    Icon(Icons.Filled.Add, "Add subtask", tint = Ob.Primary.copy(alpha = 0.7f))
                }
                Box(handleModifier.size(44.dp), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.DragHandle, "Reorder", tint = Ob.Primary.copy(alpha = 0.45f))
                }
            }

            if (!done && subs.isNotEmpty()) {
                Column(
                    Modifier.padding(start = 10.dp, bottom = 8.dp)
                        .drawBehind {
                            drawLine(Ob.Hair, Offset(0f, 0f), Offset(0f, size.height), 1.dp.toPx())
                        }
                        .padding(start = 24.dp)
                ) {
                    subs.forEach { s ->
                        Row(
                            Modifier.fillMaxWidth().heightIn(min = 40.dp).clickable { onToggleSub(s.id) },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TickCircle(s.isDone, 0, 0, 14.dp)
                            Spacer(Modifier.width(12.dp))
                            Text(
                                s.title, Modifier.weight(1f), fontSize = 15.sp,
                                color = if (s.isDone) Ob.Muted else Ob.Text.copy(alpha = 0.9f),
                                textDecoration = if (s.isDone) TextDecoration.LineThrough else null
                            )
                            IconButton(onClick = { onDeleteSub(s.id) }, modifier = Modifier.size(40.dp)) {
                                Icon(Icons.Filled.Close, "Delete subtask", tint = Ob.Primary.copy(alpha = 0.4f), modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }

            if (addingSub) {
                QuickField(
                    "Add a subtask", { onAddSubs(it) },
                    Modifier.padding(start = 0.dp, end = 8.dp, bottom = 8.dp),
                    showButton = false, autoFocus = true
                )
            }
        }
    }
}
