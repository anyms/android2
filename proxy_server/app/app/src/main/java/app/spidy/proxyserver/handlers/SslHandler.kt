package app.spidy.proxyserver.handlers

import java.io.*
import java.net.Socket
import java.util.regex.Matcher

class SslHandler(
    private val clientOutputStream: OutputStream,
    private val clientInputStream: InputStream,
    private val matcher: Matcher
) {

    fun run() {
        var header: String?
        do {
            header = getLine(clientInputStream)
        } while ("" != header)
        val outputStreamWriter = OutputStreamWriter(
            clientOutputStream,
            "ISO-8859-1"
        )
//        val host = matcher.group(1)!!
//        if (AdBlocker.isAd(host, true)) {
//            Log.d("hell", "BLOCKED => $host")
//            outputStreamWriter.write(
//                "HTTP/${matcher.group(3)} 502 Bad Gateway\r\n"
//            )
//            outputStreamWriter.write("Proxy-agent: Spidy/0.1\r\n")
//            outputStreamWriter.write("\r\n")
//            outputStreamWriter.flush()
//            return
//        }

        val forwardSocket: Socket
        try {
            forwardSocket = Socket(matcher.group(1), matcher.group(2)!!.toInt())
        } catch (e: IOException) {
            e.printStackTrace() // TODO: implement catch
            outputStreamWriter.write(
                "HTTP/${matcher.group(3)} 502 Bad Gateway\r\n"
            )
            outputStreamWriter.write("Proxy-agent: Spidy/0.1\r\n")
            outputStreamWriter.write("\r\n")
            outputStreamWriter.flush()
            return
        } catch (e: NumberFormatException) {
            e.printStackTrace()
            outputStreamWriter.write(
                "HTTP/${matcher.group(3)} 502 Bad Gateway\r\n"
            )
            outputStreamWriter.write("Proxy-agent: Spidy/0.1\r\n")
            outputStreamWriter.write("\r\n")
            outputStreamWriter.flush()
            return
        }
        try {
            outputStreamWriter.write(
                "HTTP/${matcher.group(3)} 200 Connection established\r\n"
            )
            outputStreamWriter.write("Proxy-agent: Spidy/0.1\r\n")
            outputStreamWriter.write("\r\n")
            outputStreamWriter.flush()
            val remoteToClient: Thread = object : Thread() {
                override fun run() {
                    forwardData(forwardSocket.getInputStream(), clientOutputStream)
                }
            }
            remoteToClient.start()
            try {
                val read: Int = clientInputStream.read()
                if (read != -1) {
                    if (read != '\n'.toInt()) {
                        forwardSocket.getOutputStream().write(read)
                    }
                    forwardData(clientInputStream, forwardSocket.getOutputStream())
                } else {
                    if (!forwardSocket.isOutputShutdown) {
                        forwardSocket.shutdownOutput()
                    }
                }
            } finally {
                try {
                    remoteToClient.join()
                } catch (e: InterruptedException) {
                    e.printStackTrace() // TODO: implement catch
                }
            }
        } finally {
            forwardSocket.close()
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