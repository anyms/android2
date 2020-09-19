package app.spidy.spidy.data

import app.spidy.spidy.interpreter.SpidyScript2

data class Process(
    val id: Int,
    val code: String,
    val interpreter: SpidyScript2,
    var log: String = ""
)