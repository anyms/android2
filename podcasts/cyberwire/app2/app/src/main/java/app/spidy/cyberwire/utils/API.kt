package app.spidy.cyberwire.utils

import app.spidy.hiper.Hiper

object API {
    val async = Hiper.getAsyncInstance()
    val sync = Hiper.getSyncInstance()

    private const val KEY = "yOaNqItBPEoM2r9pxvyude0PjJsXFR30"
    private const val URL = "https://www.suyambu.net/api/v1/cyberwire"

    fun url(path: String) = "$URL$path?key=$KEY"
}