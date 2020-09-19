package app.spidy.proxyserver.data

data class Request(
    val host: String,
    val path: String,
    val method: String,
    val protocol: String,
    val url: String,
    val port: Int,
    val headers: HashMap<String, String> = hashMapOf()
)