package com.dinhhuy258.vintellij.lsp

import com.intellij.openapi.application.ApplicationManager
import com.intellij.util.messages.Topic
import java.util.concurrent.ConcurrentHashMap

class BufferManager {
    companion object {
        val TOPIC = Topic(
            "SyncBuffer related events", SyncBufferManagerListener::class.java)
        private val publisher = ApplicationManager.getApplication().messageBus.syncPublisher(TOPIC)
    }

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

        publisher.bufferCreated(buffer)
    }


    fun releaseBuffer(path: String) {
        ApplicationManager.getApplication().assertIsDispatchThread()
        val buffer = findBufferByPath(path) ?: return

        bufferMap.remove(path) != null
        buffer.release()
        publisher.bufferReleased(buffer)
    }
}