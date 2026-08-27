package com.rstlab.pocketpetultra

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.rstlab.pocketpetultra.game.GameRepository
import com.rstlab.pocketpetultra.ui.PocketPetApp
import com.rstlab.pocketpetultra.ui.PocketPetTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val repository = GameRepository(applicationContext)
        setContent {
            PocketPetTheme {
                PocketPetApp(repository)
            }
        }
    }
}
