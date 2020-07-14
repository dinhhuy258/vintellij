package com.dinhhuy258.plugins.connections

import com.jetbrains.rd.util.currentThreadName
import io.ktor.application.call
import io.ktor.http.ContentType
import io.ktor.response.respondText
import io.ktor.routing.get
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty

class VIServer(private val port: Int) {
    fun start() {
        embeddedServer(Netty, port) {
            routing {
                get("/") {
                    call.respondText("Hello world!!!", ContentType.Text.Html)
                }
            }
        }.start(wait = false)
    }
}
