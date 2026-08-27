package com.rstlab.pocketpetultra.game

import kotlin.math.max
import kotlin.math.min

enum class ItemType {
    FOOD, TOY, OUTFIT, FURNITURE, THEME, PET
}

data class ShopItem(
    val id: String,
    val title: String,
    val description: String,
    val type: ItemType,
    val coinPrice: Int = 0,
    val gemPrice: Int = 0,
    val hungerGain: Float = 0f,
    val happinessGain: Float = 0f,
    val energyGain: Float = 0f,
    val cleanlinessGain: Float = 0f,
    val payload: String? = null
) {
    val isUnique: Boolean
        get() = type in setOf(ItemType.OUTFIT, ItemType.FURNITURE, ItemType.THEME, ItemType.PET)
}

data class SpeciesInfo(
    val id: String,
    val name: String,
    val tagline: String
)

data class PetState(
    val id: String,
    val speciesId: String,
    val name: String,
    val level: Int = 1,
    val xp: Int = 0,
    val hunger: Float = 82f,
    val happiness: Float = 78f,
    val cleanliness: Float = 90f,
    val energy: Float = 80f,
    val bond: Float = 5f,
    val sleeping: Boolean = false,
    val outfitId: String? = null,
    val bornAt: Long = System.currentTimeMillis()
) {
    val stage: Int
        get() = when {
            level >= 25 -> 3
            level >= 10 -> 2
            else -> 1
        }
}

data class Counters(
    val feeds: Int = 0,
    val plays: Int = 0,
    val cleans: Int = 0,
    val pats: Int = 0,
    val purchases: Int = 0,
    val miniGames: Int = 0,
    val totalCoinsEarned: Int = 0
)

data class DailyCounters(
    val day: Long = -1,
    val feeds: Int = 0,
    val plays: Int = 0,
    val purchases: Int = 0,
    val miniGames: Int = 0
)

data class GameState(
    val schemaVersion: Int = 1,
    val coins: Int = 900,
    val gems: Int = 10,
    val pets: List<PetState> = listOf(
        PetState(
            id = "pet-starter-mochi",
            speciesId = "mochi",
            name = "モチ"
        )
    ),
    val activePetId: String = "pet-starter-mochi",
    val inventory: Map<String, Int> = mapOf(
        "food_berry" to 3,
        "food_cookie" to 1,
        "toy_ball" to 1,
        "theme_default" to 1
    ),
    val roomThemeId: String = "theme_default",
    val counters: Counters = Counters(),
    val daily: DailyCounters = DailyCounters(),
    val lastUpdatedAt: Long = System.currentTimeMillis(),
    val lastDailyClaimDay: Long = -1,
    val dailyStreak: Int = 0,
    val claimedAchievements: Set<String> = emptySet(),
    val claimedDailyQuests: Set<String> = emptySet(),
    val lastPatAt: Long = 0L
) {
    val activePet: PetState
        get() = pets.firstOrNull { it.id == activePetId } ?: pets.first()
}

data class Reward(
    val coins: Int = 0,
    val gems: Int = 0
)

data class Achievement(
    val id: String,
    val title: String,
    val description: String,
    val reward: Reward,
    val isUnlocked: (GameState) -> Boolean
)

data class DailyQuest(
    val id: String,
    val title: String,
    val description: String,
    val reward: Reward,
    val progress: (GameState) -> Int,
    val target: Int
)

object GameRules {
    const val MAX_COINS = 9_999_999
    const val MAX_GEMS = 999_999
    const val MAX_ITEM_COUNT = 999
    const val MAX_OFFLINE_HOURS = 48L

    fun clampStat(value: Float): Float = value.coerceIn(0f, 100f)

    fun nextLevelXp(level: Int): Int = 90 + (level.coerceAtLeast(1) - 1) * 45

    fun canSpend(coins: Int, gems: Int, coinPrice: Int, gemPrice: Int): Boolean =
        coinPrice >= 0 && gemPrice >= 0 && coins >= coinPrice && gems >= gemPrice

    fun safeCoins(value: Long): Int = value.coerceIn(0L, MAX_COINS.toLong()).toInt()

    fun safeGems(value: Long): Int = value.coerceIn(0L, MAX_GEMS.toLong()).toInt()

    fun safeCount(value: Int): Int = value.coerceIn(0, MAX_ITEM_COUNT)

    fun miniGameReward(score: Int): Int = min(650, max(0, score) * 14)
}

object SpeciesCatalog {
    val all = listOf(
        SpeciesInfo("mochi", "モチ", "ふわふわの雲から生まれた、のんびり屋"),
        SpeciesInfo("lumi", "ルミ", "暗い場所でほのかに光る、好奇心旺盛な子"),
        SpeciesInfo("nori", "ノリ", "葉っぱの耳を持つ、食いしん坊な森の子"),
        SpeciesInfo("pico", "ピコ", "小さな電気を操る、俊敏ないたずらっ子"),
        SpeciesInfo("sora", "ソラ", "空色の尾を持つ、かなり珍しい幻獣")
    )

    fun find(id: String): SpeciesInfo = all.firstOrNull { it.id == id } ?: all.first()
}

object Catalog {
    val items = listOf(
        ShopItem(
            id = "food_berry",
            title = "星ベリー",
            description = "空腹 +18 / 幸福 +3",
            type = ItemType.FOOD,
            coinPrice = 45,
            hungerGain = 18f,
            happinessGain = 3f
        ),
        ShopItem(
            id = "food_cookie",
            title = "ハニークッキー",
            description = "空腹 +30 / 幸福 +5",
            type = ItemType.FOOD,
            coinPrice = 85,
            hungerGain = 30f,
            happinessGain = 5f
        ),
        ShopItem(
            id = "food_feast",
            title = "王様プレート",
            description = "空腹 +58 / 幸福 +10 / 元気 +5",
            type = ItemType.FOOD,
            coinPrice = 210,
            hungerGain = 58f,
            happinessGain = 10f,
            energyGain = 5f
        ),
        ShopItem(
            id = "toy_ball",
            title = "バウンドボール",
            description = "遊ぶと幸福 +18、親密度アップ",
            type = ItemType.TOY,
            coinPrice = 120,
            happinessGain = 18f
        ),
        ShopItem(
            id = "toy_wand",
            title = "きらきらワンド",
            description = "遊ぶと幸福 +30、経験値多め",
            type = ItemType.TOY,
            coinPrice = 320,
            happinessGain = 30f
        ),
        ShopItem(
            id = "outfit_ribbon",
            title = "クラシックリボン",
            description = "ペットに装備できる定番アクセサリー",
            type = ItemType.OUTFIT,
            coinPrice = 380
        ),
        ShopItem(
            id = "outfit_hoodie",
            title = "ミニフーディー",
            description = "少しスポーティーな服",
            type = ItemType.OUTFIT,
            coinPrice = 780
        ),
        ShopItem(
            id = "outfit_crown",
            title = "ちいさな王冠",
            description = "レアな王冠。ジェムでも購入可能",
            type = ItemType.OUTFIT,
            gemPrice = 8
        ),
        ShopItem(
            id = "furniture_cushion",
            title = "ふかふかクッション",
            description = "ルーム用家具。所有コレクションに追加",
            type = ItemType.FURNITURE,
            coinPrice = 520
        ),
        ShopItem(
            id = "furniture_lamp",
            title = "月あかりランプ",
            description = "ルーム用家具。夜の雰囲気にぴったり",
            type = ItemType.FURNITURE,
            coinPrice = 760
        ),
        ShopItem(
            id = "furniture_plant",
            title = "ミニグリーン",
            description = "ルーム用家具。癒やしの観葉植物",
            type = ItemType.FURNITURE,
            coinPrice = 880
        ),
        ShopItem(
            id = "theme_sunset",
            title = "夕焼けルーム",
            description = "部屋の背景を夕焼け風に変更",
            type = ItemType.THEME,
            coinPrice = 950,
            payload = "theme_sunset"
        ),
        ShopItem(
            id = "theme_night",
            title = "星空ルーム",
            description = "部屋の背景を静かな夜空に変更",
            type = ItemType.THEME,
            coinPrice = 1_300,
            payload = "theme_night"
        ),
        ShopItem(
            id = "theme_sakura",
            title = "桜ルーム",
            description = "淡い桜色の特別ルーム",
            type = ItemType.THEME,
            coinPrice = 1_800,
            payload = "theme_sakura"
        ),
        ShopItem(
            id = "pet_lumi",
            title = "ルミのたまご",
            description = "新しいペット『ルミ』を迎える",
            type = ItemType.PET,
            coinPrice = 2_600,
            payload = "lumi"
        ),
        ShopItem(
            id = "pet_nori",
            title = "ノリのたまご",
            description = "新しいペット『ノリ』を迎える",
            type = ItemType.PET,
            coinPrice = 3_700,
            payload = "nori"
        ),
        ShopItem(
            id = "pet_pico",
            title = "ピコのたまご",
            description = "新しいペット『ピコ』を迎える",
            type = ItemType.PET,
            coinPrice = 5_200,
            payload = "pico"
        ),
        ShopItem(
            id = "pet_sora",
            title = "ソラのたまご",
            description = "幻獣『ソラ』を迎える",
            type = ItemType.PET,
            coinPrice = 7_500,
            gemPrice = 12,
            payload = "sora"
        )
    )

    val achievements = listOf(
        Achievement(
            id = "level_5",
            title = "はじめての成長",
            description = "どれかのペットをLv.5にする",
            reward = Reward(coins = 180),
            isUnlocked = { state -> state.pets.any { it.level >= 5 } }
        ),
        Achievement(
            id = "bond_25",
            title = "なかよし",
            description = "親密度を25以上にする",
            reward = Reward(gems = 2),
            isUnlocked = { state -> state.pets.any { it.bond >= 25f } }
        ),
        Achievement(
            id = "feed_25",
            title = "おなかいっぱい",
            description = "合計25回ごはんをあげる",
            reward = Reward(coins = 420, gems = 2),
            isUnlocked = { state -> state.counters.feeds >= 25 }
        ),
        Achievement(
            id = "shop_10",
            title = "ショッピング好き",
            description = "ショップで10回購入する",
            reward = Reward(coins = 600),
            isUnlocked = { state -> state.counters.purchases >= 10 }
        ),
        Achievement(
            id = "collector_3",
            title = "にぎやかな家",
            description = "ペットを3匹飼う",
            reward = Reward(gems = 5),
            isUnlocked = { state -> state.pets.size >= 3 }
        )
    )

    val dailyQuests = listOf(
        DailyQuest(
            id = "feed_3",
            title = "今日のごはん係",
            description = "今日3回ごはんをあげる",
            reward = Reward(coins = 150),
            progress = { state -> state.daily.feeds },
            target = 3
        ),
        DailyQuest(
            id = "play_2",
            title = "いっしょに遊ぼう",
            description = "今日2回おもちゃで遊ぶ",
            reward = Reward(coins = 180),
            progress = { state -> state.daily.plays },
            target = 2
        ),
        DailyQuest(
            id = "mini_2",
            title = "ゲームタイム",
            description = "今日ミニゲームを2回遊ぶ",
            reward = Reward(coins = 250, gems = 1),
            progress = { state -> state.daily.miniGames },
            target = 2
        )
    )

    fun findItem(id: String): ShopItem? = items.firstOrNull { it.id == id }
}
