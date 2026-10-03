package com.nj.taskwall

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.VerticalAlignCenter
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** The real wallpaper renderer, shown inside a phone outline. */
@Composable
fun PhonePreview(tasks: List<TaskWithSubs>, st: AppSettings, home: Boolean, height: Dp, modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    val aspect = remember { WallpaperRenderer.screenAspect(ctx) }
    val bmp by produceState<ImageBitmap?>(null, tasks, st, home) {
        value = withContext(Dispatchers.Default) { WallpaperRenderer.preview(ctx, tasks, st, home).asImageBitmap() }
    }
    val shape = RoundedCornerShape(28.dp)
    Box(
        modifier.height(height).aspectRatio(aspect).clip(shape)
            .background(Color.Black).border(1.dp, Ob.Hair10, shape)
    ) {
        bmp?.let {
            // On the home screen many launchers enlarge the wallpaper; show that here too.
            val z = if (home) st.homeZoom / 100f else 1f
            Image(
                it, null, Modifier.fillMaxSize().graphicsLayer { scaleX = z; scaleY = z },
                contentScale = ContentScale.FillBounds
            )
        }
        if (!home) {
            Text(
                "09:41", Modifier.align(Alignment.TopCenter).padding(top = height * 0.07f),
                color = Color.White.copy(alpha = 0.85f), fontSize = (height.value * 0.10f).sp,
                fontWeight = FontWeight.ExtraLight
            )
        } else {
            Column(
                Modifier.align(Alignment.BottomCenter).padding(horizontal = 12.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    repeat(5) { Box(Modifier.size(height * 0.075f).clip(CircleShape).background(Color(0x26FFFFFF))) }
                }
                Spacer(Modifier.height(8.dp))
                Box(Modifier.fillMaxWidth().height(height * 0.065f).clip(RoundedCornerShape(50)).background(Color(0x26FFFFFF)))
            }
        }
    }
}

@Composable
fun WallpaperScreen(vm: TaskViewModel, snack: SnackbarHostState) {
    val tasks by vm.tasks.collectAsState()
    val st by vm.settings.collectAsState()
    var home by rememberSaveable { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f).padding(end = 12.dp)) {
                Text("Wallpaper Preview", color = Ob.Text, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
                Text("Keep your focus front and center", color = Ob.TextSecondary, fontSize = 14.sp)
            }
            Segmented(listOf("Lock", "Home"), if (home) 1 else 0, { home = it == 1 }, Modifier.width(150.dp))
        }
        Spacer(Modifier.height(16.dp))
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            PhonePreview(tasks, st, home, 440.dp)
        }
        Spacer(Modifier.height(16.dp))

        Row(Modifier.fillMaxWidth().obCard().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.VerticalAlignCenter, null, tint = Ob.Primary, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(12.dp))
            Text("Position", Modifier.weight(1f), color = Ob.Text, fontSize = 16.sp)
            Segmented(listOf("Top", "Middle", "Bottom"), st.position, { p -> vm.update { it.copy(position = p) } }, Modifier.width(210.dp))
        }
        Spacer(Modifier.height(12.dp))

        Row(Modifier.fillMaxWidth().obCard().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Auto-update", color = Ob.Text, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                Text("Redraw the wallpaper when you leave the app", color = Ob.TextSecondary, fontSize = 13.sp)
            }
            Switch(
                checked = st.autoUpdate, onCheckedChange = { on -> vm.update { it.copy(autoUpdate = on) } },
                colors = SwitchDefaults.colors(
                    checkedTrackColor = Ob.Secondary, checkedThumbColor = Ob.Text,
                    uncheckedTrackColor = Ob.Button, uncheckedThumbColor = Ob.Primary, uncheckedBorderColor = Ob.Hair10
                )
            )
        }
        Spacer(Modifier.height(12.dp))

        Column(Modifier.fillMaxWidth().obCard().padding(14.dp)) {
            Text("Home screen zoom fix", color = Ob.Text, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            Text("Some launchers enlarge wallpapers. Pick the step that fits yours.", color = Ob.TextSecondary, fontSize = 13.sp)
            Spacer(Modifier.height(10.dp))
            val steps = listOf(100, 110, 120, 130)
            Segmented(steps.map { "$it%" }, steps.indexOf(st.homeZoom).coerceAtLeast(0),
                { i -> vm.update { it.copy(homeZoom = steps[i]) } }, Modifier.fillMaxWidth())
        }
        Spacer(Modifier.height(16.dp))
        ApplyButton("Apply wallpaper now", vm, snack)
        Spacer(Modifier.height(24.dp))
    }
}
