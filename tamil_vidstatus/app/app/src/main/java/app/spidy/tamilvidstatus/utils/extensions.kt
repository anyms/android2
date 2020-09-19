package app.spidy.tamilvidstatus.utils

import java.util.*

fun String.toSlug() = toLowerCase(Locale.ROOT)
    .replace("\n", " ")
    .replace("[^a-z\\d\\s]".toRegex(), " ")
    .split(" ")
    .joinToString("-")
    .replace("-+".toRegex(), "-")