package com.dinhhuy258.vintellij

import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.Executors
import kotlin.concurrent.thread
import org.eclipse.lsp4j.jsonrpc.Launcher
import org.eclipse.lsp4j.services.LanguageClient

class VintellijLanguageServerRunner {
    companion object {
        private const val DEFAULT_PORT = 6969
    }

    fun start() {
        thread(start = true) {
            val executorService = Executors.newCachedThreadPool()
            ServerSocket(DEFAULT_PORT, 0, InetAddress.getLoopbackAddress()).use { serverSocket ->
                while (true) {
                    val connection: Socket = serverSocket.accept()
                    executorService.execute {
                        createConnection(connection)
                    }
                }
            }
        }
    }

    private fun createConnection(connection: Socket) {
        val languageServer = VintellijLanguageServer()
        val launcher = Launcher.createLauncher(languageServer, VintellijLanguageClient::class.java, connection.getInputStream(), connection.getOutputStream())
        languageServer.connect(launcher.remoteProxy)
        launcher.startListening()
    }
}
