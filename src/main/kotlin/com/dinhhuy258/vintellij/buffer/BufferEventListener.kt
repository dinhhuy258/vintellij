package com.dinhhuy258.vintellij.buffer

import com.intellij.util.messages.Topic
import java.util.EventListener

interface BufferEventListener : EventListener {
    companion object {
        val TOPIC = Topic("Buffer listener", BufferEventListener::class.java)
    }

    fun bufferCreated(buffer: Buffer) {
    }

    fun bufferReleased(buffer: Buffer) {
    }
}
