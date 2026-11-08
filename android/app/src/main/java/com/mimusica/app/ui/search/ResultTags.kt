package com.mimusica.app.ui.search

/** True when the title says the song is an instrumental version (no singing), whatever the capitalisation. */
fun isInstrumental(title: String): Boolean = title.contains("instrumental", ignoreCase = true)
