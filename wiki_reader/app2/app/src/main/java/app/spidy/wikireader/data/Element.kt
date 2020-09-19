package app.spidy.wikireader.data

data class Element(
    val tagName: String,
    val index: Int,
    val text: String,
    val uId: String
)