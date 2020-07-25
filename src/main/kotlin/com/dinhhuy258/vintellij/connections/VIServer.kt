package com.dinhhuy258.vintellij.connections

import com.dinhhuy258.vintellij.exceptions.VIException
import com.dinhhuy258.vintellij.handlers.FindHierarchyHandler
import com.dinhhuy258.vintellij.handlers.FindUsageHandler
import com.dinhhuy258.vintellij.handlers.GoToDefinitionHandler
import com.dinhhuy258.vintellij.handlers.HealthCheckHandler
import com.dinhhuy258.vintellij.handlers.ImportSuggestionsHandler
import com.dinhhuy258.vintellij.handlers.OpenFileHandler
import com.dinhhuy258.vintellij.handlers.RefreshFileHandler
import com.dinhhuy258.vintellij.handlers.VIHandler
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
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

    private val handlers: Map<String, VIHandler> = mapOf(
            "import" to ImportSuggestionsHandler(),
            "open" to OpenFileHandler(),
            "goto" to GoToDefinitionHandler(),
            "refresh" to RefreshFileHandler(),
            "health-check" to HealthCheckHandler(),
            "find-hierarchy" to FindHierarchyHandler(),
            "find-usage" to FindUsageHandler()
    )

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
                val response = try {
                    val request: ServerRequest = gson.fromJson(parser.next(), ServerRequest::class.java)
                    ServerResponse.success(processRequest(request), request.handler)
                } catch (e: JsonParseException) {
                    ServerResponse.fail("Invalid json string.")
                } catch (e: VIException) {
                    ServerResponse.fail(e.message ?: "Internal server error.")
                } catch (e: Throwable) {
                    ServerResponse.fail(e.message ?: "Internal server error.")
                }
                writer.write(gson.toJson(response))
                writer.flush()
            }
        }
    }

    private fun processRequest(serverRequest: ServerRequest): JsonElement {
        val handler = handlers[serverRequest.handler]
                ?: throw VIException("Handler ${serverRequest.handler} not found!!!")

        return handler.handle(serverRequest.data)
    }
}
