package app.spidy.pirum.data

data class OtherHistory(
    var playlistName: String,
    var title: String,
    val type: String,
    var itemCount: Int,
    var src: String = "",
    var isToRead: Boolean = false
)