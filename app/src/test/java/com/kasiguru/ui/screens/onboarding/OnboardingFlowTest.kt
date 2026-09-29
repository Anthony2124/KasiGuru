package com.kasiguru.ui.screens.onboarding

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OnboardingFlowTest {

    @Test
    fun `twelve framed steps, numbered in order`() {
        assertEquals(12, OnboardingStep.framedCount)
        assertEquals(0, OnboardingStep.Name.progressIndex)
        assertEquals(11, OnboardingStep.Badges.progressIndex)
        assertEquals(-1, OnboardingStep.Awake.progressIndex)
    }

    @Test
    fun `skip before the first word jumps to it`() {
        OnboardingStep.entries
            .filter { it.skippable && it.ordinal < OnboardingStep.FirstWord.ordinal }
            .forEach { assertEquals(OnboardingStep.FirstWord, it.skipTarget) }
    }

    @Test
    fun `skip is never offered on the first word or its reward`() {
        assertFalse(OnboardingStep.FirstWord.skippable)
        assertFalse(OnboardingStep.FirstWordLearned.skippable)
        assertFalse(OnboardingStep.FirstWord.optional)
    }

    @Test
    fun `skip after the first word finishes`() {
        assertNull(OnboardingStep.Reminders.skipTarget)
        assertNull(OnboardingStep.Avatar.skipTarget)
    }

    @Test
    fun `back skips the wake-up moments`() {
        assertEquals(OnboardingStep.Welcome, OnboardingStep.Name.previous)
        assertEquals(OnboardingStep.Welcome, OnboardingStep.Awake.previous)
        assertEquals(OnboardingStep.Name, OnboardingStep.Greeting.previous)
        assertNull(OnboardingStep.Welcome.previous)
    }

    @Test
    fun `next ends after badges`() {
        assertEquals(OnboardingStep.Asleep, OnboardingStep.Welcome.next)
        assertNull(OnboardingStep.Badges.next)
    }

    @Test
    fun `goal prefers the learner's pick, then the level's suggestion, then the default`() {
        assertEquals(DailyGoal.Intense, resolveDailyGoal(DailyGoal.Intense, KnowledgeLevel.New))
        assertEquals(DailyGoal.Casual, resolveDailyGoal(null, KnowledgeLevel.New))
        assertEquals(DailyGoal.Regular, resolveDailyGoal(null, null))
    }

    @Test
    fun `first word answer is the seeded meaning`() {
        assertEquals("aldew", FirstWord.WORD)
        assertTrue(FirstWord.correctIndex >= 0)
        assertEquals("Day, sun", FirstWord.options[FirstWord.correctIndex])
    }

    @Test
    fun `saved step that no longer exists restarts at welcome`() {
        assertEquals(OnboardingStep.Welcome, OnboardingStep.fromOrdinal(99))
    }
}
