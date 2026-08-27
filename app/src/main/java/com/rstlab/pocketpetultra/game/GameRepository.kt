package com.rstlab.pocketpetultra.game

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import kotlin.math.min

class GameRepository(context: Context) {
    private val prefs = context.getSharedPreferences("pocket_pet_ultra_save", Context.MODE_PRIVATE)

    var state by mutableStateOf(GameState())
        private set

    init {
        val loaded = runCatching {
            prefs.getString(KEY_SAVE, null)?.let(::decode) ?: GameState()
        }.getOrElse { GameState() }
        val now = System.currentTimeMillis()
        val normalized = normalize(ensureDayState(loaded))
        state = normalize(applyElapsed(normalized, now)).copy(lastUpdatedAt = now)
        persist()
    }

    fun tick() {
        val now = System.currentTimeMillis()
        val withDay = ensureDayState(state)
        val elapsed = applyElapsed(withDay, now)
        commit(elapsed.copy(lastUpdatedAt = now), touchTime = false)
    }

    fun buy(item: ShopItem): String {
        var current = ensureDayState(state)
        if (item.isUnique && ownsUnique(current, item)) {
            return "すでに持っています"
        }
        if (!GameRules.canSpend(current.coins, current.gems, item.coinPrice, item.gemPrice)) {
            return "通貨が足りません"
        }

        val newCoins = current.coins - item.coinPrice
        val newGems = current.gems - item.gemPrice
        var newInventory = current.inventory
        var newPets = current.pets

        if (item.type == ItemType.PET) {
            val speciesId = item.payload ?: return "ペット情報が壊れています"
            val species = SpeciesCatalog.find(speciesId)
            val newPet = PetState(
                id = "pet-$speciesId-${System.currentTimeMillis()}",
                speciesId = speciesId,
                name = species.name
            )
            newPets = current.pets + newPet
        } else {
            val oldCount = current.inventory[item.id] ?: 0
            newInventory = current.inventory + (item.id to GameRules.safeCount(oldCount + 1))
        }

        current = current.copy(
            coins = newCoins,
            gems = newGems,
            pets = newPets,
            inventory = newInventory,
            counters = current.counters.copy(purchases = current.counters.purchases + 1),
            daily = current.daily.copy(purchases = current.daily.purchases + 1)
        )
        commit(current)
        return "${item.title}を購入しました"
    }

    fun feed(foodId: String): String {
        val item = Catalog.findItem(foodId) ?: return "アイテムが見つかりません"
        if (item.type != ItemType.FOOD) return "これは食べ物ではありません"
        val count = state.inventory[foodId] ?: 0
        if (count <= 0) return "${item.title}を持っていません"
        if (state.activePet.hunger >= 98f) return "おなかはいっぱいです"

        var current = ensureDayState(state)
        current = updateActive(current) { pet ->
            gainXp(
                pet.copy(
                    sleeping = false,
                    hunger = GameRules.clampStat(pet.hunger + item.hungerGain),
                    happiness = GameRules.clampStat(pet.happiness + item.happinessGain),
                    energy = GameRules.clampStat(pet.energy + item.energyGain),
                    bond = GameRules.clampStat(pet.bond + 1.2f)
                ),
                8
            )
        }
        val newCount = GameRules.safeCount(count - 1)
        current = current.copy(
            inventory = if (newCount == 0) current.inventory - foodId else current.inventory + (foodId to newCount),
            counters = current.counters.copy(feeds = current.counters.feeds + 1),
            daily = current.daily.copy(feeds = current.daily.feeds + 1)
        )
        current = rewardLevelUpsIfNeeded(state, current)
        commit(current)
        return "${item.title}をあげました"
    }

    fun play(toyId: String): String {
        val item = Catalog.findItem(toyId) ?: return "おもちゃが見つかりません"
        if (item.type != ItemType.TOY) return "これはおもちゃではありません"
        if ((state.inventory[toyId] ?: 0) <= 0) return "${item.title}を持っていません"
        if (state.activePet.energy < 8f) return "疲れているので、少し休ませましょう"
        if (state.activePet.happiness >= 99f) return "今はとても満足しています"

        var current = ensureDayState(state)
        current = updateActive(current) { pet ->
            gainXp(
                pet.copy(
                    sleeping = false,
                    happiness = GameRules.clampStat(pet.happiness + item.happinessGain),
                    energy = GameRules.clampStat(pet.energy - if (toyId == "toy_wand") 12f else 8f),
                    hunger = GameRules.clampStat(pet.hunger - 3f),
                    bond = GameRules.clampStat(pet.bond + if (toyId == "toy_wand") 2.8f else 1.8f)
                ),
                if (toyId == "toy_wand") 15 else 10
            )
        }
        current = current.copy(
            counters = current.counters.copy(plays = current.counters.plays + 1),
            daily = current.daily.copy(plays = current.daily.plays + 1)
        )
        current = rewardLevelUpsIfNeeded(state, current)
        commit(current)
        return "一緒に遊びました！"
    }

    fun clean(): String {
        if (state.activePet.cleanliness >= 96f) return "もうピカピカです"
        val before = state
        var current = updateActive(state) { pet ->
            gainXp(
                pet.copy(
                    cleanliness = GameRules.clampStat(pet.cleanliness + 34f),
                    happiness = GameRules.clampStat(pet.happiness + 3f),
                    bond = GameRules.clampStat(pet.bond + 0.8f)
                ),
                6
            )
        }
        current = current.copy(counters = current.counters.copy(cleans = current.counters.cleans + 1))
        current = rewardLevelUpsIfNeeded(before, current)
        commit(current)
        return "きれいになりました"
    }

    fun pat(): String {
        val now = System.currentTimeMillis()
        if (now - state.lastPatAt < 2_000L) return "もう少しゆっくり撫でてあげてください"
        val current = updateActive(state) { pet ->
            pet.copy(
                happiness = GameRules.clampStat(pet.happiness + 1.5f),
                bond = GameRules.clampStat(pet.bond + 0.7f)
            )
        }.copy(
            lastPatAt = now,
            counters = state.counters.copy(pats = state.counters.pats + 1)
        )
        commit(current)
        return "うれしそうです"
    }

    fun toggleSleep(): String {
        val pet = state.activePet
        val goingToSleep = !pet.sleeping
        val current = updateActive(state) { it.copy(sleeping = goingToSleep) }
        commit(current)
        return if (goingToSleep) "おやすみモードにしました" else "起きました！"
    }

    fun renameActivePet(newName: String): String {
        val clean = newName.trim().take(16)
        if (clean.isBlank()) return "名前を入力してください"
        commit(updateActive(state) { it.copy(name = clean) })
        return "名前を変更しました"
    }

    fun selectPet(petId: String): String {
        if (state.pets.none { it.id == petId }) return "ペットが見つかりません"
        commit(state.copy(activePetId = petId))
        return "このペットと過ごします"
    }

    fun equipOutfit(outfitId: String?): String {
        if (outfitId == null) {
            commit(updateActive(state) { it.copy(outfitId = null) })
            return "服を外しました"
        }
        val item = Catalog.findItem(outfitId) ?: return "服が見つかりません"
        if (item.type != ItemType.OUTFIT || (state.inventory[outfitId] ?: 0) <= 0) {
            return "その服は持っていません"
        }
        commit(updateActive(state) { it.copy(outfitId = outfitId) })
        return "${item.title}を着せました"
    }

    fun setRoomTheme(themeId: String): String {
        if (themeId != "theme_default") {
            val item = Catalog.findItem(themeId)
            if (item?.type != ItemType.THEME || (state.inventory[themeId] ?: 0) <= 0) {
                return "そのルームテーマは持っていません"
            }
        }
        commit(state.copy(roomThemeId = themeId))
        return "ルームを変更しました"
    }

    fun claimDailyReward(): String {
        val today = today()
        val current = ensureDayState(state)
        if (current.lastDailyClaimDay == today) return "今日のログインボーナスは受取済みです"
        val streak = if (current.lastDailyClaimDay == today - 1L) min(current.dailyStreak + 1, 99) else 1
        val coins = 120 + min(streak, 7) * 45
        val gems = if (streak % 7 == 0) 2 else 0
        commit(
            current.copy(
                coins = GameRules.safeCoins(current.coins.toLong() + coins),
                gems = GameRules.safeGems(current.gems.toLong() + gems),
                lastDailyClaimDay = today,
                dailyStreak = streak,
                counters = current.counters.copy(
                    totalCoinsEarned = GameRules.safeCoins(current.counters.totalCoinsEarned.toLong() + coins)
                )
            )
        )
        return if (gems > 0) "$coinsコイン + $gemsジェム獲得！ 連続${streak}日" else "$coinsコイン獲得！ 連続${streak}日"
    }

    fun claimDailyQuest(quest: DailyQuest): String {
        val current = ensureDayState(state)
        val key = "${today()}:${quest.id}"
        if (key in current.claimedDailyQuests) return "受取済みです"
        if (quest.progress(current) < quest.target) return "まだ達成していません"
        commit(
            addReward(current, quest.reward).copy(
                claimedDailyQuests = current.claimedDailyQuests + key
            )
        )
        return "クエスト報酬を受け取りました"
    }

    fun claimAchievement(achievement: Achievement): String {
        if (achievement.id in state.claimedAchievements) return "受取済みです"
        if (!achievement.isUnlocked(state)) return "まだ達成していません"
        commit(
            addReward(state, achievement.reward).copy(
                claimedAchievements = state.claimedAchievements + achievement.id
            )
        )
        return "実績報酬を受け取りました"
    }

    fun rewardMiniGame(rawScore: Int): String {
        val score = rawScore.coerceIn(0, 100)
        val reward = GameRules.miniGameReward(score)
        val gemBonus = if (score >= 35) 1 else 0
        val current = ensureDayState(state)
        commit(
            current.copy(
                coins = GameRules.safeCoins(current.coins.toLong() + reward),
                gems = GameRules.safeGems(current.gems.toLong() + gemBonus),
                counters = current.counters.copy(
                    miniGames = current.counters.miniGames + 1,
                    totalCoinsEarned = GameRules.safeCoins(current.counters.totalCoinsEarned.toLong() + reward)
                ),
                daily = current.daily.copy(miniGames = current.daily.miniGames + 1)
            )
        )
        return if (gemBonus > 0) "$score点！ $rewardコイン + 1ジェム獲得" else "$score点！ $rewardコイン獲得"
    }

    fun exportSave(): String = encode(normalize(state))

    fun importSave(json: String): String {
        val parsed = runCatching { decode(json.trim()) }.getOrElse { return "セーブデータを読み込めませんでした" }
        if (parsed.schemaVersion > GameState().schemaVersion) return "このセーブデータは新しいバージョン用です"
        state = normalize(ensureDayState(parsed)).copy(lastUpdatedAt = System.currentTimeMillis())
        persist()
        return "セーブデータを復元しました"
    }

    fun reset(): String {
        state = ensureDayState(GameState()).copy(lastUpdatedAt = System.currentTimeMillis())
        persist()
        return "ゲームデータを初期化しました"
    }

    fun diagnostics(): List<Pair<String, Boolean>> {
        val s = state
        val activeExists = s.pets.any { it.id == s.activePetId }
        val statsValid = s.pets.all { pet ->
            listOf(pet.hunger, pet.happiness, pet.cleanliness, pet.energy, pet.bond).all { it in 0f..100f }
        }
        val economyValid = s.coins in 0..GameRules.MAX_COINS && s.gems in 0..GameRules.MAX_GEMS
        val inventoryValid = s.inventory.values.all { it in 0..GameRules.MAX_ITEM_COUNT }
        val knownPets = s.pets.all { pet -> SpeciesCatalog.all.any { it.id == pet.speciesId } }
        val idsUnique = s.pets.map { it.id }.distinct().size == s.pets.size
        return listOf(
            "アクティブペット整合性" to activeExists,
            "ステータス範囲 0〜100" to statsValid,
            "通貨の範囲・負数防止" to economyValid,
            "インベントリ数量" to inventoryValid,
            "ペット種データ" to knownPets,
            "ペットID重複防止" to idsUnique,
            "セーブスキーマ v${s.schemaVersion}" to (s.schemaVersion == 1)
        )
    }

    private fun addReward(source: GameState, reward: Reward): GameState = source.copy(
        coins = GameRules.safeCoins(source.coins.toLong() + reward.coins),
        gems = GameRules.safeGems(source.gems.toLong() + reward.gems),
        counters = source.counters.copy(
            totalCoinsEarned = GameRules.safeCoins(source.counters.totalCoinsEarned.toLong() + reward.coins)
        )
    )

    private fun ownsUnique(source: GameState, item: ShopItem): Boolean = when (item.type) {
        ItemType.PET -> item.payload?.let { species -> source.pets.any { it.speciesId == species } } == true
        else -> (source.inventory[item.id] ?: 0) > 0
    }

    private fun updateActive(source: GameState, transform: (PetState) -> PetState): GameState {
        val id = source.activePetId
        return source.copy(pets = source.pets.map { if (it.id == id) transform(it) else it })
    }

    private fun gainXp(source: PetState, amount: Int): PetState {
        var pet = source
        var xp = pet.xp + amount.coerceAtLeast(0)
        var level = pet.level
        while (level < 99 && xp >= GameRules.nextLevelXp(level)) {
            xp -= GameRules.nextLevelXp(level)
            level += 1
        }
        return pet.copy(level = level, xp = xp)
    }

    private fun rewardLevelUpsIfNeeded(before: GameState, after: GameState): GameState {
        val oldLevel = before.pets.firstOrNull { it.id == before.activePetId }?.level ?: 1
        val newLevel = after.pets.firstOrNull { it.id == after.activePetId }?.level ?: oldLevel
        val gained = (newLevel - oldLevel).coerceAtLeast(0)
        if (gained == 0) return after
        return after.copy(
            coins = GameRules.safeCoins(after.coins.toLong() + gained * 120L),
            gems = GameRules.safeGems(after.gems.toLong() + gained),
            counters = after.counters.copy(
                totalCoinsEarned = GameRules.safeCoins(after.counters.totalCoinsEarned.toLong() + gained * 120L)
            )
        )
    }

    private fun ensureDayState(source: GameState): GameState {
        val day = today()
        if (source.daily.day == day) return source
        return source.copy(
            daily = DailyCounters(day = day),
            claimedDailyQuests = emptySet()
        )
    }

    private fun applyElapsed(source: GameState, now: Long): GameState {
        if (source.pets.isEmpty()) return source
        val maxMillis = GameRules.MAX_OFFLINE_HOURS * 60L * 60L * 1000L
        val elapsed = (now - source.lastUpdatedAt).coerceIn(0L, maxMillis)
        val minutes = elapsed / 60_000f
        if (minutes <= 0f) return source

        val pets = source.pets.map { pet ->
            if (pet.sleeping) {
                pet.copy(
                    hunger = GameRules.clampStat(pet.hunger - 0.075f * minutes),
                    cleanliness = GameRules.clampStat(pet.cleanliness - 0.035f * minutes),
                    happiness = GameRules.clampStat(pet.happiness - 0.018f * minutes),
                    energy = GameRules.clampStat(pet.energy + 0.42f * minutes)
                )
            } else {
                val hunger = GameRules.clampStat(pet.hunger - 0.11f * minutes)
                val unhappyRate = if (hunger < 25f) 0.12f else 0.035f
                pet.copy(
                    hunger = hunger,
                    cleanliness = GameRules.clampStat(pet.cleanliness - 0.052f * minutes),
                    happiness = GameRules.clampStat(pet.happiness - unhappyRate * minutes),
                    energy = GameRules.clampStat(pet.energy - 0.06f * minutes)
                )
            }
        }
        return source.copy(pets = pets)
    }

    private fun normalize(source: GameState): GameState {
        val fallbackPets = if (source.pets.isEmpty()) GameState().pets else source.pets
        val pets = fallbackPets.mapIndexed { index, pet ->
            val species = SpeciesCatalog.find(pet.speciesId)
            pet.copy(
                id = pet.id.ifBlank { "pet-${species.id}-${index}-${System.currentTimeMillis()}" },
                speciesId = species.id,
                name = pet.name.trim().take(16).ifBlank { species.name },
                level = pet.level.coerceIn(1, 99),
                xp = pet.xp.coerceIn(0, 100_000),
                hunger = GameRules.clampStat(pet.hunger),
                happiness = GameRules.clampStat(pet.happiness),
                cleanliness = GameRules.clampStat(pet.cleanliness),
                energy = GameRules.clampStat(pet.energy),
                bond = GameRules.clampStat(pet.bond)
            )
        }.distinctBy { it.id }
        val activeId = source.activePetId.takeIf { id -> pets.any { it.id == id } } ?: pets.first().id
        val inventory = source.inventory.mapNotNull { (id, count) ->
            val safe = GameRules.safeCount(count)
            if (safe <= 0) null else id to safe
        }.toMap()
        return source.copy(
            schemaVersion = 1,
            coins = GameRules.safeCoins(source.coins.toLong()),
            gems = GameRules.safeGems(source.gems.toLong()),
            pets = pets,
            activePetId = activeId,
            inventory = inventory
        )
    }

    private fun commit(newState: GameState, touchTime: Boolean = true) {
        val normalized = normalize(ensureDayState(newState))
        state = if (touchTime) normalized.copy(lastUpdatedAt = System.currentTimeMillis()) else normalized
        persist()
    }

    private fun persist() {
        prefs.edit().putString(KEY_SAVE, encode(state)).apply()
    }

    private fun today(): Long = LocalDate.now().toEpochDay()

    private fun encode(source: GameState): String {
        val root = JSONObject()
        root.put("schemaVersion", source.schemaVersion)
        root.put("coins", source.coins)
        root.put("gems", source.gems)
        root.put("activePetId", source.activePetId)
        root.put("roomThemeId", source.roomThemeId)
        root.put("lastUpdatedAt", source.lastUpdatedAt)
        root.put("lastDailyClaimDay", source.lastDailyClaimDay)
        root.put("dailyStreak", source.dailyStreak)
        root.put("lastPatAt", source.lastPatAt)

        val pets = JSONArray()
        source.pets.forEach { pet ->
            pets.put(JSONObject().apply {
                put("id", pet.id)
                put("speciesId", pet.speciesId)
                put("name", pet.name)
                put("level", pet.level)
                put("xp", pet.xp)
                put("hunger", pet.hunger.toDouble())
                put("happiness", pet.happiness.toDouble())
                put("cleanliness", pet.cleanliness.toDouble())
                put("energy", pet.energy.toDouble())
                put("bond", pet.bond.toDouble())
                put("sleeping", pet.sleeping)
                put("outfitId", pet.outfitId ?: JSONObject.NULL)
                put("bornAt", pet.bornAt)
            })
        }
        root.put("pets", pets)

        val inventory = JSONObject()
        source.inventory.forEach { (id, count) -> inventory.put(id, count) }
        root.put("inventory", inventory)

        root.put("counters", JSONObject().apply {
            put("feeds", source.counters.feeds)
            put("plays", source.counters.plays)
            put("cleans", source.counters.cleans)
            put("pats", source.counters.pats)
            put("purchases", source.counters.purchases)
            put("miniGames", source.counters.miniGames)
            put("totalCoinsEarned", source.counters.totalCoinsEarned)
        })

        root.put("daily", JSONObject().apply {
            put("day", source.daily.day)
            put("feeds", source.daily.feeds)
            put("plays", source.daily.plays)
            put("purchases", source.daily.purchases)
            put("miniGames", source.daily.miniGames)
        })

        root.put("claimedAchievements", JSONArray(source.claimedAchievements.toList()))
        root.put("claimedDailyQuests", JSONArray(source.claimedDailyQuests.toList()))
        return root.toString()
    }

    private fun decode(json: String): GameState {
        val root = JSONObject(json)
        val petsArray = root.optJSONArray("pets") ?: JSONArray()
        val pets = buildList {
            for (i in 0 until petsArray.length()) {
                val p = petsArray.optJSONObject(i) ?: continue
                add(
                    PetState(
                        id = p.optString("id", "pet-$i"),
                        speciesId = p.optString("speciesId", "mochi"),
                        name = p.optString("name", "モチ"),
                        level = p.optInt("level", 1),
                        xp = p.optInt("xp", 0),
                        hunger = p.optDouble("hunger", 82.0).toFloat(),
                        happiness = p.optDouble("happiness", 78.0).toFloat(),
                        cleanliness = p.optDouble("cleanliness", 90.0).toFloat(),
                        energy = p.optDouble("energy", 80.0).toFloat(),
                        bond = p.optDouble("bond", 5.0).toFloat(),
                        sleeping = p.optBoolean("sleeping", false),
                        outfitId = if (p.isNull("outfitId")) null else p.optString("outfitId").takeIf { it.isNotBlank() },
                        bornAt = p.optLong("bornAt", System.currentTimeMillis())
                    )
                )
            }
        }

        val inventoryObj = root.optJSONObject("inventory") ?: JSONObject()
        val inventory = buildMap<String, Int> {
            val keys = inventoryObj.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                put(key, inventoryObj.optInt(key, 0))
            }
        }

        val c = root.optJSONObject("counters") ?: JSONObject()
        val d = root.optJSONObject("daily") ?: JSONObject()

        return GameState(
            schemaVersion = root.optInt("schemaVersion", 1),
            coins = root.optInt("coins", 900),
            gems = root.optInt("gems", 10),
            pets = pets.ifEmpty { GameState().pets },
            activePetId = root.optString("activePetId", pets.firstOrNull()?.id ?: GameState().activePetId),
            inventory = inventory.ifEmpty { GameState().inventory },
            roomThemeId = root.optString("roomThemeId", "theme_default"),
            counters = Counters(
                feeds = c.optInt("feeds", 0),
                plays = c.optInt("plays", 0),
                cleans = c.optInt("cleans", 0),
                pats = c.optInt("pats", 0),
                purchases = c.optInt("purchases", 0),
                miniGames = c.optInt("miniGames", 0),
                totalCoinsEarned = c.optInt("totalCoinsEarned", 0)
            ),
            daily = DailyCounters(
                day = d.optLong("day", -1),
                feeds = d.optInt("feeds", 0),
                plays = d.optInt("plays", 0),
                purchases = d.optInt("purchases", 0),
                miniGames = d.optInt("miniGames", 0)
            ),
            lastUpdatedAt = root.optLong("lastUpdatedAt", System.currentTimeMillis()),
            lastDailyClaimDay = root.optLong("lastDailyClaimDay", -1),
            dailyStreak = root.optInt("dailyStreak", 0),
            claimedAchievements = jsonArrayToSet(root.optJSONArray("claimedAchievements")),
            claimedDailyQuests = jsonArrayToSet(root.optJSONArray("claimedDailyQuests")),
            lastPatAt = root.optLong("lastPatAt", 0L)
        )
    }

    private fun jsonArrayToSet(array: JSONArray?): Set<String> {
        if (array == null) return emptySet()
        return buildSet {
            for (i in 0 until array.length()) {
                array.optString(i).takeIf { it.isNotBlank() }?.let(::add)
            }
        }
    }

    companion object {
        private const val KEY_SAVE = "game_state_json"
    }
}
