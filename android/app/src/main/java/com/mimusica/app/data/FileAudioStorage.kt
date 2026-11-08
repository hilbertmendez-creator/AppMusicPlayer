package com.mimusica.app.data

import java.io.File

/** Stores audio as `<dir>/<songId>.m4a` inside app-private storage. */
class FileAudioStorage(private val dir: File) : AudioStorage {
    init {
        dir.mkdirs()
    }

    override fun fileFor(songId: String): File = File(dir, "$songId.m4a")

    override fun exists(songId: String): Boolean = fileFor(songId).let { it.exists() && it.length() > 0 }

    override fun delete(songId: String) {
        fileFor(songId).delete()
        File(dir, "$songId.m4a.part").delete()
    }
}
