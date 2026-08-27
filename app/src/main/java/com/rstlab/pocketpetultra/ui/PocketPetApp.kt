package com.rstlab.pocketpetultra.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.rstlab.pocketpetultra.game.GameRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class RootTab(val label: String, val symbol: String) {
    HOME("ホーム", "⌂"),
    SHOP("ショップ", "▣"),
    PLAY("ゲーム", "★"),
    PETS("ペット", "♥"),
    MORE("その他", "•••")
}

@Composable
fun PocketPetApp(repository: GameRepository) {
    var selectedTab by rememberSaveable { mutableStateOf(RootTab.HOME) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val notify: (String) -> Unit = { message ->
        scope.launch { snackbar.showSnackbar(message) }
    }

    LaunchedEffect(Unit) {
        while (isActive) {
            delay(30_000L)
            repository.tick()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            NavigationBar {
                RootTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        icon = { Text(tab.symbol) },
                        label = { Text(tab.label) }
                    )
                }
            }
        }
    ) { padding ->
        val modifier = Modifier.fillMaxSize().padding(padding)
        when (selectedTab) {
            RootTab.HOME -> HomeScreen(repository, notify, modifier)
            RootTab.SHOP -> ShopScreen(repository, notify, modifier)
            RootTab.PLAY -> MiniGameScreen(repository, notify, modifier)
            RootTab.PETS -> PetCollectionScreen(repository, notify, modifier)
            RootTab.MORE -> MoreScreen(repository, notify, modifier)
        }
    }
}
