package com.nj.taskwall

data class Theme(
    val id: String, val name: String,
    val gradient: Boolean,
    val bgTop: Int, val bgBottom: Int,   // gradient: top->bottom, or centre->edge when radial
    val radial: Boolean, val grain: Boolean,
    val fg: Int, val accent: Int, val on: Int,
    val light: Boolean
)

object Themes {
    private fun flat(id: String, name: String, bg: Long, ac: Long,
                      fg: Long = 0xFFFFFFFF, light: Boolean = false) =
        Theme(id, name, false, bg.toInt(), bg.toInt(), false, false, fg.toInt(), ac.toInt(), bg.toInt(), light)

    private fun grad(id: String, name: String, top: Long, bottom: Long, ac: Long, radial: Boolean = false) =
        Theme(id, name, true, top.toInt(), bottom.toInt(), radial, true, 0xFFFFFFFF.toInt(), ac.toInt(), bottom.toInt(), false)

    val solid = listOf(
        flat("noir", "Noir", 0xFF000000, 0xFFFFD60A),
        flat("midnight", "Midnight", 0xFF0A1122, 0xFF7CC4FF),
        flat("forest", "Forest", 0xFF0B1511, 0xFF6EE7A8, 0xFFF0FFF7),
        flat("dusk", "Dusk", 0xFF160F24, 0xFFC4A1FF, 0xFFFAF4FF),
        flat("rose", "Rose", 0xFF1C0F14, 0xFFFF8FB1, 0xFFFFF4F7),
        flat("paper", "Paper", 0xFFF3EDE2, 0xFFD9542B, 0xFF1F1B16, light = true)
    )
    val gradient = listOf(
        grad("aurora", "Aurora", 0xFF04070F, 0xFF0C3B3A, 0xFF5EEAD4),
        grad("sunset", "Sunset", 0xFF12091F, 0xFF6B2A3A, 0xFFFFB067),
        grad("deepsea", "Deep sea", 0xFF17427A, 0xFF02050C, 0xFF7CC4FF, radial = true),
        grad("nebula", "Nebula", 0xFF3A1A66, 0xFF07030F, 0xFFF0A6FF, radial = true)
    )
    val all = solid + gradient
    fun byId(id: String): Theme = all.firstOrNull { it.id == id } ?: all[0]
}
