package com.mimusica.app.data.local

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface SongDao {
    @Query("SELECT * FROM songs ORDER BY title COLLATE NOCASE")
    fun observeAll(): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs")
    suspend fun getAll(): List<SongEntity>

    @Query("SELECT * FROM songs WHERE id = :id")
    suspend fun getById(id: String): SongEntity?

    @Upsert
    suspend fun upsert(songs: List<SongEntity>)

    @Query("DELETE FROM songs WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("UPDATE songs SET audioPath = :path WHERE id = :id")
    suspend fun setAudioPath(id: String, path: String?)

    @Upsert
    suspend fun upsertLyrics(lyrics: LyricsEntity)

    @Query("SELECT * FROM lyrics WHERE songId = :songId")
    suspend fun getLyrics(songId: String): LyricsEntity?

    @Query("DELETE FROM lyrics WHERE songId = :songId")
    suspend fun deleteLyrics(songId: String)
}

@Dao
interface PlaylistDao {
    @Query("SELECT * FROM playlists ORDER BY name COLLATE NOCASE")
    fun observeAll(): Flow<List<PlaylistEntity>>

    @Query("SELECT * FROM playlists")
    suspend fun getAll(): List<PlaylistEntity>

    @Query("SELECT * FROM playlists WHERE id = :id")
    suspend fun getById(id: String): PlaylistEntity?

    @Upsert
    suspend fun upsert(playlists: List<PlaylistEntity>)

    @Query("DELETE FROM playlists WHERE id = :id")
    suspend fun deleteById(id: String)
}

@Database(
    entities = [SongEntity::class, LyricsEntity::class, PlaylistEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun songDao(): SongDao
    abstract fun playlistDao(): PlaylistDao
}
