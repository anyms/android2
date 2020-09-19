package app.spidy.pirum.data

import app.spidy.pirum.utils.PlaylistStatus

data class History(
    var playlistName: String,
    var itemCount: Int,
    val type: String,
    var status: Int = PlaylistStatus.STATE_NONE
)