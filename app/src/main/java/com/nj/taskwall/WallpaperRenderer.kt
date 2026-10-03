package com.nj.taskwall

import android.app.WallpaperManager
import android.content.Context
import android.graphics.*
import android.util.DisplayMetrics
import android.view.WindowManager
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.ColorUtils
import kotlin.random.Random

object WallpaperRenderer {
    const val LOCK = 0
    const val HOME = 1
    const val BOTH = 2

    /** How far the "Middle" layout sits above the exact screen centre, in real centimetres. */
    private const val UP_SHIFT_CM = 1.0f

    private class TL(val t: TaskWithSubs, val lines: List<String>, val label: String?,
                     val subs: List<Pair<Subtask, List<String>>>)
    private class Lay(val s: Float, val items: List<TL>, val span: Float,
                      val text: Paint, val sub: Paint, val small: Paint)

    private fun al(color: Int, a: Float) = ColorUtils.setAlphaComponent(color, (a * 255).toInt())

    private fun ellipsize(p: Paint, line: String, maxW: Float): String {
        var s = line
        while (s.isNotEmpty() && p.measureText("$s…") > maxW) s = s.dropLast(1)
        return "$s…"
    }

    private fun wrap(p: Paint, text: String, maxW: Float, maxLines: Int): List<String> {
        val words = text.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }.toMutableList()
        if (words.isEmpty()) return listOf("")
        val lines = mutableListOf<String>()
        var cur = ""
        var i = 0
        while (i < words.size) {
            val wd = words[i]
            val trial = if (cur.isEmpty()) wd else "$cur $wd"
            if (p.measureText(trial) <= maxW) { cur = trial; i++ }
            else if (cur.isEmpty()) {
                val n = maxOf(p.breakText(wd, true, maxW, null), 1)
                lines.add(wd.take(n))
                words[i] = wd.drop(n)
                if (words[i].isEmpty()) i++
            } else { lines.add(cur); cur = "" }
        }
        if (cur.isNotEmpty()) lines.add(cur)
        if (lines.size <= maxLines) return lines
        val kept = lines.take(maxLines).toMutableList()
        kept[maxLines - 1] = ellipsize(p, kept[maxLines - 1], maxW)
        return kept
    }

    private fun background(c: Canvas, th: Theme, w: Float, h: Float) {
        if (!th.gradient) {
            c.drawColor(th.bgTop)
        } else {
            val p = Paint()
            p.shader = if (th.radial)
                RadialGradient(w * 0.5f, h * 0.32f, h * 0.75f, th.bgTop, th.bgBottom, Shader.TileMode.CLAMP)
            else
                LinearGradient(0f, 0f, 0f, h, th.bgTop, th.bgBottom, Shader.TileMode.CLAMP)
            c.drawRect(0f, 0f, w, h, p)
        }
        if (th.grain) {
            val n = (w * h / 150f).toInt()
            val pts = FloatArray(n * 2)
            val rnd = Random(7)
            for (i in 0 until n) { pts[2 * i] = rnd.nextFloat() * w; pts[2 * i + 1] = rnd.nextFloat() * h }
            val gp = Paint().apply {
                color = Color.argb(14, 255, 255, 255); strokeWidth = 1.6f; strokeCap = Paint.Cap.ROUND
            }
            c.drawPoints(pts, gp)
        }
    }

    private fun typeface(context: Context, kind: Int): Typeface = when (kind) {
        1 -> Typeface.create("monospace", Typeface.NORMAL)
        2 -> Typeface.create("serif", Typeface.NORMAL)
        else -> {
            val id = context.resources.getIdentifier("inter_light", "font", context.packageName)
            (if (id != 0) ResourcesCompat.getFont(context, id) else null)
                ?: Typeface.create("sans-serif-light", Typeface.NORMAL)
        }
    }

    private fun measure(list: List<TaskWithSubs>, hidden: Boolean, s: Float, vw: Float,
                        font: Typeface, dens: Float, comp: Int): Lay {
        val text = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = font; textSize = vw * 0.048f * s }
        val sub = Paint(text).apply { textSize = vw * 0.037f * s }
        val small = Paint(text).apply { textSize = vw * 0.030f * s }
        val margin = vw * 0.10f
        val rowH = vw * 0.125f * s * dens
        val sH = vw * 0.072f * s * dens
        val g = vw * 0.018f * s * dens
        val r = vw * 0.026f * s
        val lineGap = text.textSize * 1.22f
        val subGap = sub.textSize * 1.22f
        var total = rowH
        val items = list.map { t ->
            val done = t.done
            val subs = t.sortedSubs
            val label = when {
                done -> null
                subs.isNotEmpty() -> "${subs.count { it.isDone }}/${subs.size}"
                t.task.carried -> "carried"
                else -> null
            }
            val labelW = if (label != null) small.measureText(label) + vw * 0.03f else 0f
            val textX = margin + r * 2 + vw * 0.035f
            val lines = wrap(text, t.task.title, (vw - margin) - textX - labelW, 2)
            val subX = margin + r + vw * 0.085f
            val shown = if (done) emptyList() else if (comp == 2) subs.filter { !it.isDone } else subs
            val subLines = shown.map { it to wrap(sub, it.title, (vw - margin) - subX, 2) }
            var h = rowH + (lines.size - 1) * lineGap
            if (subLines.isNotEmpty())
                h += subLines.fold(0f) { a, p -> a + sH + (p.second.size - 1) * subGap } + g
            total += h
            TL(t, lines, label, subLines)
        }
        if (hidden) total += rowH
        return Lay(s, items, total - rowH, text, sub, small)
    }

    private fun pick(tasks: List<TaskWithSubs>, vw: Float, vh: Float, font: Typeface,
                     dens: Float, comp: Int): Pair<List<TaskWithSubs>, Lay> {
        var visible = tasks
        while (true) {
            var s = 1f
            while (s >= 0.5f) {
                val l = measure(visible, visible.size < tasks.size, s, vw, font, dens, comp)
                if (l.span <= vh * 0.50f) return visible to l
                s -= 0.03f
            }
            if (visible.size <= 1)
                return visible to measure(visible, visible.size < tasks.size, 0.5f, vw, font, dens, comp)
            visible = visible.dropLast(1)
        }
    }

    /** zoom > 1 compensates for launchers that enlarge the wallpaper (see "Home zoom fix"). */
    fun render(context: Context, tasks: List<TaskWithSubs>, st: AppSettings,
               width: Int, height: Int, zoom: Float, upPx: Float = 0f): Bitmap {
        val th = st.theme
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val w = width.toFloat()
        val h = height.toFloat()
        background(c, th, w, h)

        val font = typeface(context, st.font)
        val dens = when (st.density) { 0 -> 0.85f; 2 -> 1.18f; else -> 1f }
        val comp = st.completed

        val vw = w / zoom
        val vh = h / zoom
        c.save()
        c.translate((w - vw) / 2f, (h - vh) / 2f)

        val candidates = if (comp == 2) tasks.filter { !it.done } else tasks
        val (visible, lay) = pick(candidates, vw, vh, font, dens, comp)
        val s = lay.s
        val text = lay.text
        val subText = lay.sub
        val small = lay.small
        val margin = vw * 0.10f
        val rowH = vw * 0.125f * s * dens
        val sH = vw * 0.072f * s * dens
        val g = vw * 0.018f * s * dens
        val r = vw * 0.026f * s
        val lineGap = text.textSize * 1.22f
        val subGap = subText.textSize * 1.22f

        val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE; strokeWidth = vw * 0.003f; color = al(th.fg, 0.65f)
        }
        val track = Paint(stroke).apply { color = al(th.fg, 0.28f) }
        val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = th.accent }
        val tickP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE; strokeWidth = vw * 0.005f
            strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND; color = th.on
        }
        val arcP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE; strokeWidth = vw * 0.0055f
            strokeCap = Paint.Cap.ROUND; color = th.accent
        }
        val guide = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE; strokeWidth = vw * 0.0025f; color = al(th.fg, 0.18f)
        }
        val header = Paint(text).apply { textSize = vw * 0.032f; letterSpacing = 0.18f }

        val doneCount = tasks.count { it.done }
        val allDone = tasks.isNotEmpty() && doneCount == tasks.size
        header.color = if (allDone) th.accent else al(th.fg, 0.55f)
        val head = when {
            tasks.isEmpty() -> "NOTHING PLANNED YET"
            allDone -> "ALL DONE"
            else -> "TODAY  ·  $doneCount/${tasks.size}"
        }

        var y = when (st.position) {
            0 -> vh * 0.27f                                  // top: just under the clock
            2 -> vh * 0.80f - lay.span                       // bottom: just above the dock
            else -> vh * 0.5f - lay.span / 2f - upPx / zoom  // middle (nudged up)
        }
        y = maxOf(y, vh * 0.18f)
        c.drawText(head, margin, y, header)
        y += rowH

        val doneAlpha = if (comp == 1) 0.30f else 0.40f
        val doneStrike = comp != 1

        for (tl in lay.items) {
            val done = tl.t.done
            val subs = tl.t.sortedSubs
            val cx = margin + r
            val cy = y - text.textSize * 0.32f

            if (done) {
                c.drawCircle(cx, cy, r, fill)
                val p = Path().apply {
                    moveTo(cx - r * 0.45f, cy)
                    lineTo(cx - r * 0.1f, cy + r * 0.38f)
                    lineTo(cx + r * 0.5f, cy - r * 0.35f)
                }
                c.drawPath(p, tickP)
                text.color = al(th.fg, doneAlpha); text.isStrikeThruText = doneStrike
            } else {
                if (subs.isNotEmpty()) {
                    c.drawCircle(cx, cy, r, track)
                    val prog = subs.count { it.isDone } / subs.size.toFloat()
                    if (prog > 0f) c.drawArc(RectF(cx - r, cy - r, cx + r, cy + r), -90f, 360f * prog, false, arcP)
                } else {
                    c.drawCircle(cx, cy, r, stroke)
                }
                text.color = al(th.fg, 0.94f); text.isStrikeThruText = false
            }

            val textX = margin + r * 2 + vw * 0.035f
            tl.lines.forEachIndexed { i, ln -> c.drawText(ln, textX, y + i * lineGap, text) }
            tl.label?.let { lb ->
                small.color = if (subs.isNotEmpty()) th.accent else al(th.fg, 0.4f)
                c.drawText(lb, vw - margin - small.measureText(lb), y, small)
            }
            val pbLast = y + (tl.lines.size - 1) * lineGap
            y = pbLast + rowH

            if (tl.subs.isNotEmpty()) {
                val dotX = cx + vw * 0.055f
                val subX = dotX + vw * 0.03f
                val dotR = vw * 0.011f * s
                var yb = pbLast + rowH * 0.62f
                var lastCy = cy
                for ((sb, ls) in tl.subs) {
                    val sy = yb - subText.textSize * 0.32f
                    lastCy = sy
                    if (sb.isDone) {
                        c.drawCircle(dotX, sy, dotR, fill)
                        subText.color = al(th.fg, doneAlpha - 0.05f); subText.isStrikeThruText = doneStrike
                    } else {
                        c.drawCircle(dotX, sy, dotR, stroke)
                        subText.color = al(th.fg, 0.7f); subText.isStrikeThruText = false
                    }
                    ls.forEachIndexed { j, ln -> c.drawText(ln, subX, yb + j * subGap, subText) }
                    yb += sH + (ls.size - 1) * subGap
                }
                c.drawLine(cx, cy + r * 1.4f, cx, lastCy, guide)
                y = yb + rowH * 0.38f + g
            }
        }

        if (visible.size < candidates.size) {
            text.isStrikeThruText = false
            text.color = al(th.fg, 0.5f)
            c.drawText("+${candidates.size - visible.size} more", margin, y, text)
        }
        c.restore()
        return bmp
    }

    private fun realSize(context: Context): Triple<Int, Int, Float> {
        val dm = DisplayMetrics()
        @Suppress("DEPRECATION")
        (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay.getRealMetrics(dm)
        val w = minOf(dm.widthPixels, dm.heightPixels)
        val h = maxOf(dm.widthPixels, dm.heightPixels)
        val ydpi = if (dm.ydpi > 50f) dm.ydpi else dm.densityDpi.toFloat()
        return Triple(w, h, ydpi / 2.54f * UP_SHIFT_CM)
    }

    fun screenAspect(context: Context): Float {
        val (w, h, _) = realSize(context)
        return w.toFloat() / h
    }

    /** Small bitmap for the in-app preview, same layout as the real wallpaper. */
    fun preview(context: Context, tasks: List<TaskWithSubs>, st: AppSettings, home: Boolean): Bitmap {
        val (w, h, up) = realSize(context)
        val pw = 540
        val ph = (pw.toFloat() * h / w).toInt()
        return render(context, tasks, st, pw, ph, if (home) st.homeZoom / 100f else 1f, up * pw / w)
    }

    /** Returns false if this device does not allow setting the wallpaper. */
    fun update(context: Context, tasks: List<TaskWithSubs>, st: AppSettings): Boolean {
        val wm = WallpaperManager.getInstance(context)
        if (!wm.isWallpaperSupported || !wm.isSetWallpaperAllowed) return false
        val (w, h, up) = realSize(context)
        if (st.target == LOCK || st.target == BOTH)
            wm.setBitmap(render(context, tasks, st, w, h, 1f, up), null, true, WallpaperManager.FLAG_LOCK)
        if (st.target == HOME || st.target == BOTH)
            wm.setBitmap(render(context, tasks, st, w, h, st.homeZoom / 100f, up), null, true, WallpaperManager.FLAG_SYSTEM)
        return true
    }

    suspend fun refreshFromStorage(context: Context): Boolean =
        update(context, AppDb.get(context).dao().getAll(), SettingsStore.load(context))
}
