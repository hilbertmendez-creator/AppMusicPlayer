package com.mimusica.app.data

import java.io.File

/** Stores audio as `<dir>/<songId>.<extension>` (m4a, webm, ...) inside app-private storage. */
class FileAudioStorage(private val dir: File) : AudioStorage {
    init {
        dir.mkdirs()
    }

    override fun fileFor(songId: String, extension: String): File = File(dir, "$songId.$extension")

    override fun find(songId: String): File? = filesOf(songId)
        .firstOrNull { it.extension != AudioStorage.PART_EXTENSION && it.length() > 0 }

    override fun exists(songId: String): Boolean = find(songId) != null

    override fun delete(songId: String) {
        filesOf(songId).forEach { it.delete() }
    }

    private fun filesOf(songId: String): List<File> =
        dir.listFiles { file -> file.name.startsWith("$songId.") }?.toList().orEmpty()
}
