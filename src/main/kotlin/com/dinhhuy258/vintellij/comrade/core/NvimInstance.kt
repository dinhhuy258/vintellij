package com.dinhhuy258.vintellij.comrade.core

import com.dinhhuy258.vintellij.comrade.buffer.SyncBufferManager
import com.dinhhuy258.vintellij.comrade.parseIPV4String
import com.dinhhuy258.vintellij.neovim.ApiInfo
import com.dinhhuy258.vintellij.neovim.NeovimConnection
import com.dinhhuy258.vintellij.neovim.SocketConnection
import com.intellij.openapi.Disposable
import com.intellij.openapi.diagnostic.Logger
import java.io.File
import java.net.Socket
import org.scalasbt.ipcsocket.UnixDomainSocket
import org.scalasbt.ipcsocket.Win32NamedPipeSocketPatched

private val Log = Logger.getInstance(NvimInstance::class.java)

class NvimInstance(private val address: String, onClose: (Throwable?) -> Unit) : Disposable {
    companion object {
        private val VERSION: Map<String, String> = mapOf(
                "major" to "1",
                "minor" to "0",
                "patch" to "0",
                "prerelease" to ""
        )
    }

    private val log = Logger.getInstance(NvimInstance::class.java)
    lateinit var apiInfo: ApiInfo
    val bufManager = SyncBufferManager(this)
    @Volatile var connected = false
        private set

    var isSyncBuffer: Boolean? = null

    override fun toString(): String {
        return address
    }

    override fun dispose() {
        TODO("Not yet implemented")
    }

    fun updateSyncBuffer(isEnable: Boolean) {
        isSyncBuffer = isEnable
        bufManager.synchronizer.isEnableSync = isSyncBuffer!!
    }
}

private fun createRPCConnection(address: String): NeovimConnection {
    Log.info("Creating RPC connection from '$address'")

    val ipInfo = parseIPV4String(address)
    if (ipInfo != null)
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
