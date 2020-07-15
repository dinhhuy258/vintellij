package com.dinhhuy258.plugins.connections

import com.dinhhuy258.plugins.exceptions.HandlerNotFoundException
import com.dinhhuy258.plugins.handlers.EchoHandler
import com.dinhhuy258.plugins.handlers.VIHandler
import com.google.gson.Gson
import io.ktor.application.call
import io.ktor.http.ContentType
import io.ktor.request.receiveText
import io.ktor.response.respondText
import io.ktor.routing.post
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty

class VIServer(private val port: Int) {
    private val gson: Gson = Gson()

    private val handlers: Map<String, VIHandler>

    init {
        handlers = mapOf("echo" to EchoHandler())
    }

    fun start() {
        embeddedServer(Netty, port) {
            routing {
                post("/") {
                    val body = call.receiveText()
                    val request = gson.fromJson(body, ServerRequest::class.java)

                    call.respondText(processRequest(request), ContentType.Application.Json)
                }
            }
        }.start(wait = false)
    }

    private fun processRequest(serverRequest: ServerRequest): String {
        val handler = handlers[serverRequest.handler] ?: throw HandlerNotFoundException("Handler ${serverRequest.handler} not found!!!")

        return handler.handle(serverRequest.data)
    }
}
