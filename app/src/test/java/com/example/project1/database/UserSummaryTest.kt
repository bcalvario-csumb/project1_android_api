package com.example.project1.database

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/**
 * [UserSummary] is a plain data class projected by [UserDAO.getAllUsersWithCardCount]
 * for the admin panel. These tests lock down equality/copy semantics and that
 * `cardCount` -- the whole reason the projection exists instead of reusing `User` --
 * actually carries the value it is given.
 */
class UserSummaryTest {

    @Test
    fun equality_isBasedOnAllProperties() {
        val a = UserSummary(id = 1, email = "a@b.com", name = "Alice", cardCount = 5)
        val b = UserSummary(id = 1, email = "a@b.com", name = "Alice", cardCount = 5)
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun equality_differsWhenCardCountDiffers() {
        val a = UserSummary(id = 1, email = "a@b.com", name = "Alice", cardCount = 5)
        val b = UserSummary(id = 1, email = "a@b.com", name = "Alice", cardCount = 6)
        assertNotEquals(a, b)
    }

    @Test
    fun copy_canOverrideCardCountAlone() {
        val original = UserSummary(id = 1, email = "a@b.com", name = "Alice", cardCount = 0)
        val updated = original.copy(cardCount = 7)

        assertEquals(7, updated.cardCount)
        assertEquals(original.id, updated.id)
        assertEquals(original.email, updated.email)
        assertEquals(original.name, updated.name)
    }

    @Test
    fun cardCount_carriesTheValueItWasGiven() {
        val zeroCards = UserSummary(id = 2, email = "empty@b.com", name = "Empty", cardCount = 0)
        val manyCards = UserSummary(id = 3, email = "full@b.com", name = "Full", cardCount = 42)

        assertEquals(0, zeroCards.cardCount)
        assertEquals(42, manyCards.cardCount)
    }
}
