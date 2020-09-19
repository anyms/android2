package app.spidy.wikireader.utils

import app.spidy.wikireader.data.Language

object LanguageUtil {
    const val TAG_CURRENT_LANGUAGE_CODE = "app.spidy.wikireader.TAG_CURRENT_LANGUAGE_CODE"
    const val TAG_CURRENT_LANGUAGE_NAME = "app.spidy.wikireader.TAG_CURRENT_LANGUAGE_NAME"

    private val ibmLangs = hashMapOf(
        "en" to "English"
    )

    private val langs = hashMapOf(
        "ta" to "தமிழ்",
        "si" to "සිංහල",
        "es" to "Español",
        "eo" to "Esperanto",
        "fr" to "Français",
        "io" to "Ido",
        "ia" to "Interlingua",
        "pt" to "Português",
        "en" to "English",
        "gn" to "Avañe'ẽ",
        "es" to "Español",
        "fr" to "Français",
        "gcr" to "Kriyòl gwiyannen",
        "nl" to "Nederlands",
        "nah" to "Nāhuatl",
        "pt" to "Português",
        "qu" to "Runa Simi",
        "yi" to "ייִדיש",
        "kbd" to "Адыгэбзэ",
        "be" to "Беларуская",
        "be-tarask" to "Беларуская (тарашкевіца)‎",
        "bg" to "Български",
        "kv" to "Коми",
        "krc" to "Къарачай-малкъар",
        "mk" to "Македонски",
        "rue" to "Русиньскый",
        "ru" to "Русский",
        "sah" to "Саха тыла",
        "sr-cyrl" to "Српски / srpski",
        "tt" to "Татарча/tatarça",
        "uk" to "Українська",
        "myv" to "Эрзянь",
        "el" to "Ελληνικά",
        "an" to "Aragonés",
        "ast" to "Asturianu",
        "az-latn" to "Azərbaycanca",
        "bs" to "Bosanski",
        "br" to "Brezhoneg",
        "ca" to "Català",
        "cy" to "Cymraeg",
        "da" to "Dansk",
        "de" to "Deutsch",
        "et" to "Eesti",
        "es" to "Español",
        "eu" to "Euskara",
        "fr" to "Français",
        "ga" to "Gaeilge",
        "gl" to "Galego",
        "gd" to "Gàidhlig",
        "hr" to "Hrvatski",
        "it" to "Italiano",
        "kw" to "Kernowek",
        "ku-latn" to "Kurdî",
        "la" to "Latina",
        "lv" to "Latviešu",
        "lt" to "Lietuvių",
        "lij" to "Ligure",
        "lb" to "Lëtzebuergesch",
        "hu" to "Magyar",
        "nl" to "Nederlands",
        "frr" to "Nordfriisk",
        "nb" to "Norsk bokmål",
        "nn" to "Norsk nynorsk",
        "oc" to "Occitan",
        "pms" to "Piemontèis",
        "pl" to "Polski",
        "pt" to "Português",
        "rmy" to "Romani čhib",
        "ro" to "Română",
        "sco" to "Scots",
        "sq" to "Shqip",
        "scn" to "Sicilianu",
        "sl" to "Slovenščina",
        "sh" to "Srpskohrvatski / српскохрватски",
        "fi" to "Suomi",
        "sv" to "Svenska",
        "tr" to "Türkçe",
        "is" to "Íslenska",
        "cs" to "Čeština",
        "szl" to "Ślůnski",
        "sgs" to "Žemaitėška",
        "yi" to "ייִדיש",
        "hy" to "Հայերեն",
        "xmf" to "მარგალური",
        "ka" to "ქართული",
        "ur" to "اردو",
        "ar" to "العربية",
        "az-arab" to "تۆرکجه",
        "fa" to "فارسی",
        "arz" to "مصرى",
        "pnb" to "پنجابی",
        "ckb" to "کوردی",
        "kbd" to "Адыгэбзэ",
        "ru" to "Русский",
        "az-latn" to "Azərbaycanca",
        "ku-latn" to "Kurdî",
        "tr" to "Türkçe",
        "yi" to "ייִדיש",
        "he" to "עברית",
        "mr" to "मराठी",
        "ml" to "മലയാളം",
        "hy" to "Հայերեն",
        "am" to "አማርኛ",
        "ar" to "العربية",
        "arz" to "مصرى",
        "af" to "Afrikaans",
        "es" to "Español",
        "rw" to "Kinyarwanda",
        "sw" to "Kiswahili",
        "mg" to "Malagasy",
        "pt" to "Português",
        "so" to "Soomaaliga",
        "yo" to "Yorùbá",
        "ug-arab" to "ئۇيغۇرچە / Uyghurche",
        "ur" to "اردو",
        "az-arab" to "تۆرکجه",
        "fa" to "فارسی",
        "pnb" to "پنجابی",
        "zh" to "中文",
        "wuu" to "吴语",
        "ja" to "日本語",
        "yue" to "粵語",
        "ko" to "한국어",
        "mn" to "Монгол",
        "ru" to "Русский",
        "sah" to "Саха тыла",
        "tg-cyrl" to "Тоҷикӣ",
        "id" to "Bahasa Indonesia",
        "ms" to "Bahasa Melayu",
        "bjn" to "Banjar",
        "bcl" to "Bikol Central",
        "nan" to "Bân-lâm-gú",
        "ceb" to "Cebuano",
        "hif" to "Fiji Hindi",
        "hak" to "客家語/Hak-kâ-ngî",
        "ilo" to "Ilokano",
        "jv" to "Jawa",
        "min" to "Minangkabau",
        "uz" to "Oʻzbekcha/ўзбекча",
        "pt" to "Português",
        "tl" to "Tagalog",
        "vi" to "Tiếng Việt",
        "war" to "Winaray",
        "dv" to "ދިވެހިބަސް",
        "dty" to "डोटेली",
        "new" to "नेपाल भाषा",
        "ne" to "नेपाली",
        "bho" to "भोजपुरी",
        "mr" to "मराठी",
        "mai" to "मैथिली",
        "sa" to "संस्कृतम्",
        "hi" to "हिन्दी",
        "as" to "অসমীয়া",
        "bn" to "বাংলা",
        "pa-guru" to "ਪੰਜਾਬੀ",
        "gu" to "ગુજરાતી",
        "or" to "ଓଡ଼ିଆ",
        "ta" to "தமிழ்",
        "te" to "తెలుగు",
        "kn" to "ಕನ್ನಡ",
        "tcy" to "ತುಳು",
        "ml" to "മലയാളം",
        "si" to "සිංහල",
        "sat" to "ᱥᱟᱱᱛᱟᱲᱤ",
        "th" to "ไทย",
        "es" to "Español",
        "hif" to "Fiji Hindi",
        "jv" to "Jawa",
        "pt" to "Português"
    )

    fun getNames(): List<String> {
        val names = ArrayList<String>()
        for ((k, v) in langs) {
            names.add(v)
        }
        names.sort()
        return names
    }

    fun getCodes(): List<String> {
        val codes = ArrayList<String>()
        for ((k, v) in langs) {
            codes.add(k)
        }
        return codes
    }

    fun getLanguages(): List<Language> {
        val languages = ArrayList<Language>()
        for ((k, v) in langs) {
            languages.add(Language(k, v))
        }
        return languages
    }

    fun isIBMLanguage(lang: Language) {
        var isExist = false
        for ((k, v) in ibmLangs) {
            if (lang.code == k && lang.name == v) {
                isExist = true
            }
        }
    }
}