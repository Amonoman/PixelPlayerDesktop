package com.kayoi.pixelplayerdesktop.utils

import com.atilika.kuromoji.ipadic.Tokenizer
import com.ibm.icu.text.Transliterator
import net.sourceforge.pinyin4j.PinyinHelper
import net.sourceforge.pinyin4j.format.HanyuPinyinCaseType
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType
import com.kayoi.pixelplayerdesktop.data.model.Lyrics
import java.util.Locale

object MultiLangRomanizer {
    private val kuromojiTokenizer: Tokenizer? by lazy {
        try {
            Thread.currentThread().contextClassLoader = MultiLangRomanizer::class.java.classLoader
            Tokenizer()
        } catch (_: Throwable) {
            null
        }
    }

    private val HANGUL_ROMAJA_MAP = mapOf(
        "cho" to mapOf("ᄀ" to "g", "ᄁ" to "kk", "ᄂ" to "n", "ᄃ" to "d", "ᄄ" to "tt", "ᄅ" to "r", "ᄆ" to "m", "ᄇ" to "b", "ᄈ" to "pp", "ᄉ" to "s", "ᄊ" to "ss", "ᄋ" to "", "ᄌ" to "j", "ᄍ" to "jj", "ᄎ" to "ch", "ᄏ" to "k", "ᄐ" to "t", "ᄑ" to "p", "ᄒ" to "h"),
        "jung" to mapOf("ᅡ" to "a", "ᅢ" to "ae", "ᅣ" to "ya", "ᅤ" to "yae", "ᅥ" to "eo", "ᅦ" to "e", "ᅧ" to "yeo", "ᅨ" to "ye", "ᅩ" to "o", "ᅪ" to "wa", "ᅫ" to "wae", "ᅬ" to "oe", "ᅭ" to "yo", "ᅮ" to "u", "ᅯ" to "wo", "ᅰ" to "we", "ᅱ" to "wi", "ᅲ" to "yu", "ᅳ" to "eu", "ᅴ" to "eui", "ᅵ" to "i"),
        "jong" to mapOf("ᆨ" to "k", "ᆨᄋ" to "g", "ᆨᄂ" to "ngn", "ᆨᄅ" to "ngn", "ᆨᄆ" to "ngm", "ᆨᄒ" to "kh", "ᆩ" to "kk", "ᆩᄋ" to "kg", "ᆩᄂ" to "ngn", "ᆩᄅ" to "ngn", "ᆩᄆ" to "ngm", "ᆩᄒ" to "kh", "ᆪ" to "k", "ᆪᄋ" to "ks", "ᆪᄂ" to "ngn", "ᆪᄅ" to "ngn", "ᆪᄆ" to "ngm", "ᆪᄒ" to "kch", "ᆫ" to "n", "ᆫᄅ" to "ll", "ᆬ" to "n", "ᆬᄋ" to "nj", "ᆬᄂ" to "nn", "ᆬᄅ" to "nn", "ᆬᄆ" to "nm", "ᆬㅎ" to "nch", "ᆭ" to "n", "ᆭᄋ" to "nh", "ᆭᄅ" to "nn", "ᆮ" to "t", "ᆮᄋ" to "d", "ᆮᄂ" to "nn", "ᆮᄅ" to "nn", "ᆮᄆ" to "nm", "ᆮᄒ" to "th", "ᆯ" to "l", "ᆯᄋ" to "r", "ᆯᄂ" to "ll", "ᆯᄅ" to "ll", "ᆰ" to "k", "ᆰᄋ" to "lg", "ᆰᄂ" to "ngn", "ᆰᄅ" to "ngn", "ᆰᄆ" to "ngm", "ᆰᄒ" to "lkh", "ᆱ" to "m", "ᆱᄋ" to "lm", "ᆱᄂ" to "mn", "ᆱᄅ" to "mn", "ᆱᄆ" to "mm", "ᆱᄒ" to "lmh", "ᆲ" to "p", "ᆲᄋ" to "lb", "ᆲᄂ" to "mn", "ᆲᄅ" to "mn", "ᆲᄆ" to "mm", "ᆲᄒ" to "lph", "ᆳ" to "t", "ᆳᄋ" to "ls", "ᆳᄂ" to "nn", "ᆳᄅ" to "nn", "ᆳᄆ" to "nm", "ᆳᄒ" to "lsh", "ᆴ" to "t", "ᆴᄋ" to "lt", "ᆴᄂ" to "nn", "ᆴᄅ" to "nn", "ᆴᄆ" to "nm", "ᆴᄒ" to "lth", "ᆵ" to "p", "ᆵᄋ" to "lp", "ᆵᄂ" to "mn", "ᆵᄅ" to "mn", "ᆵᄆ" to "mm", "ᆵᄒ" to "lph", "ᆶ" to "l", "ᆶᄋ" to "lh", "ᆶᄂ" to "ll", "ᆶᄅ" to "ll", "ᆶᄆ" to "lm", "ᆶᄒ" to "lh", "ᆷ" to "m", "ᆷᄅ" to "mn", "ᆸ" to "p", "ᆸᄋ" to "b", "ᆸᄂ" to "mn", "ᆸᄅ" to "mn", "ᆸᄆ" to "mm", "ᆸᄒ" to "ph", "ᆹ" to "p", "ᆹᄋ" to "ps", "ᆹᄂ" to "mn", "ᆹᄅ" to "mn", "ᆹᄆ" to "mm", "ᆹᄒ" to "psh", "ᆺ" to "t", "ᆺᄋ" to "s", "ᆺᄂ" to "nn", "ᆺᄅ" to "nn", "ᆺᄆ" to "nm", "ᆺᄒ" to "sh", "ᆻ" to "t", "ᆻᄋ" to "ss", "ᆻᄂ" to "tn", "ᆻᄅ" to "tn", "ᆻᄆ" to "nm", "ᆻᄒ" to "th", "ᆼ" to "ng", "ᆽ" to "t", "ᆽᄋ" to "j", "ᆽᄂ" to "nn", "ᆽᄅ" to "nn", "ᆽᄆ" to "nm", "ᆽᄒ" to "ch", "ᆾ" to "t", "ᆾᄋ" to "ch", "ᆾᄂ" to "nn", "ᆾᄅ" to "nn", "ᆾᄆ" to "nm", "ᆾᄒ" to "ch", "ᆿ" to "k", "ᆿᄋ" to "k", "ᆿᄂ" to "ngn", "ᆿᄅ" to "ngn", "ᆿᄆ" to "ngm", "ᆿᄒ" to "kh", "ᇀ" to "t", "ᇀᄋ" to "t", "ᇀᄂ" to "nn", "ᇀᄅ" to "nn", "ᇀᄆ" to "nm", "ᇀᄒ" to "th", "ᇁ" to "p", "ᇁᄋ" to "p", "ᇁᄂ" to "mn", "ᇁᄅ" to "mn", "ᇁᄆ" to "mm", "ᇁᄒ" to "ph", "ᇂ" to "t", "ᇂᄋ" to "h", "ᇂᄂ" to "nn", "ᇂᄅ" to "nn", "ᇂᄆ" to "mm", "ᇂᄒ" to "t", "ᇂᄀ" to "k")
    )

    private val DEVANAGARI_ROMAJI_MAP = mapOf(
        "अ" to "a", "आ" to "aa", "इ" to "i", "ई" to "ee", "उ" to "u", "ऊ" to "oo", "ऋ" to "ri", "ए" to "e", "ऐ" to "ai", "ओ" to "o", "औ" to "au", "क" to "k", "ख" to "kh", "ग" to "g", "घ" to "gh", "ङ" to "ng", "च" to "ch", "छ" to "chh", "ज" to "j", "झ" to "jh", "ञ" to "ny", "ट" to "t", "ठ" to "th", "ड" to "d", "ढ" to "dh", "ण" to "n", "त" to "t", "थ" to "th", "द" to "d", "ध" to "dh", "न" to "n", "प" to "p", "फ" to "ph", "ब" to "b", "भ" to "bh", "म" to "m", "य" to "y", "र" to "r", "ल" to "l", "व" to "v", "श" to "sh", "ष" to "sh", "स" to "s", "ह" to "h", "क्ष" to "ksh", "त्र" to "tr", "ज्ञ" to "gy", "श्र" to "shr", "ा" to "aa", "ि" to "i", "ी" to "ee", "ु" to "u", "ू" to "oo", "ृ" to "ri", "े" to "e", "ै" to "ai", "ो" to "o", "ौ" to "au", "ं" to "n", "ः" to "h", "ँ" to "n", "़" to "", "्" to "", "०" to "0", "१" to "1", "२" to "2", "३" to "3", "४" to "4", "५" to "5", "६" to "6", "७" to "7", "८" to "8", "९" to "9", "ॐ" to "Om", "ऽ" to "", "क़" to "q", "ख़" to "kh", "ग़" to "g", "ज़" to "z", "ड़" to "r", "ढ़" to "rh", "फ़" to "f", "य़" to "y", "क\u093C" to "q", "ख\u093C" to "kh", "ग\u093C" to "g", "ज\u093C" to "z", "ड\u093C" to "r", "ढ\u093C" to "rh", "फ\u093C" to "f", "य\u093C" to "y"
    )

    private val GURMUKHI_ROMAJI_MAP = mapOf(
        "ੳ" to "o", "ਅ" to "a", "ੲ" to "e", "ਸ" to "s", "ਹ" to "h", "ਕ" to "k", "ਖ" to "kh", "ਗ" to "g", "ਘ" to "gh", "ਙ" to "ng", "ਚ" to "ch", "ਛ" to "chh", "ਜ" to "j", "ਝ" to "jh", "ਞ" to "ny", "ਟ" to "t", "ਠ" to "th", "ਡ" to "d", "ਢ" to "dh", "ਣ" to "n", "ਤ" to "t", "ਥ" to "th", "ਦ" to "d", "ਧ" to "dh", "ਨ" to "n", "ਪ" to "p", "ਫ" to "ph", "ਬ" to "b", "ਭ" to "bh", "ਮ" to "m", "ਯ" to "y", "ਰ" to "r", "ਲ" to "l", "ਵ" to "v", "ੜ" to "r", "ਸ਼" to "sh", "ਖ਼" to "kh", "ਗ਼" to "g", "ਜ਼" to "z", "ਫ਼" to "f", "ਲ਼" to "l", "ਾ" to "aa", "ਿ" to "i", "ੀ" to "ee", "ੁ" to "u", "ੂ" to "oo", "ੇ" to "e", "ੈ" to "ai", "ੋ" to "o", "ੌ" to "au", "ੰ" to "n", "ਂ" to "n", "ੱ" to "", "੍" to "", "਼" to "", "ੴ" to "Ek Onkar", "੦" to "0", "੧" to "1", "੨" to "2", "੩" to "3", "੪" to "4", "੫" to "5", "੬" to "6", "੭" to "7", "੮" to "8", "੯" to "9"
    )

    private val GENERAL_CYRILLIC_ROMAJI_MAP = mapOf(
        "А" to "A", "Б" to "B", "В" to "V", "Г" to "G", "Ґ" to "G", "Д" to "D", "Ѓ" to "Ǵ", "Ђ" to "Đ", "Е" to "E", "Ё" to "Yo", "Є" to "Ye", "Ж" to "Zh", "З" to "Z", "Ѕ" to "Dz", "И" to "I", "І" to "I", "Ї" to "Yi", "Й" to "Y", "Ј" to "Y", "К" to "K", "Л" to "L", "Љ" to "Ly", "М" to "M", "Н" to "N", "Њ" to "Ny", "О" to "O", "П" to "P", "Р" to "R", "С" to "S", "Т" to "T", "Ћ" to "Ć", "У" to "U", "Ў" to "Ŭ", "Ф" to "F", "Х" to "Kh", "Ц" to "Ts", "Ч" to "Ch", "Џ" to "Dž", "Ш" to "Sh", "Щ" to "Shch", "Ъ" to "ʺ", "Ы" to "Y", "Ь" to "ʹ", "Э" to "E", "Ю" to "Yu", "Я" to "Ya",
        "а" to "a", "б" to "b", "в" to "v", "г" to "g", "ґ" to "g", "д" to "d", "ѓ" to "ǵ", "ђ" to "đ", "е" to "e", "ё" to "yo", "є" to "ye", "ж" to "zh", "з" to "z", "ѕ" to "dz", "и" to "i", "і" to "i", "ї" to "yi", "й" to "y", "ј" to "y", "к" to "k", "л" to "l", "љ" to "ly", "м" to "m", "н" to "n", "њ" to "ny", "о" to "o", "п" to "p", "р" to "r", "с" to "s", "т" to "t", "ћ" to "ć", "у" to "u", "ў" to "ŭ", "ф" to "f", "х" to "kh", "ц" to "ts", "ч" to "ch", "џ" to "dž", "ш" to "sh", "щ" to "shch", "ъ" to "ʺ", "ы" to "y", "ь" to "ʹ", "э" to "e", "ю" to "yu", "я" to "ya"
    )

    private val RUSSIAN_ROMAJI_MAP = mapOf("ого" to "ovo", "Ого" to "Ovo", "его" to "evo", "Его" to "Evo")
    private val UKRAINIAN_ROMAJI_MAP = mapOf("Г" to "H", "г" to "h", "Ґ" to "G", "ґ" to "g", "Є" to "Ye", "є" to "ye", "І" to "I", "і" to "i", "Ї" to "Yi", "ї" to "yi")
    private val SERBIAN_ROMAJI_MAP = mapOf("Ж" to "Ž", "Љ" to "Lj", "Њ" to "Nj", "Ц" to "C", "Ч" to "Č", "Џ" to "Dž", "Ш" to "Š", "Х" to "H", "ж" to "ž", "љ" to "lj", "њ" to "nj", "ц" to "c", "ч" to "č", "џ" to "dž", "ш" to "š", "х" to "h")
    private val BULGARIAN_ROMAJI_MAP = mapOf("Ж" to "Zh", "Ц" to "Ts", "Ч" to "Ch", "Ш" to "Sh", "Щ" to "Sht", "Ъ" to "A", "Ь" to "Y", "Ю" to "Yu", "Я" to "Ya", "ж" to "zh", "ц" to "ts", "ч" to "ch", "ш" to "sh", "щ" to "sht", "ъ" to "a", "ь" to "y", "ю" to "yu", "я" to "ya")
    private val BELARUSIAN_ROMAJI_MAP = mapOf("Г" to "H", "г" to "h", "Ў" to "W", "ў" to "w")
    private val KYRGYZ_ROMAJI_MAP = mapOf("Ү" to "Ü", "ү" to "ü", "Ы" to "Y", "ы" to "y")
    private val MACEDONIAN_ROMAJI_MAP = mapOf("Ѓ" to "Gj", "Ѕ" to "Dz", "И" to "I", "Ј" to "J", "Љ" to "Lj", "Њ" to "Nj", "Ќ" to "Kj", "Џ" to "Dž", "Ч" to "Č", "Ш" to "Sh", "Ж" to "Zh", "Ц" to "C", "Х" to "H", "ѓ" to "gj", "ѕ" to "dz", "и" to "i", "ј" to "j", "љ" to "lj", "њ" to "nj", "ќ" to "kj", "џ" to "dž", "ч" to "č", "ш" to "sh", "ж" to "zh", "ц" to "c", "х" to "h")

    private val MULTI_SPACE_REGEX = Regex("""\s+""")
    private val JAPANESE_N_ASSIMILATION_REGEX = Regex("n(?=[bp])")
    private val ERHUA_NG_REGEX = Regex("ng$")
    private val ERHUA_N_REGEX = Regex("n$")
    private val CYRILLIC_WORD_SPLIT_REGEX = Regex("((?<=\\s|[.,!?;])|(?=\\s|[.,!?;]))")
    private val CYRILLIC_PUNCTUATION_CHARS = setOf('.', ',', '!', '?', ';')

    private val RUSSIAN_CYRILLIC_LETTERS = setOf('А', 'Б', 'В', 'Г', 'Д', 'Е', 'Ё', 'Ж', 'З', 'И', 'Й', 'К', 'Л', 'М', 'Н', 'О', 'П', 'Р', 'С', 'Т', 'У', 'Ф', 'Х', 'Ц', 'Ч', 'Ш', 'Щ', 'Ъ', 'Ы', 'Ь', 'Э', 'Ю', 'Я', 'а', 'б', 'в', 'г', 'д', 'е', 'ё', 'ж', 'з', 'и', 'й', 'к', 'л', 'м', 'н', 'о', 'п', 'р', 'с', 'т', 'у', 'ф', 'х', 'ц', 'ч', 'ш', 'щ', 'ъ', 'ы', 'ь', 'э', 'ю', 'я')
    private val UKRAINIAN_CYRILLIC_LETTERS = setOf('А', 'Б', 'В', 'Г', 'Ґ', 'Д', 'Е', 'Є', 'Ж', 'З', 'И', 'І', 'Ї', 'Й', 'К', 'Л', 'М', 'Н', 'О', 'П', 'Р', 'С', 'Т', 'У', 'Ф', 'Х', 'Ц', 'Ч', 'Ш', 'Щ', 'Ь', 'Ю', 'Я', 'а', 'б', 'в', 'г', 'ґ', 'д', 'е', 'є', 'ж', 'з', 'и', 'і', 'ї', 'й', 'к', 'л', 'м', 'н', 'о', 'п', 'р', 'с', 'т', 'у', 'ф', 'х', 'ц', 'ч', 'ш', 'щ', 'ь', 'ю', 'я')
    private val SERBIAN_CYRILLIC_LETTERS = setOf('А', 'Б', 'В', 'Г', 'Д', 'Ђ', 'Е', 'Ж', 'З', 'И', 'Ј', 'К', 'Л', 'Љ', 'М', 'Н', 'Њ', 'О', 'П', 'Р', 'С', 'Т', 'Ћ', 'У', 'Ф', 'Х', 'Ц', 'Ч', 'Џ', 'Ш', 'а', 'б', 'в', 'г', 'д', 'ђ', 'е', 'ж', 'з', 'и', 'ј', 'к', 'л', 'љ', 'м', 'н', 'њ', 'о', 'п', 'р', 'с', 'т', 'ћ', 'у', 'ф', 'х', 'ц', 'ч', 'џ', 'ш')
    private val BULGARIAN_CYRILLIC_LETTERS = setOf('А', 'Б', 'В', 'Г', 'Д', 'Е', 'Ж', 'З', 'И', 'Й', 'К', 'Л', 'М', 'Н', 'О', 'П', 'Р', 'С', 'Т', 'У', 'Ф', 'Х', 'Ц', 'Ч', 'Ш', 'Щ', 'Ъ', 'Ь', 'Ю', 'Я', 'а', 'б', 'в', 'г', 'д', 'е', 'ж', 'з', 'и', 'й', 'к', 'л', 'м', 'н', 'о', 'п', 'р', 'с', 'т', 'у', 'ф', 'х', 'ц', 'ч', 'ш', 'щ', 'ъ', 'ь', 'ю', 'я')
    private val BELARUSIAN_CYRILLIC_LETTERS = setOf('А', 'Б', 'В', 'Г', 'Д', 'Е', 'Ё', 'Ж', 'З', 'І', 'Й', 'К', 'Л', 'М', 'Н', 'О', 'П', 'Р', 'С', 'Т', 'У', 'Ў', 'Ф', 'Х', 'Ц', 'Ч', 'Ш', 'Ь', 'Ю', 'Я', 'Ы', 'Э', 'а', 'б', 'в', 'г', 'д', 'е', 'ё', 'ж', 'з', 'і', 'й', 'к', 'л', 'м', 'н', 'о', 'п', 'р', 'с', 'т', 'у', 'ў', 'ф', 'х', 'ц', 'ч', 'ш', 'ь', 'ю', 'я', 'ы', 'э')
    private val KYRGYZ_CYRILLIC_LETTERS = setOf('А', 'Б', 'В', 'Г', 'Д', 'Е', 'Ё', 'Ж', 'З', 'И', 'Й', 'К', 'Л', 'М', 'Н', 'Ң', 'О', 'Ө', 'П', 'Р', 'С', 'Т', 'У', 'Ү', 'Ф', 'Х', 'Ц', 'Ч', 'Ш', 'Щ', 'Ъ', 'Ы', 'Ь', 'Э', 'Ю', 'Я', 'а', 'б', 'в', 'г', 'д', 'е', 'ё', 'ж', 'з', 'и', 'й', 'к', 'л', 'м', 'н', 'ң', 'о', 'ө', 'п', 'р', 'с', 'т', 'у', 'ү', 'ф', 'х', 'ц', 'ч', 'ш', 'щ', 'ъ', 'ы', 'ь', 'э', 'ю', 'я')
    private val MACEDONIAN_CYRILLIC_LETTERS = setOf('А', 'Б', 'В', 'Г', 'Д', 'Ѓ', 'Е', 'Ж', 'З', 'Ѕ', 'И', 'Ј', 'К', 'Л', 'Љ', 'М', 'Н', 'Њ', 'О', 'П', 'Р', 'С', 'Т', 'Ќ', 'У', 'Ф', 'Х', 'Ц', 'Ч', 'Џ', 'Ш', 'а', 'б', 'в', 'г', 'д', 'ѓ', 'е', 'ж', 'з', 'ѕ', 'и', 'ј', 'к', 'л', 'љ', 'м', 'н', 'њ', 'о', 'п', 'р', 'с', 'т', 'ќ', 'у', 'ф', 'х', 'ц', 'ч', 'џ', 'ш')

    private val UKRAINIAN_SPECIFIC_CYRILLIC_LETTERS = setOf('Ґ', 'ґ', 'Є', 'є', 'І', 'і', 'Ї', 'ї')
    private val SERBIAN_SPECIFIC_CYRILLIC_LETTERS = setOf('Ђ', 'ђ', 'Ј', 'ј', 'Љ', 'љ', 'Њ', 'њ', 'Ћ', 'ћ', 'Џ', 'џ')
    private val BELARUSIAN_SPECIFIC_CYRILLIC_LETTERS = setOf('Ў', 'ў', 'І', 'і')
    private val KYRGYZ_SPECIFIC_CYRILLIC_LETTERS = setOf('Ң', 'ң', 'Ө', 'ө', 'Ү', 'ү')
    private val MACEDONIAN_SPECIFIC_CYRILLIC_LETTERS = setOf('Ѓ', 'ѓ', 'Ѕ', 'ѕ', 'Ќ', 'ќ')

    fun isJapanese(text: String, entireLyricsHasKana: Boolean = false): Boolean {
        if (text.any { it in '\u3040'..'\u309F' || it in '\u30A0'..'\u30FF' }) return true
        return entireLyricsHasKana && text.any { it in '\u4E00'..'\u9FFF' }
    }

    fun isKorean(text: String) = text.any { it in '\uAC00'..'\uD7A3' }
    fun isHindi(text: String) = text.any { it in '\u0900'..'\u097F' }
    fun isPunjabi(text: String) = text.any { it in '\u0A00'..'\u0A7F' }
    fun isCyrillic(text: String) = text.any { it in '\u0400'..'\u04FF' }
    fun isChinese(text: String) = text.any { it in '\u4E00'..'\u9FFF' }

    fun isScriptThatNeedsRomanization(text: String): Boolean {
        return text.any { char ->
            val code = char.code
            code in 0x3040..0x309F ||
            code in 0x30A0..0x30FF ||
            code in 0x4E00..0x9FFF ||
            code in 0xAC00..0xD7A3 ||
            code in 0x0900..0x097F ||
            code in 0x0A00..0x0A7F ||
            code in 0x0400..0x04FF
        }
    }

    fun romanizeText(text: String, entireLyricsHasKana: Boolean = false): String? {
        return when {
            isJapanese(text, entireLyricsHasKana) -> romanizeJapanese(text)
            isChinese(text) -> romanizeChinese(text)
            isKorean(text) -> romanizeKorean(text)
            isHindi(text) -> romanizeHindi(text)
            isPunjabi(text) -> romanizePunjabi(text)
            isCyrillic(text) -> romanizeCyrillic(text)
            else -> null
        }?.let { capitalizeFirstLetter(it) }?.trim()
    }

    fun enrichLyrics(lyrics: Lyrics): Lyrics {
        val hasKana = lyrics.synced?.any { line -> line.line.any { it in '\u3040'..'\u309F' || it in '\u30A0'..'\u30FF' } } == true ||
            lyrics.plain?.any { line -> line.any { it in '\u3040'..'\u309F' || it in '\u30A0'..'\u30FF' } } == true

        val enrichedSynced = lyrics.synced?.map { line ->
            if (!line.romanization.isNullOrBlank()) {
                line
            } else if (isScriptThatNeedsRomanization(line.line)) {
                line.copy(romanization = romanizeText(line.line, hasKana))
            } else {
                line
            }
        }

        val enrichedPlain = lyrics.plain?.map { rawLine ->
            val parts = rawLine.split("\n")
            val primary = parts.firstOrNull().orEmpty()
            if (parts.size > 1) {
                rawLine
            } else if (isScriptThatNeedsRomanization(primary)) {
                val rom = romanizeText(primary, hasKana)
                if (!rom.isNullOrBlank()) "$primary\n$rom" else primary
            } else {
                rawLine
            }
        }

        return lyrics.copy(synced = enrichedSynced, plain = enrichedPlain)
    }

    internal fun isRussian(text: String) = text.any { it in RUSSIAN_CYRILLIC_LETTERS } && text.all { it in RUSSIAN_CYRILLIC_LETTERS || it !in '\u0400'..'\u04FF' }
    internal fun isUkrainian(text: String) = text.any { it in UKRAINIAN_CYRILLIC_LETTERS || it in UKRAINIAN_SPECIFIC_CYRILLIC_LETTERS } && text.all { it in UKRAINIAN_CYRILLIC_LETTERS || it in UKRAINIAN_SPECIFIC_CYRILLIC_LETTERS || it !in '\u0400'..'\u04FF' }
    internal fun isSerbian(text: String) = text.any { it in SERBIAN_CYRILLIC_LETTERS || it in SERBIAN_SPECIFIC_CYRILLIC_LETTERS } && text.all { it in SERBIAN_CYRILLIC_LETTERS || it in SERBIAN_SPECIFIC_CYRILLIC_LETTERS || it !in '\u0400'..'\u04FF' }
    internal fun isBulgarian(text: String) = text.any { it in BULGARIAN_CYRILLIC_LETTERS } && text.all { it in BULGARIAN_CYRILLIC_LETTERS || it !in '\u0400'..'\u04FF' }
    internal fun isBelarusian(text: String) = text.any { it in BELARUSIAN_CYRILLIC_LETTERS || it in BELARUSIAN_SPECIFIC_CYRILLIC_LETTERS } && text.all { it in BELARUSIAN_CYRILLIC_LETTERS || it in BELARUSIAN_SPECIFIC_CYRILLIC_LETTERS || it !in '\u0400'..'\u04FF' }
    internal fun isKyrgyz(text: String) = text.any { it in KYRGYZ_CYRILLIC_LETTERS || it in KYRGYZ_SPECIFIC_CYRILLIC_LETTERS } && text.all { it in KYRGYZ_CYRILLIC_LETTERS || it in KYRGYZ_SPECIFIC_CYRILLIC_LETTERS || it !in '\u0400'..'\u04FF' }
    internal fun isMacedonian(text: String) = text.any { it in MACEDONIAN_CYRILLIC_LETTERS || it in MACEDONIAN_SPECIFIC_CYRILLIC_LETTERS } && text.all { it in MACEDONIAN_CYRILLIC_LETTERS || it in MACEDONIAN_SPECIFIC_CYRILLIC_LETTERS || it !in '\u0400'..'\u04FF' }

    fun romanizeJapanese(japaneseText: String): String? {
        val tokenizer = kuromojiTokenizer ?: return null

        return try {
            val tokens = tokenizer.tokenize(japaneseText)

            val readings = mutableListOf<Triple<String, String, String>>()
            var i = 0
            while (i < tokens.size) {
                val token = tokens[i]
                val next = tokens.getOrNull(i + 1)

                when {
                    token.surface == "一" && next?.surface == "人" -> {
                        readings += Triple("ヒトリ", "名詞", "一般"); i += 2; continue
                    }
                    token.surface == "二" && next?.surface == "人" -> {
                        readings += Triple("フタリ", "名詞", "一般"); i += 2; continue
                    }
                    token.surface == "今日" -> {
                        readings += Triple("キョウ", "名詞", "一般"); i++; continue
                    }
                    token.surface == "明日" -> {
                        readings += Triple("アシタ", "名詞", "一般"); i++; continue
                    }
                    token.surface == "昨日" -> {
                        readings += Triple("キノウ", "名詞", "一般"); i++; continue
                    }
                }

                val pos1 = token.partOfSpeechLevel1 ?: ""
                val pos2 = token.partOfSpeechLevel2 ?: ""
                val surface = token.surface ?: ""
                val reading = token.reading?.takeIf { it.isNotBlank() && it != "*" }

                val kata = when {
                    pos1 == "助詞" -> when (surface) {
                        "は" -> "ワ"
                        "へ" -> "エ"
                        "を" -> "オ"
                        else -> reading ?: surface
                    }
                    else -> reading ?: surface
                }
                readings += Triple(kata, pos1, pos2)
                i++
            }

            val kataBuf = StringBuilder()
            readings.forEachIndexed { idx, (kata, pos1, pos2) ->
                val prevKata = readings.getOrNull(idx - 1)?.first ?: ""
                val prevEndsWithSokuon = prevKata.endsWith("ッ") || prevKata.endsWith("っ")
                val noSpace = idx == 0
                    || prevEndsWithSokuon
                    || pos1 == "助動詞"
                    || pos2 == "接尾"
                    || pos1 == "記号"
                    || pos2 == "非自立"
                if (!noSpace) kataBuf.append(" ")
                kataBuf.append(kata)
            }

            val katakanaText = kataBuf.toString().replace(MULTI_SPACE_REGEX, " ").trim()

            val latin = Transliterator
                .getInstance("Katakana-Latin; Lower")
                .transliterate(katakanaText)

            latin
                .replace(JAPANESE_N_ASSIMILATION_REGEX, "m")
                .replace(MULTI_SPACE_REGEX, " ")
                .trim()
        } catch (_: Throwable) {
            null
        }
    }

    private val POLYPHONE_OVERRIDE = mapOf(
        '中' to "zhong", '行' to "xing", '长' to "chang", '重' to "zhong",
        '好' to "hao", '觉' to "jue", '得' to "de", '地' to "di",
        '的' to "de", '了' to "le", '还' to "hai", '只' to "zhi",
        '着' to "zhe", '没' to "mei", '为' to "wei", '难' to "nan",
        '空' to "kong", '间' to "jian", '数' to "shu", '差' to "cha",
        '调' to "diao", '传' to "chuan", '弹' to "tan", '发' to "fa",
        '分' to "fen", '过' to "guo", '和' to "he", '会' to "hui",
        '看' to "kan", '男' to "nan", '女' to "nu", '儿' to "er",
        '那' to "na", '哪' to "na", '啊' to "a", '吗' to "ma",
        '呢' to "ne", '吧' to "ba", '嗯' to "en", '哦' to "o",
        '喔' to "o", '哇' to "wa", '咦' to "yi", '呀' to "ya", '嘛' to "ma"
    )

    private val CONTEXT_PINYIN = mapOf(
        '音' to mapOf('乐' to "yue"),
        '快' to mapOf('乐' to "le"),
        '欢' to mapOf('乐' to "le"),
        '娱' to mapOf('乐' to "le"),
        '生' to mapOf('长' to "zhang"),
        '成' to mapOf('长' to "zhang"),
        '长' to mapOf('大' to "da"),
        '重' to mapOf('复' to "fu"),
        '再' to mapOf('重' to "chong"),
        '严' to mapOf('重' to "zhong"),
        '地' to mapOf('方' to "di"),
        '大' to mapOf('地' to "di"),
        '土' to mapOf('地' to "di")
    )

    fun romanizeChinese(text: String): String? {
        return try {
            val format = HanyuPinyinOutputFormat().apply {
                caseType = HanyuPinyinCaseType.LOWERCASE
                toneType = HanyuPinyinToneType.WITHOUT_TONE
            }

            val chars = text.toList()
            val sb = StringBuilder()
            var idx = 0

            while (idx < chars.size) {
                val c = chars[idx]
                val next = chars.getOrNull(idx + 1)

                when {
                    next == '儿' && idx + 1 < chars.size
                        && c in '\u4E00'..'\u9FA5'
                        && c != '儿' -> {
                        val base = getPinyin(c, format, prevChar = chars.getOrNull(idx - 1))
                        val erhua = base
                            .replace(ERHUA_NG_REGEX, "r")
                            .replace(ERHUA_N_REGEX, "r")
                            .let { if (!it.endsWith("r")) it + "r" else it }
                        sb.append(erhua).append(" ")
                        idx += 2
                        continue
                    }

                    c in '\u4E00'..'\u9FA5' -> {
                        sb.append(getPinyin(c, format, prevChar = chars.getOrNull(idx - 1))).append(" ")
                        idx++
                        continue
                    }

                    else -> {
                        sb.append(c)
                        idx++
                        continue
                    }
                }
            }

            sb.toString().replace(MULTI_SPACE_REGEX, " ").trim()
        } catch (_: Throwable) {
            null
        }
    }

    private fun getPinyin(
        c: Char,
        format: HanyuPinyinOutputFormat,
        prevChar: Char? = null
    ): String {
        prevChar?.let { prev ->
            CONTEXT_PINYIN[prev]?.get(c)?.let { return it }
        }

        POLYPHONE_OVERRIDE[c]?.let { return it }

        return try {
            PinyinHelper.toHanyuPinyinStringArray(c, format)
                ?.firstOrNull()
                ?.trimEnd('0', '1', '2', '3', '4', '5')
                ?: c.toString()
        } catch (_: Exception) {
            c.toString()
        }
    }

    fun romanizeKorean(text: String): String {
        val romajaBuilder = StringBuilder()
        var prevFinal: String? = null
        for (i in text.indices) {
            val char = text[i]
            if (char in '\uAC00'..'\uD7A3') {
                val syllableIndex = char.code - 0xAC00
                val choIndex = syllableIndex / (21 * 28)
                val jungIndex = (syllableIndex % (21 * 28)) / 28
                val jongIndex = syllableIndex % 28

                val choChar = (0x1100 + choIndex).toChar().toString()
                val jungChar = (0x1161 + jungIndex).toChar().toString()
                val jongChar = if (jongIndex == 0) null else (0x11A7 + jongIndex).toChar().toString()

                if (prevFinal != null) {
                    val contextKey = prevFinal + choChar
                    val jong = HANGUL_ROMAJA_MAP["jong"]?.get(contextKey) ?: HANGUL_ROMAJA_MAP["jong"]?.get(prevFinal) ?: prevFinal
                    romajaBuilder.append(jong)
                }

                val cho = HANGUL_ROMAJA_MAP["cho"]?.get(choChar) ?: choChar
                val jung = HANGUL_ROMAJA_MAP["jung"]?.get(jungChar) ?: jungChar
                romajaBuilder.append(cho).append(jung)
                prevFinal = jongChar
            } else {
                if (prevFinal != null) {
                    romajaBuilder.append(HANGUL_ROMAJA_MAP["jong"]?.get(prevFinal) ?: prevFinal)
                    prevFinal = null
                }
                romajaBuilder.append(char)
            }
        }
        if (prevFinal != null) romajaBuilder.append(HANGUL_ROMAJA_MAP["jong"]?.get(prevFinal) ?: prevFinal)
        return romajaBuilder.toString()
    }

    fun romanizeHindi(text: String): String {
        val sb = StringBuilder(text.length)
        var i = 0
        while (i < text.length) {
            var consumed = false
            if (i + 1 < text.length) {
                val twoCharCandidate = text.substring(i, i + 2)
                val mappedTwoChar = DEVANAGARI_ROMAJI_MAP[twoCharCandidate]
                if (mappedTwoChar != null) {
                    sb.append(mappedTwoChar)
                    i += 2
                    consumed = true
                }
            }
            if (!consumed) {
                val charStr = text[i].toString()
                sb.append(DEVANAGARI_ROMAJI_MAP[charStr] ?: charStr)
                i += 1
            }
        }
        return sb.toString()
    }

    fun romanizePunjabi(text: String): String {
        val sb = StringBuilder(text.length)
        var i = 0
        while (i < text.length) {
            val char = text[i]
            var consumed = false
            if (char == '\u0A71') {
                if (i + 1 < text.length) {
                    val nextCharStr = text[i + 1].toString()
                    val nextMapped = GURMUKHI_ROMAJI_MAP[nextCharStr]
                    if (nextMapped != null && nextMapped.isNotEmpty()) {
                        sb.append(nextMapped[0])
                    }
                }
                i++
                continue
            }
            if (i + 1 < text.length) {
                val twoCharCandidate = text.substring(i, i + 2)
                val mappedTwoChar = GURMUKHI_ROMAJI_MAP[twoCharCandidate]
                if (mappedTwoChar != null) {
                    sb.append(mappedTwoChar)
                    i += 2
                    consumed = true
                }
            }
            if (!consumed) {
                val str = char.toString()
                sb.append(GURMUKHI_ROMAJI_MAP[str] ?: str)
                i++
            }
        }
        return sb.toString()
    }

    fun romanizeCyrillic(text: String): String? {
        if (text.isEmpty()) return null
        val cyrillicChars = text.filter { it in '\u0400'..'\u04FF' }
        if (cyrillicChars.isEmpty() || (cyrillicChars.length == 1 && (cyrillicChars[0] == 'е' || cyrillicChars[0] == 'Е'))) return null

        return when {
            isRussian(text) -> processCyrillicWordByWord(text, RUSSIAN_ROMAJI_MAP, isRussian = true)
            isUkrainian(text) -> processUkrainian(text)
            isSerbian(text) -> processCyrillicWordByWord(text, SERBIAN_ROMAJI_MAP)
            isBulgarian(text) -> processCyrillicWordByWord(text, BULGARIAN_ROMAJI_MAP)
            isBelarusian(text) -> processBelarusian(text)
            isKyrgyz(text) -> processCyrillicWordByWord(text, KYRGYZ_ROMAJI_MAP)
            isMacedonian(text) -> processCyrillicWordByWord(text, MACEDONIAN_ROMAJI_MAP)
            else -> processCyrillicWordByWord(text, emptyMap())
        }
    }

    private fun processCyrillicWordByWord(text: String, specificMap: Map<String, String>, isRussian: Boolean = false): String {
        val romajiBuilder = StringBuilder(text.length)
        val words = text.split(CYRILLIC_WORD_SPLIT_REGEX).filter { it.isNotEmpty() }

        words.forEach { word ->
            if ((word.length == 1 && word[0] in CYRILLIC_PUNCTUATION_CHARS) || word.isBlank()) {
                romajiBuilder.append(word)
                return@forEach
            }

            var charIndex = 0
            while (charIndex < word.length) {
                val char = word[charIndex]
                val charStr = char.toString()
                val prevChar = word.getOrNull(charIndex - 1)

                if (charStr == "ъ" || charStr == "Ъ") {
                    charIndex++; continue
                }

                if (charStr == "ь" || charStr == "Ь") {
                    val built = romajiBuilder.toString()
                    val palatalized = when {
                        built.endsWith("t") -> built.dropLast(1) + "tʲ"
                        built.endsWith("d") -> built.dropLast(1) + "dʲ"
                        built.endsWith("n") -> built.dropLast(1) + "ny"
                        built.endsWith("l") -> built.dropLast(1) + "ly"
                        built.endsWith("s") -> built.dropLast(1) + "sy"
                        built.endsWith("z") -> built.dropLast(1) + "zy"
                        built.endsWith("r") -> built.dropLast(1) + "ry"
                        built.endsWith("p") -> built.dropLast(1) + "py"
                        built.endsWith("b") -> built.dropLast(1) + "by"
                        built.endsWith("m") -> built.dropLast(1) + "my"
                        built.endsWith("v") -> built.dropLast(1) + "vy"
                        built.endsWith("f") -> built.dropLast(1) + "fy"
                        built.endsWith("k") -> built.dropLast(1) + "ky"
                        built.endsWith("g") -> built.dropLast(1) + "gy"
                        built.endsWith("kh") -> built.dropLast(2) + "khy"
                        else -> built
                    }
                    romajiBuilder.clear(); romajiBuilder.append(palatalized)
                    charIndex++; continue
                }

                if (isRussian && (charStr == "е" || charStr == "Е")) {
                    val afterVowelOrStart = charIndex == 0
                        || prevChar == null
                        || prevChar.isWhitespace()
                        || "аеёиоуыэюяАЕЁИОУЫЭЮЯ".contains(prevChar)
                        || prevChar == 'ь' || prevChar == 'ъ' || prevChar == 'Ь' || prevChar == 'Ъ'
                    romajiBuilder.append(if (afterVowelOrStart) {
                        if (char.isUpperCase()) "Ye" else "ye"
                    } else {
                        if (char.isUpperCase()) "E" else "e"
                    })
                    charIndex++; continue
                }

                if (charStr == "ё" || charStr == "Ё") {
                    romajiBuilder.append(if (char.isUpperCase()) "Yo" else "yo")
                    charIndex++; continue
                }

                if ((charStr == "и" || charStr == "И") && prevChar != null) {
                    romajiBuilder.append(specificMap[charStr] ?: GENERAL_CYRILLIC_ROMAJI_MAP[charStr] ?: charStr)
                    charIndex++; continue
                }

                var consumed = false
                if (charIndex + 2 < word.length) {
                    val three = word.substring(charIndex, charIndex + 3)
                    specificMap[three]?.let { romajiBuilder.append(it); charIndex += 3; consumed = true }
                }
                if (!consumed && charIndex + 1 < word.length) {
                    val two = word.substring(charIndex, charIndex + 2)
                    specificMap[two]?.let { romajiBuilder.append(it); charIndex += 2; consumed = true }
                }
                if (!consumed) {
                    romajiBuilder.append(specificMap[charStr] ?: GENERAL_CYRILLIC_ROMAJI_MAP[charStr] ?: charStr)
                    charIndex++
                }
            }
        }
        return romajiBuilder.toString()
    }

    private fun processUkrainian(text: String): String {
        val romajiBuilder = StringBuilder(text.length)
        val words = text.split(CYRILLIC_WORD_SPLIT_REGEX).filter { it.isNotEmpty() }

        words.forEach { word ->
            if ((word.length == 1 && word[0] in CYRILLIC_PUNCTUATION_CHARS) || word.isBlank()) {
                romajiBuilder.append(word)
            } else {
                var charIndex = 0
                while (charIndex < word.length) {
                    val charStr = word[charIndex].toString()
                    var processed = false
                    if (charIndex > 0 && word[charIndex - 1].isLetter() && !"АаЕеЄєИиІіЇїОоУуЮюЯяЫыЭэ".contains(word[charIndex - 1])) {
                        if (charStr == "Ю") { romajiBuilder.append("Iu"); processed = true }
                        else if (charStr == "ю") { romajiBuilder.append("iu"); processed = true }
                        else if (charStr == "Я") { romajiBuilder.append("Ia"); processed = true }
                        else if (charStr == "я") { romajiBuilder.append("ia"); processed = true }
                    }
                    if (!processed) {
                        romajiBuilder.append(UKRAINIAN_ROMAJI_MAP[charStr] ?: GENERAL_CYRILLIC_ROMAJI_MAP[charStr] ?: charStr)
                    }
                    charIndex++
                }
            }
        }
        return romajiBuilder.toString()
    }

    private fun processBelarusian(text: String): String {
        val romajiBuilder = StringBuilder(text.length)
        val words = text.split(CYRILLIC_WORD_SPLIT_REGEX).filter { it.isNotEmpty() }

        words.forEach { word ->
            if ((word.length == 1 && word[0] in CYRILLIC_PUNCTUATION_CHARS) || word.isBlank()) {
                romajiBuilder.append(word)
            } else {
                var charIndex = 0
                while (charIndex < word.length) {
                    val charStr = word[charIndex].toString()
                    if ((charStr == "е" || charStr == "Е") && (charIndex == 0 || word[charIndex - 1].isWhitespace())) {
                        romajiBuilder.append(if (charStr == "е") "ye" else "Ye")
                    } else {
                        romajiBuilder.append(BELARUSIAN_ROMAJI_MAP[charStr] ?: GENERAL_CYRILLIC_ROMAJI_MAP[charStr] ?: charStr)
                    }
                    charIndex += 1
                }
            }
        }
        return romajiBuilder.toString()
    }

    fun capitalizeFirstLetter(text: String): String {
        if (text.isEmpty()) return text
        return text.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
    }
}
