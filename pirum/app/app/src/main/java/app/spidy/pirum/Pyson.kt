package app.spidy.pirum

class Pyson(private val data: Any) {

    fun toJson(): String? {
        if (data::class.simpleName == "HashMap") {
            return parseHashMap(data as HashMap<String, String>)
        } else if (data::class.simpleName.toString().contains("List")) {
            return parseList(data as List<HashMap<String, String>>)
        }
        return null
    }

    private fun parseHashMap(map: HashMap<String, String>): String {
        val keys = map.keys
        var s = "{"
        for (k in keys) {
            s += "\"$k\": \"${map[k]}\","
        }
        s = s.dropLast(1)
        s += "}"
        return s
    }

    private fun parseList(lst: List<HashMap<String, String>>): String {
        var s = "["
        for (h in lst) {
            val keys = h.keys
            s += "{"
            for (k in keys) {
                s += "\"$k\": \"${h[k]}\","
            }
            s = s.dropLast(1)
            s += "},"
        }
        s = s.dropLast(1)
        s += "]"
        return s
    }
}