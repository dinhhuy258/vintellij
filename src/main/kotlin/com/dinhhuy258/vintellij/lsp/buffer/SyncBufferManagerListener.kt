package com.dinhhuy258.vintellij.lsp.buffer

import java.util.EventListener

interface SyncBufferManagerListener : EventListener {
    fun bufferCreated(syncBuffer: Buffer) {
    }

    fun bufferReleased(syncBuffer: Buffer) {
    }

    /**
     * Triggered when content of both sides (JetBrain and Nvim) get synced.
     */

    // TODO: Trigger when user is typing
    fun bufferSynced(syncBuffer: Buffer) {
    }
}