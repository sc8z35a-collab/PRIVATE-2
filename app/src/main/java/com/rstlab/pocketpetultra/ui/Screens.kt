package com.rstlab.pocketpetultra.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rstlab.pocketpetultra.game.Catalog
import com.rstlab.pocketpetultra.game.DailyQuest
import com.rstlab.pocketpetultra.game.GameRepository
import com.rstlab.pocketpetultra.game.GameRules
import com.rstlab.pocketpetultra.game.ItemType
import com.rstlab.pocketpetultra.game.ShopItem
import com.rstlab.pocketpetultra.game.SpeciesCatalog
import kotlinx.coroutines.delay
import kotlin.random.Random

@Composable
fun HomeScreen(repository: GameRepository, notify: (String) -> Unit, modifier: Modifier = Modifier) {
    val state = repository.state
    val pet = state.activePet
    var foodPicker by remember { mutableStateOf(false) }
    var toyPicker by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { WalletBar(state.coins, state.gems) }
        item {
            RoomBackdrop(state.roomThemeId, Modifier.fillMaxWidth().height(320.dp)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    PetVisual(pet, Modifier.size(220.dp))
                    PetIdentity(pet)
                    if (pet.sleeping) {
                        Text("睡眠中", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
        item {
            SoftCard(Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    val needed = GameRules.nextLevelXp(pet.level)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("成長", fontWeight = FontWeight.Bold)
                        Text("EXP ${pet.xp} / $needed", style = MaterialTheme.typography.labelMedium)
                    }
                    LinearProgressIndicator(
                        progress = { (pet.xp.toFloat() / needed.toFloat()).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        "親密度 ${pet.bond.toInt()} ・ ${SpeciesCatalog.find(pet.speciesId).tagline}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        item {
            SoftCard(Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("コンディション", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    StatMeter("●", "おなか", pet.hunger)
                    StatMeter("♥", "ごきげん", pet.happiness)
                    StatMeter("✦", "清潔", pet.cleanliness)
                    StatMeter("▲", "元気", pet.energy)
                }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(onClick = { foodPicker = true }, modifier = Modifier.weight(1f)) { Text("ごはん") }
                    Button(onClick = { toyPicker = true }, modifier = Modifier.weight(1f)) { Text("遊ぶ") }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FilledTonalButton(onClick = { notify(repository.pat()) }, modifier = Modifier.weight(1f)) { Text("なでる") }
                    FilledTonalButton(onClick = { notify(repository.clean()) }, modifier = Modifier.weight(1f)) { Text("おそうじ") }
                    FilledTonalButton(onClick = { notify(repository.toggleSleep()) }, modifier = Modifier.weight(1f)) {
                        Text(if (pet.sleeping) "起こす" else "寝かせる")
                    }
                }
            }
        }
        item {
            SoftCard(Modifier.fillMaxWidth()) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("デイリーボーナス", fontWeight = FontWeight.Bold)
                        Text("連続 ${state.dailyStreak} 日", style = MaterialTheme.typography.bodySmall)
                    }
                    FilledTonalButton(onClick = { notify(repository.claimDailyReward()) }) { Text("受け取る") }
                }
            }
        }
    }

    if (foodPicker) {
        InventoryPickerDialog(
            title = "ごはんを選ぶ",
            items = Catalog.items.filter { it.type == ItemType.FOOD && (state.inventory[it.id] ?: 0) > 0 },
            stateCount = { state.inventory[it.id] ?: 0 },
            onDismiss = { foodPicker = false },
            onPick = {
                foodPicker = false
                notify(repository.feed(it.id))
            }
        )
    }
    if (toyPicker) {
        InventoryPickerDialog(
            title = "おもちゃを選ぶ",
            items = Catalog.items.filter { it.type == ItemType.TOY && (state.inventory[it.id] ?: 0) > 0 },
            stateCount = { state.inventory[it.id] ?: 0 },
            onDismiss = { toyPicker = false },
            onPick = {
                toyPicker = false
                notify(repository.play(it.id))
            }
        )
    }
}

@Composable
private fun InventoryPickerDialog(
    title: String,
    items: List<ShopItem>,
    stateCount: (ShopItem) -> Int,
    onDismiss: () -> Unit,
    onPick: (ShopItem) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            if (items.isEmpty()) {
                Text("使えるアイテムがありません。ショップで購入できます。")
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items.forEach { item ->
                        Card(
                            modifier = Modifier.fillMaxWidth().clickable { onPick(item) },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Row(
                                Modifier.fillMaxWidth().padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(item.title, fontWeight = FontWeight.SemiBold)
                                    Text(item.description, style = MaterialTheme.typography.bodySmall)
                                }
                                Text("×${stateCount(item)}")
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("閉じる") } }
    )
}

@Composable
fun ShopScreen(repository: GameRepository, notify: (String) -> Unit, modifier: Modifier = Modifier) {
    val state = repository.state
    var filter by rememberSaveable { mutableStateOf("ALL") }
    val filters = listOf(
        "ALL" to "すべて",
        "FOOD" to "フード",
        "TOY" to "おもちゃ",
        "OUTFIT" to "服",
        "ROOM" to "部屋",
        "PET" to "ペット"
    )
    val filtered = Catalog.items.filter { item ->
        when (filter) {
            "FOOD" -> item.type == ItemType.FOOD
            "TOY" -> item.type == ItemType.TOY
            "OUTFIT" -> item.type == ItemType.OUTFIT
            "ROOM" -> item.type == ItemType.FURNITURE || item.type == ItemType.THEME
            "PET" -> item.type == ItemType.PET
            else -> true
        }
    }

    LazyColumn(
        modifier = modifier,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { WalletBar(state.coins, state.gems) }
        item {
            Text("ショップ", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("ゲーム内通貨だけで購入できます。実課金はありません。", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                filters.forEach { (key, label) ->
                    FilterChip(selected = filter == key, onClick = { filter = key }, label = { Text(label) })
                }
            }
        }
        items(filtered, key = { it.id }) { item ->
            val owned = when (item.type) {
                ItemType.PET -> item.payload?.let { species -> state.pets.any { it.speciesId == species } } == true
                else -> item.isUnique && (state.inventory[item.id] ?: 0) > 0
            }
            val count = state.inventory[item.id] ?: 0
            ShopCard(
                item = item,
                owned = owned,
                count = count,
                onBuy = { notify(repository.buy(item)) }
            )
        }
        item { Spacer(Modifier.height(12.dp)) }
    }
}

@Composable
private fun ShopCard(item: ShopItem, owned: Boolean, count: Int, onBuy: () -> Unit) {
    SoftCard(Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    Text(item.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(item.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (!item.isUnique && count > 0) Text("所持 $count", style = MaterialTheme.typography.labelMedium)
            }
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val price = buildString {
                    if (item.coinPrice > 0) append("● ${item.coinPrice}")
                    if (item.gemPrice > 0) {
                        if (isNotEmpty()) append("  +  ")
                        append("◆ ${item.gemPrice}")
                    }
                }
                Text(price, fontWeight = FontWeight.SemiBold)
                Button(onClick = onBuy, enabled = !owned) { Text(if (owned) "所有済み" else "購入") }
            }
        }
    }
}

@Composable
fun MiniGameScreen(repository: GameRepository, notify: (String) -> Unit, modifier: Modifier = Modifier) {
    var running by rememberSaveable { mutableStateOf(false) }
    var secondsLeft by rememberSaveable { mutableIntStateOf(10) }
    var score by rememberSaveable { mutableIntStateOf(0) }
    var targetX by remember { mutableIntStateOf(110) }
    var targetY by remember { mutableIntStateOf(120) }

    LaunchedEffect(running) {
        if (running) {
            while (secondsLeft > 0) {
                delay(1_000L)
                secondsLeft -= 1
            }
            running = false
            notify(repository.rewardMiniGame(score))
        }
    }

    LazyColumn(
        modifier = modifier,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { WalletBar(repository.state.coins, repository.state.gems) }
        item {
            Text("スターキャッチ", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("10秒間で動くスターをできるだけ多くタップ。スコアに応じてコインを獲得します。", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            SoftCard(Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    ScoreBlock("残り", if (running) "${secondsLeft}秒" else "—")
                    ScoreBlock("スコア", score.toString())
                    ScoreBlock("最大報酬", "650")
                }
            }
        }
        item {
            Box(
                Modifier.fillMaxWidth().height(370.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(28.dp))
            ) {
                if (running) {
                    Box(
                        Modifier.offset(x = targetX.dp, y = targetY.dp).size(72.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                            .clickable {
                                score += 1
                                targetX = Random.nextInt(8, 230)
                                targetY = Random.nextInt(8, 285)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("★", fontSize = 34.sp, color = MaterialTheme.colorScheme.onPrimary)
                    }
                } else {
                    Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("★", fontSize = 72.sp, color = MaterialTheme.colorScheme.primary)
                        Text("準備OK？", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        item {
            Button(
                onClick = {
                    score = 0
                    secondsLeft = 10
                    targetX = Random.nextInt(8, 230)
                    targetY = Random.nextInt(8, 285)
                    running = true
                },
                enabled = !running,
                modifier = Modifier.fillMaxWidth()
            ) { Text(if (running) "プレイ中…" else "ゲームスタート") }
        }
        item {
            Text(
                "35点以上ならジェム1個のボーナス。報酬は1ゲーム最大650コインに制限されています。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ScoreBlock(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun PetCollectionScreen(repository: GameRepository, notify: (String) -> Unit, modifier: Modifier = Modifier) {
    val state = repository.state
    val active = state.activePet
    var renameDialog by remember { mutableStateOf(false) }
    var renameText by remember(active.id) { mutableStateOf(active.name) }

    LazyColumn(
        modifier = modifier,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { WalletBar(state.coins, state.gems) }
        item {
            Text("ペットコレクション", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("${state.pets.size}匹と暮らしています。ショップのたまごから仲間を増やせます。", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        items(state.pets, key = { it.id }) { pet ->
            val selected = pet.id == state.activePetId
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    RoomBackdrop(state.roomThemeId, Modifier.size(112.dp)) {
                        PetVisual(pet, Modifier.size(96.dp))
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(pet.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("${SpeciesCatalog.find(pet.speciesId).name} ・ Lv.${pet.level} ・ ${stageLabel(pet.stage)}")
                        Text("親密度 ${pet.bond.toInt()}", style = MaterialTheme.typography.bodySmall)
                        if (selected) Text("いま一緒にいる", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                    }
                    if (!selected) {
                        OutlinedButton(onClick = { notify(repository.selectPet(pet.id)) }) { Text("選ぶ") }
                    }
                }
            }
        }
        item {
            SoftCard(Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("${active.name} のカスタマイズ", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    OutlinedButton(
                        onClick = {
                            renameText = active.name
                            renameDialog = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("名前を変更") }

                    Text("服", fontWeight = FontWeight.SemiBold)
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = active.outfitId == null,
                            onClick = { notify(repository.equipOutfit(null)) },
                            label = { Text("なし") }
                        )
                        Catalog.items.filter { it.type == ItemType.OUTFIT && (state.inventory[it.id] ?: 0) > 0 }.forEach { item ->
                            FilterChip(
                                selected = active.outfitId == item.id,
                                onClick = { notify(repository.equipOutfit(item.id)) },
                                label = { Text(item.title) }
                            )
                        }
                    }

                    Text("ルームテーマ", fontWeight = FontWeight.SemiBold)
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = state.roomThemeId == "theme_default",
                            onClick = { notify(repository.setRoomTheme("theme_default")) },
                            label = { Text("やわらかルーム") }
                        )
                        Catalog.items.filter { it.type == ItemType.THEME && (state.inventory[it.id] ?: 0) > 0 }.forEach { item ->
                            FilterChip(
                                selected = state.roomThemeId == item.id,
                                onClick = { notify(repository.setRoomTheme(item.id)) },
                                label = { Text(item.title) }
                            )
                        }
                    }
                }
            }
        }
        item {
            SoftCard(Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("家具コレクション", fontWeight = FontWeight.Bold)
                    val furniture = Catalog.items.filter { it.type == ItemType.FURNITURE && (state.inventory[it.id] ?: 0) > 0 }
                    if (furniture.isEmpty()) Text("まだ家具を持っていません", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    furniture.forEach { item -> Text("• ${item.title}") }
                }
            }
        }
    }

    if (renameDialog) {
        AlertDialog(
            onDismissRequest = { renameDialog = false },
            title = { Text("名前を変更") },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it.take(16) },
                    singleLine = true,
                    label = { Text("名前") }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    renameDialog = false
                    notify(repository.renameActivePet(renameText))
                }) { Text("保存") }
            },
            dismissButton = { TextButton(onClick = { renameDialog = false }) { Text("キャンセル") } }
        )
    }
}

@Composable
fun MoreScreen(repository: GameRepository, notify: (String) -> Unit, modifier: Modifier = Modifier) {
    val state = repository.state
    val clipboard = LocalClipboardManager.current
    var importDialog by remember { mutableStateOf(false) }
    var importText by remember { mutableStateOf("") }
    var resetDialog by remember { mutableStateOf(false) }
    val diagnostics = repository.diagnostics()

    LazyColumn(
        modifier = modifier,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { WalletBar(state.coins, state.gems) }
        item {
            Text("デイリー & 実績", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("毎日の小さな目標と長期実績。報酬は重複受取できません。", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            SoftCard(Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("ログインボーナス", fontWeight = FontWeight.Bold)
                        Text("現在 ${state.dailyStreak} 日連続", style = MaterialTheme.typography.bodySmall)
                    }
                    Button(onClick = { notify(repository.claimDailyReward()) }) { Text("受取") }
                }
            }
        }
        item { SectionTitle("今日のクエスト") }
        items(Catalog.dailyQuests, key = { it.id }) { quest ->
            QuestCard(quest, state, onClaim = { notify(repository.claimDailyQuest(quest)) })
        }
        item { SectionTitle("実績") }
        items(Catalog.achievements, key = { it.id }) { achievement ->
            val unlocked = achievement.isUnlocked(state)
            val claimed = achievement.id in state.claimedAchievements
            SoftCard(Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(achievement.title, fontWeight = FontWeight.Bold)
                    Text(achievement.description, style = MaterialTheme.typography.bodySmall)
                    Text(rewardText(achievement.reward.coins, achievement.reward.gems), color = MaterialTheme.colorScheme.primary)
                    Button(
                        onClick = { notify(repository.claimAchievement(achievement)) },
                        enabled = unlocked && !claimed,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(if (claimed) "受取済み" else if (unlocked) "報酬を受け取る" else "未達成") }
                }
            }
        }
        item { SectionTitle("インベントリ") }
        item {
            SoftCard(Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    val entries = state.inventory.filterValues { it > 0 }.toList()
                    if (entries.isEmpty()) Text("アイテムはありません")
                    entries.forEach { (id, count) ->
                        val item = Catalog.findItem(id)
                        Text("• ${item?.title ?: id}  ×$count")
                    }
                }
            }
        }
        item { SectionTitle("セーブ & バックアップ") }
        item {
            SoftCard(Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Text("ゲームは端末内に自動保存されます。バックアップ文字列をコピーして別端末へ移すこともできます。", style = MaterialTheme.typography.bodySmall)
                    OutlinedButton(
                        onClick = {
                            clipboard.setText(AnnotatedString(repository.exportSave()))
                            notify("バックアップをクリップボードへコピーしました")
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("バックアップをコピー") }
                    OutlinedButton(onClick = { importDialog = true }, modifier = Modifier.fillMaxWidth()) { Text("バックアップから復元") }
                    TextButton(onClick = { resetDialog = true }, modifier = Modifier.fillMaxWidth()) { Text("すべてのデータを初期化") }
                }
            }
        }
        item { SectionTitle("自己診断") }
        item {
            SoftCard(Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    diagnostics.forEach { (name, ok) ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(name, style = MaterialTheme.typography.bodySmall)
                            Text(if (ok) "OK" else "ERROR", fontWeight = FontWeight.Bold, color = if (ok) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
                        }
                    }
                    HorizontalDivider()
                    Text("ネット接続権限なし / 実課金なし / ローカル保存", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
        item { Spacer(Modifier.height(16.dp)) }
    }

    if (importDialog) {
        AlertDialog(
            onDismissRequest = { importDialog = false },
            title = { Text("バックアップから復元") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("PocketPet Ultraのバックアップ文字列を貼り付けてください。")
                    OutlinedTextField(
                        value = importText,
                        onValueChange = { importText = it },
                        minLines = 5,
                        maxLines = 9,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val result = repository.importSave(importText)
                    importDialog = false
                    importText = ""
                    notify(result)
                }) { Text("復元") }
            },
            dismissButton = { TextButton(onClick = { importDialog = false }) { Text("キャンセル") } }
        )
    }

    if (resetDialog) {
        AlertDialog(
            onDismissRequest = { resetDialog = false },
            title = { Text("本当に初期化しますか？") },
            text = { Text("ペット・通貨・購入品・実績を含むゲームデータが初期状態に戻ります。") },
            confirmButton = {
                TextButton(onClick = {
                    resetDialog = false
                    notify(repository.reset())
                }) { Text("初期化する") }
            },
            dismissButton = { TextButton(onClick = { resetDialog = false }) { Text("やめる") } }
        )
    }
}

@Composable
private fun QuestCard(quest: DailyQuest, state: com.rstlab.pocketpetultra.game.GameState, onClaim: () -> Unit) {
    val progress = quest.progress(state).coerceAtMost(quest.target)
    val key = "${state.daily.day}:${quest.id}"
    val claimed = key in state.claimedDailyQuests
    SoftCard(Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(quest.title, fontWeight = FontWeight.Bold)
                Text("$progress / ${quest.target}")
            }
            Text(quest.description, style = MaterialTheme.typography.bodySmall)
            LinearProgressIndicator(progress = { progress.toFloat() / quest.target.toFloat() }, modifier = Modifier.fillMaxWidth())
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(rewardText(quest.reward.coins, quest.reward.gems), color = MaterialTheme.colorScheme.primary)
                Button(onClick = onClaim, enabled = progress >= quest.target && !claimed) { Text(if (claimed) "受取済み" else "受取") }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
}

private fun rewardText(coins: Int, gems: Int): String = buildString {
    if (coins > 0) append("● $coins")
    if (gems > 0) {
        if (isNotEmpty()) append("  ")
        append("◆ $gems")
    }
}
