package com.dinhhuy258.vintellij.buffer

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import java.util.concurrent.ConcurrentHashMap

class BufferManager(private val project: Project) {
    private val bufferMap = ConcurrentHashMap<String, Buffer>()

    fun findBufferByPath(path: String): Buffer? {
        return bufferMap[path]
    }

    fun loadBuffer(path: String): Buffer? {
        var buffer = findBufferByPath(path)
        if (buffer == null) {
            try {
                buffer = Buffer(project, path)
            } catch (e: BufferNotInProjectException) {
                return null
            }
            bufferMap[buffer.path] = buffer
        }

        buffer.navigate()

        return buffer
    }

    fun releaseBuffer(path: String): Buffer? {
        ApplicationManager.getApplication().assertIsDispatchThread()
        val buffer = findBufferByPath(path) ?: return null

        bufferMap.remove(path) != null
        buffer.release()

        return buffer
    }
}
