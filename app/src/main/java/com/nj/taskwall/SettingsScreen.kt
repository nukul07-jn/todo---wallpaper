package com.nj.taskwall

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.StrikethroughS
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.VerticalAlignBottom
import androidx.compose.material.icons.outlined.VerticalAlignCenter
import androidx.compose.material.icons.outlined.VerticalAlignTop
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

@Composable
fun SettingsScreen(vm: TaskViewModel, snack: SnackbarHostState) {
    val tasks by vm.tasks.collectAsState()
    val st by vm.settings.collectAsState()
    val posNames = listOf("Top", "Middle", "Bottom")
    val densNames = listOf("Compact", "Comfortable", "Spacious")

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Tune, null, tint = Ob.Primary, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(6.dp))
            Text("APPEARANCE", color = Ob.TextSecondary, fontSize = 11.sp, letterSpacing = 0.06.em, fontWeight = FontWeight.Medium)
        }
        Text("Wallpaper Style", color = Ob.Text, fontSize = 26.sp, fontWeight = FontWeight.SemiBold)
        Text("Choose a quiet theme and layout for your wallpaper.", color = Ob.TextSecondary, fontSize = 14.sp)
        Spacer(Modifier.height(16.dp))

        Column(Modifier.fillMaxWidth().obCard().padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Live preview", Modifier.weight(1f), color = Ob.TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                InfoChip("${posNames[st.position.coerceIn(0, 2)]} • ${st.theme.name}")
            }
            Spacer(Modifier.height(10.dp))
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { PhonePreview(tasks, st, false, 300.dp) }
        }

        SectionHeader("Color & Theme", st.theme.name, "Dull, matte surfaces tuned for minimal OLED drain.")
        Text("Solid", color = Ob.TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(bottom = 6.dp))
        ThemeSwatches(Themes.solid, st.themeId) { id -> vm.update { it.copy(themeId = id) } }
        Spacer(Modifier.height(14.dp))
        Text("Gradient", color = Ob.TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(bottom = 6.dp))
        ThemeSwatches(Themes.gradient, st.themeId) { id -> vm.update { it.copy(themeId = id) } }

        SectionHeader("Layout & Safe Zone", subtitle = "Anchors the task tree relative to system widgets.")
        Text("Vertical Position", color = Ob.TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(bottom = 6.dp))
        Segmented(
            listOf("Top (Clock)", "Middle", "Bottom (Dock)"), st.position,
            { p -> vm.update { it.copy(position = p) } }, Modifier.fillMaxWidth(),
            icons = listOf(Icons.Outlined.VerticalAlignTop, Icons.Outlined.VerticalAlignCenter, Icons.Outlined.VerticalAlignBottom)
        )
        Spacer(Modifier.height(14.dp))
        Row {
            Text("Vertical Density", Modifier.weight(1f), color = Ob.TextSecondary, fontSize = 12.sp)
            Text(densNames[st.density.coerceIn(0, 2)], color = Ob.Text, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        }
        Spacer(Modifier.height(6.dp))
        Segmented(densNames, st.density, { d -> vm.update { it.copy(density = d) } }, Modifier.fillMaxWidth())
        Spacer(Modifier.height(14.dp))
        Text("Apply wallpaper to", color = Ob.TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(bottom = 6.dp))
        Segmented(listOf("Lock", "Home", "Both"), st.target, { t -> vm.update { it.copy(target = t) } }, Modifier.fillMaxWidth())

        SectionHeader("Completed Tasks", subtitle = "Determine how checked items show on your wallpaper.")
        Column(Modifier.fillMaxWidth().obCard().padding(6.dp)) {
            OptionRow(Icons.Outlined.StrikethroughS, "Strikethrough & Keep", "Marks the line visible with a soft muted line", st.completed == 0) { vm.update { it.copy(completed = 0) } }
            OptionRow(Icons.Outlined.WaterDrop, "Fade to 30%", "Recedes into the background without a strike", st.completed == 1) { vm.update { it.copy(completed = 1) } }
            OptionRow(Icons.Outlined.VisibilityOff, "Hide Immediately", "Keeps the wallpaper completely uncluttered", st.completed == 2) { vm.update { it.copy(completed = 2) } }
        }

        SectionHeader("Typography", subtitle = "Subdued font selection aligned with editorial journals.")
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            FontCard("Aa", "System Sans", "Clean & Neutral", FontFamily.SansSerif, st.font == 0, Modifier.weight(1f)) { vm.update { it.copy(font = 0) } }
            FontCard("01", "Minimal Mono", "Terminal vibe", FontFamily.Monospace, st.font == 1, Modifier.weight(1f)) { vm.update { it.copy(font = 1) } }
            FontCard("Aa", "Warm Serif", "Bookish grain", FontFamily.Serif, st.font == 2, Modifier.weight(1f)) { vm.update { it.copy(font = 2) } }
        }

        Spacer(Modifier.height(24.dp))
        ApplyButton("Apply style now", vm, snack)
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
fun ThemeSwatches(list: List<Theme>, current: String, onPick: (String) -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        list.forEach { t ->
            val sel = t.id == current
            Column(Modifier.clickable { onPick(t.id) }, horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier.size(64.dp).clip(shape).background(t.brush())
                        .border(if (sel) 1.5.dp else 1.dp, if (sel) Ob.Text else Ob.Hair10, shape),
                    contentAlignment = Alignment.Center
                ) {
                    if (sel) Icon(Icons.Filled.Check, t.name, tint = Color(t.accent), modifier = Modifier.size(22.dp))
                    else Box(Modifier.size(10.dp).clip(CircleShape).background(Color(t.accent)))
                }
                Spacer(Modifier.height(4.dp))
                Text(t.name, color = if (sel) Ob.Text else Ob.TextSecondary, fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun OptionRow(icon: ImageVector, title: String, subtitle: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
            .background(if (selected) Ob.Elevated else Color.Transparent)
            .clickable(onClick = onClick).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = if (selected) Ob.Text else Ob.TextSecondary, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = Ob.Text, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Text(subtitle, color = Ob.TextSecondary, fontSize = 13.sp)
        }
        RadioButton(
            selected = selected, onClick = onClick,
            colors = RadioButtonDefaults.colors(selectedColor = Ob.Accent, unselectedColor = Ob.Muted)
        )
    }
}

@Composable
fun FontCard(
    sample: String, name: String, sub: String, family: FontFamily,
    selected: Boolean, modifier: Modifier, onClick: () -> Unit
) {
    val shape = RoundedCornerShape(10.dp)
    Column(
        modifier.clip(shape).background(if (selected) Ob.Elevated else Ob.Subtle)
            .border(1.dp, if (selected) Ob.Secondary else Ob.Hair06, shape)
            .clickable(onClick = onClick).padding(12.dp)
    ) {
        Text(sample, color = Ob.Text, fontSize = 20.sp, fontFamily = family)
        Spacer(Modifier.height(6.dp))
        Text(name, color = if (selected) Ob.Text else Ob.TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        Text(sub, color = if (selected) Ob.Accent else Ob.Muted, fontSize = 11.sp)
    }
}
