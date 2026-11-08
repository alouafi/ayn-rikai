package io.github.alouafi.aynrikai.dictionary

/**
 * The lines of a UTF-8 text file, kept as bytes and decoded on demand, so a large dictionary costs
 * its file size in memory rather than millions of strings. Empty lines and lines starting with `#` are skipped.
 */
internal class Lines(private val bytes: ByteArray) {

    private val starts: IntArray = lineStarts(bytes)

    val size: Int get() = starts.size

    operator fun get(index: Int): String {
        val start = starts[index]
        return String(bytes, start, lineEnd(bytes, start) - start, Charsets.UTF_8)
    }

    /** The index of the line whose first tab-separated field is [key], or -1. Lines must be sorted by that field. */
    fun find(key: String): Int {
        val keyBytes = key.encodeToByteArray()
        var low = 0
        var high = starts.size - 1
        while (low <= high) {
            val middle = (low + high) ushr 1
            val order = compareFirstField(starts[middle], keyBytes)
            when {
                order < 0 -> low = middle + 1
                order > 0 -> high = middle - 1
                else -> return middle
            }
        }
        return -1
    }

    /** Compares bytes as unsigned values, which orders UTF-8 text by code point. */
    private fun compareFirstField(start: Int, key: ByteArray): Int {
        var i = 0
        while (true) {
            val position = start + i
            val fieldEnded = position == bytes.size || bytes[position] == TAB || bytes[position] == NEWLINE
            if (i == key.size) return if (fieldEnded) 0 else 1
            if (fieldEnded) return -1
            val order = (bytes[position].toInt() and 0xFF) - (key[i].toInt() and 0xFF)
            if (order != 0) return order
            i++
        }
    }
}

private fun lineStarts(bytes: ByteArray): IntArray {
    var count = 0
    forEachLineStart(bytes) { count++ }
    val starts = IntArray(count)
    var i = 0
    forEachLineStart(bytes) { starts[i++] = it }
    return starts
}

private inline fun forEachLineStart(bytes: ByteArray, action: (Int) -> Unit) {
    var start = 0
    while (start < bytes.size) {
        val end = lineEnd(bytes, start)
        if (end > start && bytes[start] != COMMENT) action(start)
        start = end + 1
    }
}

private fun lineEnd(bytes: ByteArray, start: Int): Int {
    var end = start
    while (end < bytes.size && bytes[end] != NEWLINE) end++
    return end
}

private const val TAB = '\t'.code.toByte()
private const val NEWLINE = '\n'.code.toByte()
private const val COMMENT = '#'.code.toByte()
