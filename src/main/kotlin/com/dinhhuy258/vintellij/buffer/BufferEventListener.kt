package com.dinhhuy258.vintellij.buffer

import java.util.EventListener

interface BufferEventListener : EventListener {
    fun bufferCreated(buffer: Buffer) {
    }

    fun bufferReleased(buffer: Buffer) {
    }

    //TODO: Remove?
    fun bufferSynced(buffer: Buffer) {
    }
}