package com.mahmoudrateb.player

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MRTheme {
                val nav = rememberNavController()
                val vm: MainViewModel = viewModel()
                val toChannels = { nav.navigate("channels") }
                val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
                    if (uri != null) vm.loadFile(uri) { toChannels() }
                }
                NavHost(nav, startDestination = "home") {
                    composable("home") {
                        HomeScreen(
                            vm,
                            onM3u = { nav.navigate("m3u") },
                            onXtream = { nav.navigate("xtream") },
                            onFile = { picker.launch(arrayOf("*/*")) },
                            onMac = { nav.navigate("mac") }
                        )
                    }
                    composable("m3u") { M3uScreen(vm, { nav.popBackStack() }) { toChannels() } }
                    composable("xtream") { XtreamScreen(vm, { nav.popBackStack() }) { toChannels() } }
                    composable("mac") { MacScreen(vm, { nav.popBackStack() }) { toChannels() } }
                    composable("channels") {
                        ChannelsScreen(vm, { nav.popBackStack() }) { ch ->
                            vm.selected = ch
                            nav.navigate("player")
                        }
                    }
                    composable("player") {
                        vm.selected?.let { PlayerScreen(vm, it) { nav.popBackStack() } }
                    }
                }
            }
        }
    }
}
