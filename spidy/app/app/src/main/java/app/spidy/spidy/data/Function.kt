package app.spidy.spider.data

import org.json.JSONObject

data class Function(
    val name: String,
    val statements: List<JSONObject>
)