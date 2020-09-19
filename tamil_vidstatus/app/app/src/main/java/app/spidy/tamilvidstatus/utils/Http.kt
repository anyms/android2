package app.spidy.tamilvidstatus.utils

import app.spidy.hiper.Hiper

object Http {
    val async = Hiper.getAsyncInstance()
    val sync = Hiper.getSyncInstance()
    const val apiUrl = "https://suyambu.net/api/v1/vidstatus"
    const val apiKey = "yOaNqItBPEoM2r9pxvyude0PjJsXFR30"
}