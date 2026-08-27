package com.rstlab.pocketpetultra.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rstlab.pocketpetultra.game.PetState
import com.rstlab.pocketpetultra.game.SpeciesCatalog

@Composable
fun WalletBar(coins: Int, gems: Int, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(22.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("POCKETPET ULTRA", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                Text("マイウォレット", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                WalletValue("●", coins.toString(), "コイン")
                WalletValue("◆", gems.toString(), "ジェム")
            }
        }
    }
}

@Composable
private fun WalletValue(icon: String, value: String, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(icon, color = MaterialTheme.colorScheme.tertiary, fontSize = 18.sp)
        Column {
            Text(value, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun StatMeter(symbol: String, title: String, value: Float, modifier: Modifier = Modifier) {
    val safe = value.coerceIn(0f, 100f)
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("$symbol  $title", style = MaterialTheme.typography.labelMedium)
            Text("${safe.toInt()}%", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
        }
        LinearProgressIndicator(
            progress = { safe / 100f },
            modifier = Modifier.fillMaxWidth().height(7.dp),
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}

@Composable
fun RoomBackdrop(themeId: String, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val colors = when (themeId) {
        "theme_sunset" -> listOf(Color(0xFFFFE1C9), Color(0xFFF2C7D7), Color(0xFFB7CBEA))
        "theme_night" -> listOf(Color(0xFF1E2846), Color(0xFF38466E), Color(0xFF716786))
        "theme_sakura" -> listOf(Color(0xFFFFE6ED), Color(0xFFF7D1DE), Color(0xFFE1D9F2))
        else -> listOf(Color(0xFFE7F2ED), Color(0xFFF6EBDD), Color(0xFFE9E2F1))
    }
    Box(
        modifier = modifier.background(Brush.verticalGradient(colors), RoundedCornerShape(30.dp)),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@Composable
fun PetVisual(pet: PetState, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "pet-idle")
    val bob by transition.animateFloat(
        initialValue = 2f,
        targetValue = -8f,
        animationSpec = infiniteRepeatable(tween(1500), RepeatMode.Reverse),
        label = "pet-bob"
    )
    val breathe by transition.animateFloat(
        initialValue = 0.985f,
        targetValue = 1.025f,
        animationSpec = infiniteRepeatable(tween(1800), RepeatMode.Reverse),
        label = "pet-breathe"
    )

    val bodyColor = when (pet.speciesId) {
        "lumi" -> Color(0xFFD8C7F2)
        "nori" -> Color(0xFFB8D7AC)
        "pico" -> Color(0xFFFFD66D)
        "sora" -> Color(0xFFA7D7EE)
        else -> Color(0xFFF3D6C7)
    }
    val accent = when (pet.speciesId) {
        "lumi" -> Color(0xFF76589B)
        "nori" -> Color(0xFF55734F)
        "pico" -> Color(0xFF8A6A22)
        "sora" -> Color(0xFF4D7890)
        else -> Color(0xFF9B6F61)
    }
    val stageScale = when (pet.stage) {
        3 -> 1.10f
        2 -> 1.04f
        else -> 0.96f
    }

    Box(
        modifier = modifier.graphicsLayer {
            translationY = bob
            scaleX = breathe * stageScale
            scaleY = breathe * stageScale
        },
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            drawOval(
                color = Color.Black.copy(alpha = 0.12f),
                topLeft = Offset(w * 0.21f, h * 0.78f),
                size = Size(w * 0.58f, h * 0.10f)
            )

            when (pet.speciesId) {
                "lumi" -> {
                    drawOval(bodyColor, Offset(w * 0.26f, h * 0.04f), Size(w * 0.17f, h * 0.39f))
                    drawOval(bodyColor, Offset(w * 0.57f, h * 0.04f), Size(w * 0.17f, h * 0.39f))
                }
                "nori" -> {
                    val leftLeaf = Path().apply {
                        moveTo(w * 0.34f, h * 0.32f)
                        lineTo(w * 0.14f, h * 0.12f)
                        lineTo(w * 0.40f, h * 0.18f)
                        close()
                    }
                    val rightLeaf = Path().apply {
                        moveTo(w * 0.66f, h * 0.32f)
                        lineTo(w * 0.86f, h * 0.12f)
                        lineTo(w * 0.60f, h * 0.18f)
                        close()
                    }
                    drawPath(leftLeaf, bodyColor)
                    drawPath(rightLeaf, bodyColor)
                }
                "pico" -> {
                    val leftEar = Path().apply {
                        moveTo(w * 0.34f, h * 0.30f)
                        lineTo(w * 0.20f, h * 0.04f)
                        lineTo(w * 0.45f, h * 0.19f)
                        close()
                    }
                    val rightEar = Path().apply {
                        moveTo(w * 0.66f, h * 0.30f)
                        lineTo(w * 0.80f, h * 0.04f)
                        lineTo(w * 0.55f, h * 0.19f)
                        close()
                    }
                    drawPath(leftEar, bodyColor)
                    drawPath(rightEar, bodyColor)
                }
                "sora" -> {
                    drawCircle(accent.copy(alpha = 0.7f), radius = w * 0.055f, center = Offset(w * 0.36f, h * 0.19f))
                    drawCircle(accent.copy(alpha = 0.7f), radius = w * 0.055f, center = Offset(w * 0.64f, h * 0.19f))
                }
                else -> {
                    drawCircle(bodyColor, radius = w * 0.12f, center = Offset(w * 0.31f, h * 0.28f))
                    drawCircle(bodyColor, radius = w * 0.12f, center = Offset(w * 0.69f, h * 0.28f))
                }
            }

            drawOval(
                color = bodyColor,
                topLeft = Offset(w * 0.20f, h * 0.20f),
                size = Size(w * 0.60f, h * 0.60f)
            )
            drawOval(
                color = Color.White.copy(alpha = 0.28f),
                topLeft = Offset(w * 0.30f, h * 0.29f),
                size = Size(w * 0.40f, h * 0.34f)
            )

            if (pet.sleeping) {
                drawLine(accent, Offset(w * 0.35f, h * 0.49f), Offset(w * 0.43f, h * 0.49f), strokeWidth = 5f)
                drawLine(accent, Offset(w * 0.57f, h * 0.49f), Offset(w * 0.65f, h * 0.49f), strokeWidth = 5f)
            } else {
                drawCircle(accent, radius = w * 0.025f, center = Offset(w * 0.39f, h * 0.47f))
                drawCircle(accent, radius = w * 0.025f, center = Offset(w * 0.61f, h * 0.47f))
                drawCircle(Color.White, radius = w * 0.009f, center = Offset(w * 0.382f, h * 0.462f))
                drawCircle(Color.White, radius = w * 0.009f, center = Offset(w * 0.602f, h * 0.462f))
            }
            drawCircle(accent.copy(alpha = 0.65f), radius = w * 0.017f, center = Offset(w * 0.50f, h * 0.55f))
            drawLine(accent.copy(alpha = 0.7f), Offset(w * 0.47f, h * 0.60f), Offset(w * 0.50f, h * 0.615f), strokeWidth = 4f)
            drawLine(accent.copy(alpha = 0.7f), Offset(w * 0.50f, h * 0.615f), Offset(w * 0.53f, h * 0.60f), strokeWidth = 4f)
        }

        if (pet.outfitId != null) {
            val symbol = when (pet.outfitId) {
                "outfit_crown" -> "♛"
                "outfit_hoodie" -> "▰"
                else -> "🎀"
            }
            Text(
                symbol,
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 2.dp),
                fontSize = if (pet.outfitId == "outfit_crown") 34.sp else 28.sp
            )
        }
        if (pet.sleeping) {
            Text("Z z", modifier = Modifier.align(Alignment.TopEnd).padding(10.dp), fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun PetIdentity(pet: PetState, modifier: Modifier = Modifier) {
    val species = SpeciesCatalog.find(pet.speciesId)
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(pet.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(
            "${species.name} ・ Lv.${pet.level} ・ ${stageLabel(pet.stage)}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

fun stageLabel(stage: Int): String = when (stage) {
    3 -> "進化体"
    2 -> "成長体"
    else -> "幼体"
}

@Composable
fun SoftCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Box(Modifier.padding(16.dp)) { content() }
    }
}

@Composable
fun EmptyHint(text: String, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
        Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
