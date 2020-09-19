package app.spidy.proxyserver.handlers

import app.spidy.kotlinutils.debug
import java.io.*
import java.net.Socket
import java.util.regex.Matcher
import java.util.regex.Pattern

class ProxyHandler(
    private val clientSocket: Socket,
    private val isBlocked: (request: String, domain: String, isHttps: Boolean) -> Boolean
) {
    companion object {
        val CONNECT_PATTERN: Pattern = Pattern.compile(
            "CONNECT (.+):(.+) HTTP/(1\\.[01])",
            Pattern.CASE_INSENSITIVE
        )
    }

    fun run() {
        val clientInputStream = clientSocket.getInputStream()
        val clientOutputStream = clientSocket.getOutputStream()
        try {
            val request = getLine(clientInputStream)
            if (request == null) {
                clientSocket.close()
                return
            }

            val parts = request.split(" ")
            if (parts.size != 3) {
                clientSocket.close()
                return
            }

            var isHttps = false
            val domain = if (parts[0] == "CONNECT") {
                isHttps = true
                parts[1].split(":")[0]
            } else {
                parts[1].split("://")[1].split("?")[0].split("/")[0]
            }

            if (isBlocked(request, domain, isHttps)) {
                clientSocket.close()
                return
            }

            val matcher: Matcher = CONNECT_PATTERN.matcher(request)
            if (matcher.matches()) {
                val sslHandler = SslHandler(
                    clientOutputStream,
                    clientInputStream,
                    matcher
                )
                sslHandler.run()
            } else if (request.contains("://")) {
                val httpHandler = HttpHandler(
                    clientOutputStream,
                    clientInputStream,
                    request
                )
                httpHandler.run()
            }
        } catch (e: IOException) {
            debug(e)
            e.printStackTrace() // TODO: implement catch
        } finally {
            try {
                clientSocket.close()
            } catch (e: IOException) {
                e.printStackTrace() // TODO: implement catch
            }
        }
    }

    @Throws(IOException::class)
    private fun getLine(inputStream: InputStream): String? {
        val buffer = StringBuilder()
        var byteBuffer = inputStream.read()
        if (byteBuffer < 0) return ""
        do {
            if (byteBuffer != '\r'.toInt()) {
                buffer.append(byteBuffer.toChar())
            }
            byteBuffer = inputStream.read()
        } while (byteBuffer != '\n'.toInt() && byteBuffer >= 0)
        return buffer.toString()
    }
}