package com.rstlab.pocketpetultra.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GameRulesTest {
    @Test
    fun statClampNeverLeavesRange() {
        assertEquals(0f, GameRules.clampStat(-999f))
        assertEquals(100f, GameRules.clampStat(999f))
        assertEquals(42f, GameRules.clampStat(42f))
    }

    @Test
    fun economyRejectsNegativePricesAndOverspend() {
        assertFalse(GameRules.canSpend(100, 5, -1, 0))
        assertFalse(GameRules.canSpend(100, 5, 101, 0))
        assertFalse(GameRules.canSpend(100, 5, 100, 6))
        assertTrue(GameRules.canSpend(100, 5, 100, 5))
    }

    @Test
    fun miniGameRewardHasHardCap() {
        assertEquals(0, GameRules.miniGameReward(-20))
        assertEquals(140, GameRules.miniGameReward(10))
        assertEquals(650, GameRules.miniGameReward(999))
    }

    @Test
    fun currencySanitizersPreventOverflowAndNegativeValues() {
        assertEquals(0, GameRules.safeCoins(-1L))
        assertEquals(GameRules.MAX_COINS, GameRules.safeCoins(Long.MAX_VALUE))
        assertEquals(0, GameRules.safeGems(-5L))
        assertEquals(GameRules.MAX_GEMS, GameRules.safeGems(Long.MAX_VALUE))
    }
}
