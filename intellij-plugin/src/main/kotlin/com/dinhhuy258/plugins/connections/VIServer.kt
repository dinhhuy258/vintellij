package com.dinhhuy258.plugins.connections

import com.dinhhuy258.plugins.exceptions.HandlerNotFoundException
import com.dinhhuy258.plugins.handlers.ImportSuggestionsHandler
import com.dinhhuy258.plugins.handlers.OpenFileHandler
import com.dinhhuy258.plugins.handlers.VIHandler
import com.google.gson.Gson
import com.google.gson.JsonStreamParser
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.nio.charset.StandardCharsets
import java.util.concurrent.Executors
import kotlin.concurrent.thread

class VIServer(private val port: Int) {
    private val gson: Gson = Gson()

    private val handlers: Map<String, VIHandler>

    init {
        handlers = mapOf(
                "import" to ImportSuggestionsHandler(),
                "open" to OpenFileHandler()
        )
    }

    fun start() {
        thread(start = true) {
            val executorService = Executors.newCachedThreadPool()
            ServerSocket(port, 0, InetAddress.getLoopbackAddress()).use { serverSocket ->
                while (true) {
                    val socket: Socket = serverSocket.accept()
                    executorService.execute {
                        process(socket)
                    }
                }
            }
        }
    }

    private fun process(socket: Socket) {
        socket.use {
            val parser = JsonStreamParser(InputStreamReader(BufferedInputStream(socket.getInputStream()), StandardCharsets.UTF_8))
            val writer = OutputStreamWriter(BufferedOutputStream(socket.getOutputStream()))
            while (parser.hasNext()) {
                val request: ServerRequest = gson.fromJson(parser.next(), ServerRequest::class.java)
                val response = processRequest(request)
                writer.write(response)
                writer.flush()
            }
        }
    }

    private fun processRequest(serverRequest: ServerRequest): String {
        val handler = handlers[serverRequest.handler]
                ?: throw HandlerNotFoundException("Handler ${serverRequest.handler} not found!!!")

        return handler.handle(serverRequest.data)
    }
}
