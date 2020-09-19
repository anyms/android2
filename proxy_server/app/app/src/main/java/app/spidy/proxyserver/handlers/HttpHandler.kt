package app.spidy.proxyserver.handlers

import app.spidy.kotlinutils.debug
import app.spidy.proxyserver.data.Request
import java.io.*
import java.net.Socket
import java.util.*

class HttpHandler(
    private val clientOutputStream: OutputStream,
    private val clientInputStream: InputStream,
    private val requestLine: String
) {
    fun run() {
        handleRequest()
    }

    private fun handleRequest() {
        val request = getRequest() ?: return
        request.headers["connection"] = "close"

//        if (AdBlocker.isAd(request.host, true)) {
////            debug("BLOCKED => ${request.host}")
//            return
//        }

        val initRequest = createRequest(request)

        debug(request.host)

        val server = Socket(request.host, request.port)
        server.getOutputStream().write(initRequest.toByteArray())

        if (request.headers.containsKey("content-length")) {
            var tmp = 0
            val buffer = ByteArray(4096)
            while (true) {
                val d = clientInputStream.read(buffer)
                tmp += d
                server.getOutputStream().write(buffer)

                if (tmp >= request.headers["content-length"]!!.toInt()) break
            }
        }


        val buffer = ByteArray(4096)
        while (true) {
            try {
                val len = server.getInputStream().read(buffer)
                if (len > 0) {
                    clientOutputStream.write(buffer, 0, len)
                } else {
                    break
                }
            } catch (e: Exception) {
                break
            }
        }

        server.close()
    }

    private fun createRequest(request: Request): String {
        var s = "${request.method} ${request.path} ${request.protocol}\r\n"
        for ((k, v) in request.headers) {
            s += "${k}: $v\r\n"
        }
        return "$s\r\n"
    }

    private fun getRequest(): Request? {
        val buffer = ByteArray(1)
        var data = ""

        while (true) {
            try {
                val recv = clientInputStream.read(buffer)
                data += String(buffer)

                if (data.endsWith("\r\n\r\n")) break
            } catch (e: Exception) {
                return null
            }
        }

        val request = parseRequest(requestLine)
        val lines = data.trim().split("\r\n")

        for (line in lines) {
            val parts = line.split(": ").toMutableList()
            val k = parts.removeAt(0).toLowerCase(Locale.ROOT)
            val v = parts.joinToString(": ")
            request.headers[k] = v
        }
        return request
    }

    private fun parseRequest(requestLine: String): Request {
        val parts = requestLine.split(" ")
        val method = parts[0]
        val urlNodes = parts[1].split("://").last().split("/").toMutableList()
        var host = urlNodes.removeAt(0)
        val path = "/${urlNodes.joinToString("/")}"
        var port = 80

        if (host.contains(":")) {
            val tmp = host.split(":")
            port = tmp.last().toInt()
            host = tmp[0]
        }

        return Request(host, path, method, parts[2], parts[1], port)
    }

    private fun forwardData(inputStream: InputStream, outputStream: OutputStream) {
        try {
            val buffer = ByteArray(4096)
            var read: Int
            do {
                read = inputStream.read(buffer)
                if (read > 0) {
                    outputStream.write(buffer, 0, read)
                    if (inputStream.available() < 1) {
                        outputStream.flush()
                    }
                }
            } while (read >= 0)
        } catch (e: IOException) {
            e.printStackTrace() // TODO: implement catch
        }
    }
}