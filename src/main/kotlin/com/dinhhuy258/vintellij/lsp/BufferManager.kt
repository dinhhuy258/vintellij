package com.dinhhuy258.vintellij.lsp

import com.intellij.openapi.application.ApplicationManager
import java.util.concurrent.ConcurrentHashMap

class BufferManager {
    private val bufferMap = ConcurrentHashMap<String, Buffer>()

    fun findBufferByPath(path: String): Buffer? {
        return bufferMap[path]
    }

    fun loadBuffer(path: String) {
        var buffer = findBufferByPath(path)
        if (buffer == null) {
            try {
                buffer = Buffer(path)
            } catch (e: BufferNotInProjectException) {
                return
            }
            bufferMap[buffer.path] = buffer
        }

        buffer.navigate()
    }


    fun releaseBuffer(path: String) {
        ApplicationManager.getApplication().assertIsDispatchThread()
        val buffer = findBufferByPath(path) ?: return

        bufferMap.remove(path) != null
        buffer.release()
    }
}