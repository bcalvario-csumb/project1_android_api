package com.example.project1.database

/**
 * A read-only projection of a user for the admin list.
 *
 * Deliberately does NOT include the password column. Room maps a @Query onto whatever
 * class you ask for, so selecting only the columns an admin list needs keeps password
 * hashes out of the UI layer entirely rather than relying on the screen not to show them.
 *
 * @param cardCount how many cards the user owns, computed by the join in
 *   [UserDAO.getAllUsersWithCardCount] rather than by a query per user.
 */
data class UserSummary(
    val id: Int,
    val email: String,
    val name: String,
    val cardCount: Int,
)
