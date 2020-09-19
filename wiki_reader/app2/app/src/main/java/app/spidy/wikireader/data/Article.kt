package app.spidy.wikireader.data

data class Article(
    val title: String,
    val elements: List<Element>,
    val url: String
)