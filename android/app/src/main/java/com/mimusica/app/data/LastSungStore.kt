package com.mimusica.app.data

import kotlinx.coroutines.flow.Flow

/** Remembers the last song that was opened in the karaoke, so Home can offer "Seguir cantando". */
interface LastSungStore {
    val lastSungSongId: Flow<String?>
    suspend fun setLastSungSongId(id: String)
}
