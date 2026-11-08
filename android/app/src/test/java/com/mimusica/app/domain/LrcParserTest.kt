package com.mimusica.app.domain

import com.mimusica.app.domain.lyrics.LrcParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LrcParserTest {

    @Test
    fun `parses minutes seconds and hundredths`() {
        val result = LrcParser.parse("[00:12.34]Hello world")
        assertEquals(1, result.lines.size)
        assertEquals(12_340L, result.lines[0].timeMs)
        assertEquals("Hello world", result.lines[0].text)
    }

    @Test
    fun `parses three digit milliseconds`() {
        val result = LrcParser.parse("[01:02.345]Line")
        assertEquals(62_345L, result.lines[0].timeMs)
    }

    @Test
    fun `parses timestamps without fraction`() {
        val result = LrcParser.parse("[02:05]No fraction")
        assertEquals(125_000L, result.lines[0].timeMs)
    }

    @Test
    fun `parses single digit fraction as tenths`() {
        val result = LrcParser.parse("[00:01.5]Tenths")
        assertEquals(1_500L, result.lines[0].timeMs)
    }

    @Test
    fun `expands multiple timestamps on one line`() {
        val result = LrcParser.parse("[00:10.00][00:30.50]Chorus")
        assertEquals(listOf(10_000L, 30_500L), result.lines.map { it.timeMs })
        assertTrue(result.lines.all { it.text == "Chorus" })
    }

    @Test
    fun `sorts lines by time`() {
        val result = LrcParser.parse("[00:20.00]Second\n[00:05.00]First\n[00:40.00]Third")
        assertEquals(listOf("First", "Second", "Third"), result.lines.map { it.text })
    }

    @Test
    fun `ignores metadata tags`() {
        val lrc = "[ar:Queen]\n[ti:Bohemian Rhapsody]\n[al:A Night at the Opera]\n[by:someone]\n[00:01.00]Is this the real life?"
        val result = LrcParser.parse(lrc)
        assertEquals(1, result.lines.size)
        assertEquals("Is this the real life?", result.lines[0].text)
    }

    @Test
    fun `reads positive offset tag in milliseconds`() {
        val result = LrcParser.parse("[offset:+500]\n[00:01.00]Hi")
        assertEquals(500L, result.offsetMs)
    }

    @Test
    fun `reads negative offset tag`() {
        val result = LrcParser.parse("[offset:-250]\n[00:01.00]Hi")
        assertEquals(-250L, result.offsetMs)
    }

    @Test
    fun `offset defaults to zero`() {
        assertEquals(0L, LrcParser.parse("[00:01.00]Hi").offsetMs)
    }

    @Test
    fun `tolerates blank and garbage lines`() {
        val lrc = "\n   \nthis is garbage\n[00:03.00]Real line\n[xx:yy]nope\n[99]also nope\n"
        val result = LrcParser.parse(lrc)
        assertEquals(1, result.lines.size)
        assertEquals("Real line", result.lines[0].text)
    }

    @Test
    fun `keeps timestamped empty lines as instrumental gaps`() {
        val result = LrcParser.parse("[00:01.00]Sing\n[00:05.00]\n[00:09.00]Again")
        assertEquals(listOf("Sing", "", "Again"), result.lines.map { it.text })
    }

    @Test
    fun `strips enhanced word timestamps`() {
        val result = LrcParser.parse("[00:01.00]<00:01.00>Hel<00:01.50>lo")
        assertEquals("Hello", result.lines[0].text)
    }

    @Test
    fun `handles windows line endings`() {
        val result = LrcParser.parse("[00:01.00]A\r\n[00:02.00]B\r\n")
        assertEquals(listOf("A", "B"), result.lines.map { it.text })
    }

    @Test
    fun `empty input yields no lines`() {
        assertTrue(LrcParser.parse("").lines.isEmpty())
    }
}
