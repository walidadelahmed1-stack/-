package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Screen
import com.example.ui.GameViewModel
import com.example.ui.screens.*
import com.example.ui.theme.CyberVoid
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private val viewModel: GameViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cleanWebViewSeedCache()
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val currentScreen by viewModel.screen.collectAsStateWithLifecycle()
                val progress by viewModel.progress.collectAsStateWithLifecycle()

                Scaffold(
                    contentWindowInsets = WindowInsets.systemBars,
                    containerColor = CyberVoid,
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .background(CyberVoid)
                    ) {
                        AnimatedContent(
                            targetState = currentScreen,
                            transitionSpec = {
                                fadeIn() togetherWith fadeOut()
                            },
                            label = "screen_transition"
                        ) { screen ->
                            when (screen) {
                                is Screen.Home -> {
                                    HomeScreen(
                                        viewModel = viewModel,
                                        progress = progress,
                                        onNavigate = { viewModel.navigateTo(it) }
                                    )
                                }
                                is Screen.Map -> {
                                    MapScreen(
                                        viewModel = viewModel,
                                        progress = progress,
                                        initialChapterId = screen.initialChapterId,
                                        onNavigate = { viewModel.navigateTo(it) }
                                    )
                                }
                                is Screen.Play -> {
                                    PlayScreen(
                                        viewModel = viewModel,
                                        progress = progress,
                                        onNavigate = { viewModel.navigateTo(it) }
                                    )
                                }
                                is Screen.Puzzle -> {
                                    PuzzleScreen(
                                        viewModel = viewModel,
                                        levelId = screen.levelId,
                                        onNavigate = { viewModel.navigateTo(it) }
                                    )
                                }
                                is Screen.Story -> {
                                    StoryScreen(
                                        viewModel = viewModel,
                                        chapterId = screen.chapterId,
                                        onNavigate = { viewModel.navigateTo(it) }
                                    )
                                }
                                is Screen.Codex -> {
                                    CodexScreen(
                                        viewModel = viewModel,
                                        onNavigate = { viewModel.navigateTo(it) }
                                    )
                                }
                                is Screen.Leaderboard -> {
                                    LeaderboardScreen(
                                        viewModel = viewModel,
                                        onNavigate = { viewModel.navigateTo(it) }
                                    )
                                }
                            }
                        }

                        // Top-level AdMob Dialog Overlay (Simulated & Fallback Ads)
                        val adState by viewModel.adSimulationState.collectAsStateWithLifecycle()
                        com.example.ui.components.AdMobOverlayDialog(
                            state = adState,
                            onDismiss = { viewModel.adMobManager.dismissSimulation() }
                        )
                    }
                }
            }
        }
    }

    private fun cleanWebViewSeedCache() {
        try {
            val dataDir = applicationContext.applicationInfo.dataDir
            val webViewDir = java.io.File(dataDir, "app_webview")
            if (webViewDir.exists() && webViewDir.isDirectory) {
                val seedFiles = webViewDir.listFiles { file ->
                    file.name.contains("variations_seed")
                }
                seedFiles?.forEach { file ->
                    try {
                        file.delete()
                    } catch (_: Exception) {}
                }
            }
        } catch (_: Throwable) {}
    }
}
