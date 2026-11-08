package io.github.alouafi.ayntranslate.dictionary

/** Kana to Hepburn romaji. Long vowels are doubled (ラーメン → raamen) rather than written with macrons. */
object Romaji {

    fun fromKana(text: String): String {
        val kana = toHiragana(text)
        val out = StringBuilder()
        var geminate = false
        var i = 0
        while (i < kana.length) {
            val c = kana[i]
            val digraph = if (i + 1 < kana.length) DIGRAPHS[kana.substring(i, i + 2)] else null
            val syllable = digraph ?: SYLLABLES[c]
            when {
                c == 'っ' -> Unit // Doubles the consonant of the next syllable.
                c == 'ー' -> out.lastOrNull { it in VOWELS }?.let(out::append)
                c == 'ん' -> out.append(if (startsWithVowelOrY(kana.getOrNull(i + 1))) "n'" else "n")
                syllable == null -> out.append(c)
                else -> {
                    if (geminate && syllable[0] !in VOWELS) out.append(if (syllable.startsWith("ch")) 't' else syllable[0])
                    out.append(syllable)
                }
            }
            geminate = c == 'っ'
            i += if (digraph != null) 2 else 1
        }
        return out.toString()
    }

    private fun toHiragana(text: String): String = buildString(text.length) {
        for (c in text) append(if (c in 'ァ'..'ヶ') c - KATAKANA_OFFSET else c)
    }

    private fun startsWithVowelOrY(next: Char?): Boolean =
        next?.let { SYLLABLES[it] }?.let { it[0] in VOWELS || it[0] == 'y' } == true

    private const val KATAKANA_OFFSET = 0x60
    private const val VOWELS = "aiueo"

    private val SYLLABLES: Map<Char, String> = run {
        val rows = listOf(
            "あいうえお" to listOf("a", "i", "u", "e", "o"),
            "かきくけこ" to listOf("ka", "ki", "ku", "ke", "ko"),
            "がぎぐげご" to listOf("ga", "gi", "gu", "ge", "go"),
            "さしすせそ" to listOf("sa", "shi", "su", "se", "so"),
            "ざじずぜぞ" to listOf("za", "ji", "zu", "ze", "zo"),
            "たちつてと" to listOf("ta", "chi", "tsu", "te", "to"),
            "だぢづでど" to listOf("da", "ji", "zu", "de", "do"),
            "なにぬねの" to listOf("na", "ni", "nu", "ne", "no"),
            "はひふへほ" to listOf("ha", "hi", "fu", "he", "ho"),
            "ばびぶべぼ" to listOf("ba", "bi", "bu", "be", "bo"),
            "ぱぴぷぺぽ" to listOf("pa", "pi", "pu", "pe", "po"),
            "まみむめも" to listOf("ma", "mi", "mu", "me", "mo"),
            "やゆよ" to listOf("ya", "yu", "yo"),
            "らりるれろ" to listOf("ra", "ri", "ru", "re", "ro"),
            "わゐゑを" to listOf("wa", "i", "e", "o"),
            "ぁぃぅぇぉ" to listOf("a", "i", "u", "e", "o"),
            "ゃゅょゎ" to listOf("ya", "yu", "yo", "wa"),
            "ゔ" to listOf("vu"),
        )
        rows.flatMap { (kana, romaji) -> kana.toList().zip(romaji) }.toMap()
    }

    private val DIGRAPHS: Map<String, String> = buildMap {
        // き + ゃ → kya, し + ゃ → sha, じ + ゃ → ja...
        val iRow = mapOf(
            'き' to "ky", 'ぎ' to "gy", 'し' to "sh", 'じ' to "j", 'ち' to "ch", 'ぢ' to "j", 'に' to "ny",
            'ひ' to "hy", 'び' to "by", 'ぴ' to "py", 'み' to "my", 'り' to "ry",
        )
        for ((kana, prefix) in iRow) {
            put("${kana}ゃ", "${prefix}a")
            put("${kana}ゅ", "${prefix}u")
            put("${kana}ょ", "${prefix}o")
        }
        // Combinations used in loanwords.
        for ((small, vowel) in listOf('ぁ' to "a", 'ぃ' to "i", 'ぇ' to "e", 'ぉ' to "o")) {
            put("ふ$small", "f$vowel")
            put("ゔ$small", "v$vowel")
            put("つ$small", "ts$vowel")
        }
        put("うぃ", "wi")
        put("うぇ", "we")
        put("うぉ", "wo")
        put("いぇ", "ye")
        put("しぇ", "she")
        put("じぇ", "je")
        put("ちぇ", "che")
        put("てぃ", "ti")
        put("でぃ", "di")
        put("とぅ", "tu")
        put("どぅ", "du")
    }
}
