package io.github.alouafi.aynrikai.dictionary

/** The conjugation classes of dictionary words, plus the stems a conjugated word passes through. */
enum class Form(val code: String?) {
    ICHIDAN("v1"),
    GODAN("v5"),

    /** 行く: a godan verb with irregular te and past forms (行って, 行った). */
    IKU("v5k-s"),

    /** くださる, いらっしゃる: godan verbs whose stem ends in い (ください). */
    ARU("v5aru"),
    KURU("vk"),
    SURU("vs-i"),

    /** A noun that becomes a verb with する: 勉強. */
    SURU_NOUN("vs"),
    ADJECTIVE("adj-i"),

    /** The masu stem: 食べ in 食べます, 書き in 書きます. Not a word on its own. */
    STEM(null),

    /** The negative stem: 食べ in 食べない, 書か in 書かない. Not a word on its own. */
    IRREALIS(null),

    /** A te form followed by an auxiliary: 食べて in 食べている. */
    TE(null),
    ;

    companion object {
        private val byCode = entries.filter { it.code != null }.associateBy { it.code }

        fun ofCode(code: String): Form? = byCode[code]
    }
}

/** What a conjugation adds to a word, such as "past" for 食べた. */
enum class Inflection {
    POLITE, NEGATIVE, PAST, TE, CONDITIONAL, TARA, TARI, VOLITIONAL, IMPERATIVE,
    POTENTIAL, PASSIVE, POTENTIAL_OR_PASSIVE, CAUSATIVE, CAUSATIVE_PASSIVE,
    WANT, PROGRESSIVE, COMPLETION, IN_ADVANCE, TRY, REQUEST, PERMISSION, MUST, MUST_NOT,
    WITHOUT, WHILE, COMMAND, TOO_MUCH, SEEMS, EASY, HARD, ADVERB, NOUN,
}

/**
 * A dictionary form that a text could come from, with the [inflections] that lead to the text in order.
 * [forms] is null for the text as written, which can be any word.
 */
data class Deinflection(val text: String, val forms: Set<Form>?, val inflections: List<Inflection>) {
    fun accepts(wordForms: Set<Form>): Boolean = forms == null || forms.any { it in wordForms }
}

/**
 * Undoes conjugations, the way rikaikun does: 食べさせられなかった leads to 食べる, through the causative,
 * passive, negative and past. Candidates that are not real words are dropped by the dictionary lookup.
 */
object Deinflector {

    fun deinflect(text: String): List<Deinflection> {
        val results = mutableListOf(Deinflection(text, null, emptyList()))
        val seen = mutableSetOf(text to null as Set<Form>?)
        var next = 0
        while (next < results.size && results.size < MAX_CANDIDATES) {
            val current = results[next++]
            for (length in 0..minOf(current.text.length, longestEnding)) {
                val ending = current.text.substring(current.text.length - length)
                for (rule in rulesByEnding[ending].orEmpty()) {
                    if (!rule.appliesTo(current.forms)) continue
                    val stem = current.text.dropLast(length) + rule.base
                    if (stem.isEmpty() || !seen.add(stem to rule.to)) continue
                    results += Deinflection(stem, rule.to, rule.inflections + current.inflections)
                }
            }
        }
        return results
    }

    private val rulesByEnding = RULES.groupBy { it.inflected }
    private val longestEnding = RULES.maxOf { it.inflected.length }
    private const val MAX_CANDIDATES = 500
}

/**
 * Replaces the ending [inflected] with [base], turning a word of one of the [from] forms into one of the [to] forms.
 * An empty [from] means the ending finishes a word, so the rule applies only to the text as written.
 */
private class Rule(
    val inflected: String,
    val base: String,
    val from: Set<Form>,
    val to: Set<Form>,
    val inflections: List<Inflection>,
) {
    /** The text as written can be any word, but never a bare stem. */
    fun appliesTo(forms: Set<Form>?): Boolean =
        if (forms == null) from.all { it.code != null } else from.any { it in forms }
}

private class GodanRow(val u: String, val a: String, val i: String, val e: String, val o: String, val te: String, val ta: String)

private val GODAN_ROWS = listOf(
    GodanRow("う", "わ", "い", "え", "お", "って", "った"),
    GodanRow("く", "か", "き", "け", "こ", "いて", "いた"),
    GodanRow("ぐ", "が", "ぎ", "げ", "ご", "いで", "いだ"),
    GodanRow("す", "さ", "し", "せ", "そ", "して", "した"),
    GodanRow("つ", "た", "ち", "て", "と", "って", "った"),
    GodanRow("ぬ", "な", "に", "ね", "の", "んで", "んだ"),
    GodanRow("ぶ", "ば", "び", "べ", "ぼ", "んで", "んだ"),
    GodanRow("む", "ま", "み", "め", "も", "んで", "んだ"),
    GodanRow("る", "ら", "り", "れ", "ろ", "って", "った"),
)

private val RULES: List<Rule> = buildList {
    val word = emptySet<Form>()
    val ichidan = setOf(Form.ICHIDAN)
    val godan = setOf(Form.GODAN)
    val adjective = setOf(Form.ADJECTIVE)
    val stem = setOf(Form.STEM)
    val irrealis = setOf(Form.IRREALIS)
    val te = setOf(Form.TE)

    fun rule(inflected: String, base: String, to: Set<Form>, vararg inflections: Inflection, from: Set<Form> = word) {
        add(Rule(inflected, base, from, to, inflections.toList()))
    }

    // The te form itself, which te rules below turn back into a word: 食べて → 食べる.
    rule("て", "て", te, Inflection.TE)
    rule("で", "で", te, Inflection.TE)

    for (row in GODAN_ROWS) {
        rule(row.a, row.u, godan, from = irrealis)
        rule(row.i, row.u, godan, from = stem)
        rule(row.te, row.u, godan, from = te)
        rule(row.ta, row.u, godan, Inflection.PAST)
        rule(row.ta + "ら", row.u, godan, Inflection.TARA)
        rule(row.ta + "り", row.u, godan, Inflection.TARI)
        rule(row.e + "ば", row.u, godan, Inflection.CONDITIONAL)
        rule(row.e, row.u, godan, Inflection.IMPERATIVE)
        rule(row.o + "う", row.u, godan, Inflection.VOLITIONAL)
        rule(row.e + "る", row.u, godan, Inflection.POTENTIAL, from = ichidan)
        rule(row.a + "れる", row.u, godan, Inflection.PASSIVE, from = ichidan)
        rule(row.a + "せる", row.u, godan, Inflection.CAUSATIVE, from = ichidan)
        rule(row.a + "される", row.u, godan, Inflection.CAUSATIVE_PASSIVE, from = ichidan)
    }
    val iku = setOf(Form.IKU)
    rule("って", "く", iku, from = te)
    rule("った", "く", iku, Inflection.PAST)
    rule("ったら", "く", iku, Inflection.TARA)
    rule("ったり", "く", iku, Inflection.TARI)
    rule("い", "る", setOf(Form.ARU), from = stem)
    rule("い", "る", setOf(Form.ARU, Form.KURU), Inflection.IMPERATIVE)
    rule("くれ", "くれる", ichidan, Inflection.IMPERATIVE)

    // Ichidan verbs. 来る written in kanji conjugates the same way on the surface: 来ない, 来た.
    val ichidanOrKuru = setOf(Form.ICHIDAN, Form.KURU)
    rule("", "る", ichidanOrKuru, from = stem + irrealis)
    rule("て", "る", ichidanOrKuru, from = te)
    rule("た", "る", ichidanOrKuru, Inflection.PAST)
    rule("たら", "る", ichidanOrKuru, Inflection.TARA)
    rule("たり", "る", ichidanOrKuru, Inflection.TARI)
    rule("れば", "る", ichidanOrKuru, Inflection.CONDITIONAL)
    rule("ろ", "る", ichidan, Inflection.IMPERATIVE)
    rule("よ", "る", ichidan, Inflection.IMPERATIVE)
    rule("よう", "る", ichidanOrKuru, Inflection.VOLITIONAL)
    rule("られる", "る", ichidanOrKuru, Inflection.POTENTIAL_OR_PASSIVE, from = ichidan)
    rule("れる", "る", ichidanOrKuru, Inflection.POTENTIAL, from = ichidan)
    rule("させる", "る", ichidanOrKuru, Inflection.CAUSATIVE, from = ichidan)

    // する, and nouns that take it: 勉強した → 勉強する → 勉強.
    val suru = setOf(Form.SURU)
    rule("し", "する", suru, from = stem + irrealis)
    rule("せ", "する", suru, from = irrealis)
    rule("して", "する", suru, from = te)
    rule("した", "する", suru, Inflection.PAST)
    rule("したら", "する", suru, Inflection.TARA)
    rule("したり", "する", suru, Inflection.TARI)
    rule("すれば", "する", suru, Inflection.CONDITIONAL)
    rule("しろ", "する", suru, Inflection.IMPERATIVE)
    rule("せよ", "する", suru, Inflection.IMPERATIVE)
    rule("しよう", "する", suru, Inflection.VOLITIONAL)
    rule("される", "する", suru, Inflection.PASSIVE, from = ichidan)
    rule("させる", "する", suru, Inflection.CAUSATIVE, from = ichidan)
    rule("できる", "する", suru, Inflection.POTENTIAL, from = ichidan)
    rule("する", "", setOf(Form.SURU_NOUN), from = suru)

    // 来る written in kana: こない, きます, きて.
    val kuru = setOf(Form.KURU)
    rule("こ", "くる", kuru, from = irrealis)
    rule("き", "くる", kuru, from = stem)
    rule("きて", "くる", kuru, from = te)
    rule("きた", "くる", kuru, Inflection.PAST)
    rule("きたら", "くる", kuru, Inflection.TARA)
    rule("きたり", "くる", kuru, Inflection.TARI)
    rule("くれば", "くる", kuru, Inflection.CONDITIONAL)
    rule("こい", "くる", kuru, Inflection.IMPERATIVE)
    rule("こよう", "くる", kuru, Inflection.VOLITIONAL)
    rule("こられる", "くる", kuru, Inflection.POTENTIAL_OR_PASSIVE, from = ichidan)
    rule("これる", "くる", kuru, Inflection.POTENTIAL, from = ichidan)
    rule("こさせる", "くる", kuru, Inflection.CAUSATIVE, from = ichidan)

    // い adjectives, and the endings that conjugate like them (ない, たい).
    rule("くない", "い", adjective, Inflection.NEGATIVE, from = adjective)
    rule("かった", "い", adjective, Inflection.PAST)
    rule("くて", "い", adjective, from = te)
    rule("ければ", "い", adjective, Inflection.CONDITIONAL)
    rule("かったら", "い", adjective, Inflection.TARA)
    rule("かったり", "い", adjective, Inflection.TARI)
    rule("く", "い", adjective, Inflection.ADVERB)
    rule("さ", "い", adjective, Inflection.NOUN)
    rule("そう", "い", adjective, Inflection.SEEMS)
    rule("なさそう", "ない", adjective, Inflection.SEEMS)
    rule("よさそう", "よい", adjective, Inflection.SEEMS)
    rule("すぎる", "い", adjective, Inflection.TOO_MUCH, from = ichidan)
    rule("くありません", "い", adjective, Inflection.POLITE, Inflection.NEGATIVE)
    rule("くありませんでした", "い", adjective, Inflection.POLITE, Inflection.NEGATIVE, Inflection.PAST)

    // Endings after the negative stem: 食べ・ない, 書か・ない.
    rule("ない", "", irrealis, Inflection.NEGATIVE, from = adjective)
    rule("ないで", "", irrealis, Inflection.NEGATIVE, Inflection.TE)
    rule("ず", "", irrealis, Inflection.NEGATIVE)
    rule("ぬ", "", irrealis, Inflection.NEGATIVE)
    rule("ん", "", irrealis, Inflection.NEGATIVE)
    rule("ずに", "", irrealis, Inflection.WITHOUT)
    for (ending in listOf("ければならない", "ければいけない", "くてはならない", "くてはいけない", "くちゃいけない", "きゃいけない", "きゃならない")) {
        rule("な$ending", "", irrealis, Inflection.MUST, from = adjective)
    }
    rule("なくちゃ", "", irrealis, Inflection.MUST)
    rule("なきゃ", "", irrealis, Inflection.MUST)

    // Endings after the masu stem: 食べ・ます, 書き・ます.
    rule("ます", "", stem, Inflection.POLITE)
    rule("ました", "", stem, Inflection.POLITE, Inflection.PAST)
    rule("ません", "", stem, Inflection.POLITE, Inflection.NEGATIVE)
    rule("ませんでした", "", stem, Inflection.POLITE, Inflection.NEGATIVE, Inflection.PAST)
    rule("ましょう", "", stem, Inflection.POLITE, Inflection.VOLITIONAL)
    rule("まして", "", stem, Inflection.POLITE, Inflection.TE)
    rule("たい", "", stem, Inflection.WANT, from = adjective)
    rule("やすい", "", stem, Inflection.EASY, from = adjective)
    rule("にくい", "", stem, Inflection.HARD, from = adjective)
    rule("すぎる", "", stem, Inflection.TOO_MUCH, from = ichidan)
    rule("ながら", "", stem, Inflection.WHILE)
    rule("なさい", "", stem, Inflection.COMMAND)
    rule("そう", "", stem, Inflection.SEEMS)

    // Auxiliaries after the te form: 食べて・いる, 読んで・しまう. で follows ん, い after ぐ.
    for (t in listOf("て", "で")) {
        rule("${t}いる", t, te, Inflection.PROGRESSIVE, from = ichidan)
        rule("${t}る", t, te, Inflection.PROGRESSIVE, from = ichidan)
        rule("${t}しまう", t, te, Inflection.COMPLETION, from = godan)
        rule("${t}おく", t, te, Inflection.IN_ADVANCE, from = godan)
        rule("${t}みる", t, te, Inflection.TRY, from = ichidan)
        rule("${t}ください", t, te, Inflection.REQUEST)
        rule("${t}くれ", t, te, Inflection.REQUEST)
        rule("${t}もいい", t, te, Inflection.PERMISSION)
        rule("${t}はいけない", t, te, Inflection.MUST_NOT, from = adjective)
    }
    // Spoken contractions: 食べちゃう, 読んじゃう, 書いとく.
    rule("ちゃう", "て", te, Inflection.COMPLETION, from = godan)
    rule("じゃう", "で", te, Inflection.COMPLETION, from = godan)
    rule("とく", "て", te, Inflection.IN_ADVANCE, from = godan)
    rule("どく", "で", te, Inflection.IN_ADVANCE, from = godan)
    rule("ちゃいけない", "て", te, Inflection.MUST_NOT, from = adjective)
    rule("じゃいけない", "で", te, Inflection.MUST_NOT, from = adjective)
}
