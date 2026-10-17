package uz.namozvaqti.core

/**
 * O'zbek lotin yozuvini kirill yozuviga o'giradi (ilovadagi hamma matn bitta lotin manbadan olinadi).
 * Qoidalar: o'→ў, g'→ғ, sh→ш, ch→ч, ng→нг, h→ҳ, q→қ, x→х, y+a/o/u/e→я/ё/ю/е,
 * so'z boshida yoki unlidan keyin e→э, tutuq belgisi (')→ъ.
 * Qisqartmalar (GPS, APK), bir-biriga yopishgan nuqtali nomlar (islom.uz) va chet el nomlari (GitHub) o'zgarmaydi.
 */
object UzbekScript {
    private val protectedWords = setOf(
        "GitHub", "Google", "Speech", "Services", "Aladhan", "Muslim", "World", "League", "Wi", "Fi", "Releases",
        "Android", "Wi-Fi", "Hanafiy_",
    )
    private val vowels = "aeiouoʻ"
    private val cache = HashMap<String, String>()

    /**
     * Tanlangan yozuvda ko'rsatish. Manba lotin ham, kirill ham bo'lishi mumkin (suralar matni rasmiy kirillda).
     * [cyrillic] = true: lotin harflar kirillga o'giriladi; false: kirill harflar lotinga o'giriladi.
     */
    fun display(text: String, cyrillic: Boolean): String {
        if (text.isEmpty()) return text
        val key = if (cyrillic) "c$text" else "l$text"
        return cache.getOrPut(key) {
            if (cyrillic) latinToCyrillic(text) else if (text.any { isCyrillic(it) }) cyrillicToLatin(text) else text
        }.also { if (cache.size > 3000) cache.clear() }
    }

    private fun isCyrillic(c: Char) = c in '\u0400'..'\u04FF'

    /** Kirill (o'zbek) -> lotin. Rasmiy kirill matnlardan (muslim.uz) lotin ko'rinish hosil qilish uchun. */
    fun cyrillicToLatin(text: String): String {
        val sb = StringBuilder(text.length + 16)
        val lowerVowels = "аеёиоуўэюяы"
        var i = 0
        while (i < text.length) {
            val ch = text[i]
            if (!isCyrillic(ch)) { sb.append(ch); i++; continue }
            val lo = ch.lowercaseChar()
            val up = ch.isUpperCase()
            val prev = if (i > 0) text[i - 1] else ' '
            val next = if (i + 1 < text.length) text[i + 1] else ' '
            val wordUpper = up && (next.isUpperCase() || (prev.isUpperCase() && isCyrillic(prev)))
            fun out(s: String) {
                sb.append(if (wordUpper) s.uppercase() else if (up) s.replaceFirstChar { it.uppercaseChar() } else s)
            }
            when (lo) {
                'а' -> out("a"); 'б' -> out("b"); 'в' -> out("v"); 'г' -> out("g"); 'д' -> out("d")
                'е' -> {
                    val initial = !isCyrillic(prev) && !prev.isLetter() || prev.lowercaseChar() in lowerVowels || prev == 'ъ' || prev == 'ь'
                    out(if (initial) "ye" else "e")
                }
                'ё' -> out("yo"); 'ж' -> out("j"); 'з' -> out("z"); 'и' -> out("i"); 'й' -> out("y")
                'к' -> out("k"); 'л' -> out("l"); 'м' -> out("m"); 'н' -> out("n"); 'о' -> out("o"); 'п' -> out("p")
                'р' -> out("r"); 'с' -> out("s"); 'т' -> out("t"); 'у' -> out("u"); 'ф' -> out("f"); 'х' -> out("x")
                'ц' -> out("ts"); 'ч' -> out("ch"); 'ш' -> out("sh"); 'э' -> out("e"); 'ю' -> out("yu"); 'я' -> out("ya")
                'ў' -> out("o'"); 'ғ' -> out("g'"); 'қ' -> out("q"); 'ҳ' -> out("h"); 'ъ' -> sb.append("'")
                'ь' -> {}; 'ы' -> out("i")
                else -> sb.append(ch)
            }
            i++
        }
        return sb.toString()
    }

    fun latinToCyrillic(text: String): String {
        val sb = StringBuilder(text.length)
        var i = 0
        while (i < text.length) {
            val c = text[i]
            if (!isLatinLetter(c) && !isApostrophe(c)) {
                sb.append(c); i++; continue
            }
            // chunk: harf, tutuq va ichki nuqta/defis/pastki chiziq
            var j = i
            while (j < text.length && (isLatinLetter(text[j]) || isApostrophe(text[j]) || (text[j].isDigit() && j > i) ||
                    ((text[j] == '.' || text[j] == '-' || text[j] == '_' || text[j] == '@' || text[j] == '/' || text[j] == ':') &&
                        j + 1 < text.length && (isLatinLetter(text[j + 1]) || text[j + 1].isDigit()) && j > i))
            ) j++
            val chunk = text.substring(i, j)
            sb.append(convertChunk(chunk))
            i = j
        }
        return sb.toString()
    }

    private fun convertChunk(chunk: String): String {
        // Ichki '.', '/', ':', '@' bo'lsa (havola, domen) — o'zgarmaydi
        if (chunk.any { it == '.' || it == '/' || it == ':' || it == '@' || it == '_' }) return chunk
        // defis bilan bog'langan so'zlarni alohida o'giramiz
        if (chunk.contains('-')) return chunk.split('-').joinToString("-") { convertWord(it) }
        return convertWord(chunk)
    }

    private fun isKept(word: String): Boolean {
        if (word.isEmpty()) return true
        if (word in protectedWords) return true
        val letters = word.filter { isLatinLetter(it) }
        // QISQARTMA: GPS, APK, ISNA, MWL...
        if (letters.length >= 2 && letters.all { it.isUpperCase() }) return true
        // camelCase (GitHub, iPhone)
        return letters.drop(1).any { it.isUpperCase() }
    }

    private fun convertWord(word: String): String {
        if (word.isEmpty()) return word
        // "GitHub'dan", "Fi'da": asos o'zgarmaydi, qo'shimcha o'giriladi
        val apos = word.indexOfFirst { isApostrophe(it) }
        if (apos > 0 && isKept(word.substring(0, apos))) {
            return word.substring(0, apos) + "'" + convertWord(word.substring(apos + 1))
        }
        if (isKept(word)) return word

        val w = word.map { if (isApostrophe(it)) '\'' else it }.joinToString("")
        val sb = StringBuilder()
        var i = 0
        fun cap(s: String, upper: Boolean) = if (upper && s.isNotEmpty()) s.replaceFirstChar { it.uppercaseChar() } else s
        while (i < w.length) {
            val ch = w[i]
            val lo = ch.lowercaseChar()
            val up = ch.isUpperCase()
            val next = if (i + 1 < w.length) w[i + 1].lowercaseChar() else '\u0000'
            val prevLo = if (i > 0) w[i - 1].lowercaseChar() else '\u0000'
            when {
                lo == 'o' && next == '\'' -> { sb.append(cap("ў", up)); i += 2 }
                lo == 'g' && next == '\'' -> { sb.append(cap("ғ", up)); i += 2 }
                lo == 's' && next == 'h' -> { sb.append(cap("ш", up)); i += 2 }
                lo == 'c' && next == 'h' -> { sb.append(cap("ч", up)); i += 2 }
                lo == 'n' && next == 'g' -> { sb.append(cap("нг", up)); i += 2 }
                lo == 'y' && next == 'o' -> { sb.append(cap("ё", up)); i += 2 }
                lo == 'y' && next == 'u' -> { sb.append(cap("ю", up)); i += 2 }
                lo == 'y' && next == 'a' -> { sb.append(cap("я", up)); i += 2 }
                lo == 'y' && next == 'e' -> { sb.append(cap("е", up)); i += 2 }
                lo == 'e' -> {
                    val initial = i == 0 || prevLo in "aeiou" || prevLo == 'ў'
                    sb.append(cap(if (initial) "э" else "е", up)); i++
                }
                ch == '\'' -> { sb.append("ъ"); i++ }
                else -> { sb.append(cap(single(lo), up)); i++ }
            }
        }
        return sb.toString()
    }

    private fun single(c: Char): String = when (c) {
        'a' -> "а"; 'b' -> "б"; 'd' -> "д"; 'f' -> "ф"; 'g' -> "г"; 'h' -> "ҳ"; 'i' -> "и"; 'j' -> "ж"
        'k' -> "к"; 'l' -> "л"; 'm' -> "м"; 'n' -> "н"; 'o' -> "о"; 'p' -> "п"; 'q' -> "қ"; 'r' -> "р"
        's' -> "с"; 't' -> "т"; 'u' -> "у"; 'v' -> "в"; 'x' -> "х"; 'y' -> "й"; 'z' -> "з"; 'c' -> "ц"
        'w' -> "в"
        else -> c.toString()
    }

    private fun isLatinLetter(c: Char) = c in 'a'..'z' || c in 'A'..'Z'
    private fun isApostrophe(c: Char) = c == '\'' || c == 'ʻ' || c == 'ʼ' || c == '’' || c == '‘' || c == '`'
}
