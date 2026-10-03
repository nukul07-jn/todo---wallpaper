package com.nj.taskwall

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    private val vm: TaskViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        setContent { ObTheme { Root(vm) } }
    }

    override fun onStop() {
        super.onStop()
        vm.flushWallpaper()
    }
}

@Composable
fun ObTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            background = Ob.Canvas, surface = Ob.Subtle, onSurface = Ob.Text, onBackground = Ob.Text,
            primary = Ob.Secondary, onPrimary = Ob.Canvas, outline = Ob.Muted
        )
    ) {
        CompositionLocalProvider(LocalContentColor provides Ob.Text, content = content)
    }
}

@Composable
fun Root(vm: TaskViewModel) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val snack = remember { SnackbarHostState() }
    val imeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0

    Scaffold(
        modifier = Modifier.fillMaxSize().imePadding(),
        containerColor = Ob.Canvas,
        snackbarHost = { SnackbarHost(snack) },
        topBar = { TopBar(listOf("Today", "Wallpaper", "Settings")[tab]) { tab = 2 } },
        bottomBar = { if (!imeVisible) BottomNav(tab) { tab = it } }
    ) { pad ->
        Box(Modifier.padding(pad).fillMaxSize()) {
            when (tab) {
                0 -> TodayScreen(vm, onStyle = { tab = 2 }, onPreview = { tab = 1 })
                1 -> WallpaperScreen(vm, snack)
                else -> SettingsScreen(vm, snack)
            }
        }
    }
}

@Composable
fun TopBar(label: String, onTune: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Row(
        Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(40.dp).clip(shape).background(Ob.Subtle).border(1.dp, Ob.Hair, shape),
            contentAlignment = Alignment.Center
        ) { Icon(Icons.Outlined.CheckCircle, null, tint = Ob.Accent, modifier = Modifier.size(22.dp)) }
        Spacer(Modifier.width(12.dp))
        Text("TaskPaper", color = Ob.Text, fontSize = 18.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.width(8.dp))
        Text(label, color = Ob.Muted, fontSize = 13.sp)
        Spacer(Modifier.weight(1f))
        IconButton(onClick = onTune) {
            Icon(Icons.Outlined.Tune, "Wallpaper style", tint = Ob.Primary)
        }
    }
}

@Composable
fun BottomNav(selected: Int, onSelect: (Int) -> Unit) {
    val items: List<Pair<String, ImageVector>> = listOf(
        "Today" to Icons.Outlined.CheckCircle,
        "Wallpaper" to Icons.Outlined.Image,
        "Settings" to Icons.Outlined.Settings
    )
    Column {
        HorizontalDivider(color = Ob.Hair)
        NavigationBar(containerColor = Ob.Canvas, tonalElevation = 0.dp) {
            items.forEachIndexed { i, (label, icon) ->
                NavigationBarItem(
                    selected = selected == i, onClick = { onSelect(i) },
                    icon = { Icon(icon, label) },
                    label = { Text(label, fontSize = 12.sp, fontWeight = FontWeight.Medium) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Ob.Text, selectedTextColor = Ob.Text,
                        unselectedIconColor = Ob.Muted, unselectedTextColor = Ob.Muted,
                        indicatorColor = Color.Transparent
                    )
                )
            }
        }
    }
}
