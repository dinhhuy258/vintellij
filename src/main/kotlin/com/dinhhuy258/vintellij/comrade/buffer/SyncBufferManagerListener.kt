package com.dinhhuy258.vintellij.comrade.buffer

import java.util.EventListener

interface SyncBufferManagerListener : EventListener {
    fun bufferCreated(syncBuffer: SyncBuffer) {
    }

    fun bufferReleased(syncBuffer: SyncBuffer) {
    }

    /**
     * Triggered when content of both sides (JetBrain and Nvim) get synced.
     */
    fun bufferSynced(syncBuffer: SyncBuffer) {
    }
}
