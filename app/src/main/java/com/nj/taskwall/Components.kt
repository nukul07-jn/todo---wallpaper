package com.nj.taskwall

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

/** "Calm Obsidian" palette from the design spec. */
object Ob {
    val Canvas = Color(0xFF0D1117)
    val Subtle = Color(0xFF161B22)
    val Elevated = Color(0xFF1C2128)
    val Highlight = Color(0xFF262C36)
    val Button = Color(0xFF21262D)
    val Hair = Color(0x14FFFFFF)
    val Hair06 = Color(0x0FFFFFFF)
    val Hair10 = Color(0x1AFFFFFF)
    val Primary = Color(0xFF8B95A5)
    val Secondary = Color(0xFF6C7A9C)
    val Accent = Color(0xFFB8C6EC)
    val Text = Color(0xFFF0F6FC)
    val TextSecondary = Color(0xFF8B949E)
    val Muted = Color(0xFF484F58)
    val Error = Color(0xFFFFB4AB)
}

fun Theme.brush(): Brush = when {
    !gradient -> SolidColor(Color(bgTop))
    radial -> Brush.radialGradient(listOf(Color(bgTop), Color(bgBottom)))
    else -> Brush.verticalGradient(listOf(Color(bgTop), Color(bgBottom)))
}

fun Modifier.obCard(radius: Int = 12): Modifier {
    val shape = RoundedCornerShape(radius.dp)
    return this.clip(shape).background(Ob.Subtle).border(1.dp, Ob.Hair06, shape)
}

@Composable
fun QuietButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(8.dp)
    Box(
        modifier.clip(shape).background(Ob.Button).border(1.dp, Ob.Hair10, shape)
            .clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = Ob.Text, fontSize = 13.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.04.em)
    }
}

@Composable
fun Segmented(
    options: List<String>, selected: Int, onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier, icons: List<ImageVector>? = null
) {
    Row(modifier.clip(RoundedCornerShape(10.dp)).background(Color(0xFF0A0E14)).padding(3.dp)) {
        options.forEachIndexed { i, label ->
            val sel = i == selected
            Column(
                Modifier.weight(1f).clip(RoundedCornerShape(8.dp))
                    .background(if (sel) Ob.Highlight else Color.Transparent)
                    .clickable { onSelect(i) }.padding(vertical = 9.dp, horizontal = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (icons != null) {
                    Icon(icons[i], null, tint = if (sel) Ob.Text else Ob.TextSecondary, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.height(4.dp))
                }
                Text(label, color = if (sel) Ob.Text else Ob.TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
fun InfoChip(text: String) {
    Box(Modifier.clip(RoundedCornerShape(6.dp)).background(Ob.Elevated).padding(horizontal = 8.dp, vertical = 3.dp)) {
        Text(text, color = Ob.TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun SectionHeader(title: String, trailing: String? = null, subtitle: String? = null) {
    Column(Modifier.padding(top = 24.dp, bottom = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, Modifier.weight(1f), color = Ob.Text, fontSize = 18.sp, fontWeight = FontWeight.Medium)
            if (trailing != null) Text(trailing, color = Ob.TextSecondary, fontSize = 13.sp)
        }
        if (subtitle != null) Text(subtitle, Modifier.padding(top = 2.dp), color = Ob.TextSecondary, fontSize = 14.sp)
    }
}

/** Input bar: Enter adds a line, pasting several lines adds several items. */
@Composable
fun QuickField(
    placeholder: String, onSubmit: (List<String>) -> Unit, modifier: Modifier = Modifier,
    leading: ImageVector? = null, showButton: Boolean = true, autoFocus: Boolean = false
) {
    var input by remember { mutableStateOf("") }
    var focused by remember { mutableStateOf(false) }
    val fr = remember { FocusRequester() }
    LaunchedEffect(Unit) { if (autoFocus) fr.requestFocus() }
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier.fillMaxWidth().clip(shape).background(Ob.Subtle)
            .border(1.dp, if (focused) Ob.Secondary else Ob.Hair, shape)
            .padding(start = 14.dp, end = if (showButton) 8.dp else 14.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (leading != null) {
            Icon(leading, null, tint = Ob.Primary, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))
        }
        BasicTextField(
            value = input,
            onValueChange = { v -> if ('\n' in v) { onSubmit(v.lines()); input = "" } else input = v },
            modifier = Modifier.weight(1f).focusRequester(fr).onFocusChanged { focused = it.isFocused },
            textStyle = TextStyle(color = Ob.Text, fontSize = 16.sp),
            cursorBrush = SolidColor(Ob.Primary),
            maxLines = 4,
            decorationBox = { inner ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (input.isEmpty()) Text(placeholder, color = Ob.Muted, fontSize = 16.sp)
                    inner()
                }
            }
        )
        if (showButton) {
            Spacer(Modifier.width(8.dp))
            QuietButton("Add", onClick = { onSubmit(input.lines()); input = "" })
        }
    }
}

@Composable
fun ApplyButton(label: String, vm: TaskViewModel, snack: SnackbarHostState) {
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color(0xFFBDC7D8))
            .clickable(enabled = !busy) {
                busy = true
                vm.applyNow { ok ->
                    busy = false
                    scope.launch {
                        snack.showSnackbar(if (ok) "Wallpaper updated" else "This phone doesn't allow changing the wallpaper")
                    }
                }
            }.padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Outlined.Image, null, tint = Color(0xFF27313E), modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(if (busy) "Updating…" else label, color = Color(0xFF27313E), fontSize = 16.sp, fontWeight = FontWeight.Medium)
    }
}
