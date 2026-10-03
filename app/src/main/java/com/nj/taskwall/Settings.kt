package com.nj.taskwall

import android.content.Context

data class AppSettings(
    val themeId: String = "noir",
    val target: Int = WallpaperRenderer.BOTH,   // 0 lock, 1 home, 2 both
    val homeZoom: Int = 120,
    val position: Int = 1,                      // 0 top, 1 middle, 2 bottom
    val density: Int = 1,                       // 0 compact, 1 comfortable, 2 spacious
    val completed: Int = 0,                     // 0 strike & keep, 1 fade, 2 hide
    val font: Int = 0,                          // 0 sans, 1 mono, 2 serif
    val autoUpdate: Boolean = true
) {
    val theme: Theme get() = Themes.byId(themeId)
}

object SettingsStore {
    private fun p(c: Context) = c.getSharedPreferences("settings", Context.MODE_PRIVATE)

    fun load(c: Context): AppSettings {
        val p = p(c)
        return AppSettings(
            themeId = p.getString("theme", "noir") ?: "noir",
            target = p.getInt("target", WallpaperRenderer.BOTH),
            homeZoom = p.getInt("homeZoom", 120),
            position = p.getInt("position", 1),
            density = p.getInt("density", 1),
            completed = p.getInt("completed", 0),
            font = p.getInt("font", 0),
            autoUpdate = p.getBoolean("autoUpdate", true)
        )
    }

    fun save(c: Context, s: AppSettings) {
        p(c).edit()
            .putString("theme", s.themeId).putInt("target", s.target).putInt("homeZoom", s.homeZoom)
            .putInt("position", s.position).putInt("density", s.density).putInt("completed", s.completed)
            .putInt("font", s.font).putBoolean("autoUpdate", s.autoUpdate)
            .apply()
    }
}
