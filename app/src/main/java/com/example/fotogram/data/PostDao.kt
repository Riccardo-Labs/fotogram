package com.example.fotogram.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.fotogram.model.PostDetail

@Dao
interface PostDao {
    // COMANDO 1: Prendi tutti i post
    @Query("SELECT * FROM posts")
    suspend fun getAll(): List<PostDetail>

    // COMANDO 2: Salva una lista di post
    // OnConflictStrategy.REPLACE significa: "Se esiste già il post 76, sovrascrivilo con i dati nuovi"
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(posts: List<PostDetail>)

    // COMANDO 3: Pulisci tutto (utile quando fai logout o ricarichi da zero)
    @Query("DELETE FROM posts")
    suspend fun deleteAll()
}