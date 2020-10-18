package com.dinhhuy258.vintellij.comrade.core

import com.dinhhuy258.vintellij.VintellijManager
import com.intellij.openapi.Disposable
import com.intellij.openapi.diagnostic.Logger
import com.dinhhuy258.vintellij.comrade.Version
import com.dinhhuy258.vintellij.comrade.buffer.SyncBufferManager
import com.dinhhuy258.vintellij.comrade.completion.CompletionManager
import com.dinhhuy258.vintellij.comrade.insight.InsightProcessor
import com.dinhhuy258.vintellij.comrade.parseIPV4String
import com.dinhhuy258.vintellij.neovim.ApiInfo
import com.dinhhuy258.vintellij.neovim.Client
import com.dinhhuy258.vintellij.neovim.NeovimConnection
import com.dinhhuy258.vintellij.neovim.SocketConnection
import org.scalasbt.ipcsocket.UnixDomainSocket
import org.scalasbt.ipcsocket.Win32NamedPipeSocketPatched
import java.io.File
import java.net.Socket

private val Log = Logger.getInstance(NvimInstance::class.java)

class NvimInstance(private val address: String, onClose: (Throwable?) -> Unit) : Disposable {

    private val log = Logger.getInstance(NvimInstance::class.java)
    private val connection = createRPCConnection(address)
    val client = Client(connection, onClose)
    lateinit var apiInfo:ApiInfo
    val bufManager = SyncBufferManager(this)
    @Volatile var connected = false
        private set

    suspend fun connect() {
        apiInfo = client.api.getApiInfo()

        client.api.setClientInfo("ComradeNeovim", Version.toMap())
        client.api.command("echom \"ComradeNeovim connected. ID: ${apiInfo.channelId}\"")

        client.registerHandler(bufManager)
        client.registerHandler(CompletionManager(bufManager))
        client.registerHandler(InsightProcessor)
        client.registerHandler(VintellijManager(this))
        log.info("NvimInstance has been created for connection '$connection'")
        connected = true
    }

    override fun dispose() {
        connected = false
        connection.close()
    }

    override fun toString(): String {
        return address
    }
}

private fun createRPCConnection(address: String): NeovimConnection {
    Log.info("Creating RPC connection from '$address'")

    val ipInfo = parseIPV4String(address)
    if (ipInfo!= null)
        return SocketConnection(Socket(ipInfo.first, ipInfo.second))
    else {
        val file = File(address)
        if (file.exists())
            return SocketConnection(
                    if (isWindows()) Win32NamedPipeSocketPatched(address)
                    else UnixDomainSocket(address))
    }
    throw IllegalArgumentException("Cannot create RPC connection from given address: '$address'.")
}

private fun isWindows(): Boolean {
    val osStr = System.getProperty("os.name").toLowerCase()
    return osStr.contains("win")
}