package com.example.fotogram.model

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

// --- RICHIESTE E RISPOSTE ---
@Serializable
data class RegisterRequest(
    val username: String,
    val email: String,
    val password: String
)

@Serializable
data class UserUpdateRequest(
    val username: String,
    val bio: String,
    val dateOfBirth: String? = null
)

@Serializable
data class LoginResponse(
    val sessionId: String,
    val userId: Int
)

// --- UTENTE ---
@Serializable
data class User(
    val id: Int,
    val username: String?,
    val bio: String? = null,
    val dateOfBirth: String? = null,
    val profilePicture: String? = null,
    val isYourFollowing: Boolean? = false,
    val followersCount: Int,
    val followingCount: Int,
    val postsCount: Int
)

@Serializable
data class PostLocation(
    val latitude: Double? = null,
    val longitude: Double? = null
)

/*
// --- REAZIONE AL POST ---
@Serializable
data class Reaction (
    val hearts: Int,
    val likes: Int,
    val dislikes: Int
)


@Serializable
data class Post(
    val post: Int,
)
*/

/*
@Serializable
data class Adv(
    val id: Int,
    val image: String,
    val text: String,
    val isPositiveFeed: Boolean = false
)
*/


// --- POST ---
@Entity(tableName = "posts")
@Serializable
data class PostDetail(
    @PrimaryKey(autoGenerate = false)
    val id: Int,
    val authorId: Int,
    val createdAt: String,
    val contentPicture: String,
    val contentText: String? = null,

    var authorName: String? = null,
    var authorProfilePicture: String? = null,
    var isFollowed: Boolean = false,
    //var hearts: Int? = 0,
    //var likes: Int? = 0,
    //var dislikes: Int? = 0,

    @Embedded(prefix = "loc_")
    val location: PostLocation? = null
)

