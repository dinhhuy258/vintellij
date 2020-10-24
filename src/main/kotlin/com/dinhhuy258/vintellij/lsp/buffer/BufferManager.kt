package com.dinhhuy258.vintellij.lsp.buffer

import com.dinhhuy258.vintellij.lsp.VintellijLanguageServer
import com.intellij.openapi.application.ApplicationManager
import java.util.concurrent.ConcurrentHashMap

class BufferManager(private val languageServer: VintellijLanguageServer) {
    private val bufferMap = ConcurrentHashMap<String, Buffer>()

    @Synchronized
    fun loadBuffer(path: String) {
        val project = languageServer.getProject() ?: return

        ApplicationManager.getApplication().invokeLater {
            if (bufferMap[path] == null) {
                bufferMap[path] = Buffer(path, project)
            } else {
                bufferMap[path]?.navigate()
            }
        }
    }

    @Synchronized
    fun releaseBuffer(path: String) {
        ApplicationManager.getApplication().invokeLater {
            val buffer = bufferMap[path]
            if (buffer != null) {
                buffer.release()
                bufferMap.remove(path)
            }
        }
    }

    @Synchronized
    fun getBuffer(path: String): Buffer? {
        return bufferMap[path]
    }
}
