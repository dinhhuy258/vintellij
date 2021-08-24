package com.dinhhuy258.vintellij.comrade.buffer

import com.dinhhuy258.vintellij.comrade.ComradeScope
import com.dinhhuy258.vintellij.comrade.core.FUN_BUFFER_REGISTER
import com.dinhhuy258.vintellij.neovim.Constants.Companion.FUN_NVIM_BUF_ATTACH
import com.dinhhuy258.vintellij.neovim.Constants.Companion.FUN_NVIM_BUF_GET_CHANGEDTICK
import com.dinhhuy258.vintellij.neovim.Constants.Companion.FUN_NVIM_BUF_SET_LINES
import com.dinhhuy258.vintellij.neovim.Constants.Companion.FUN_NVIM_CALL_FUNCTION
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import kotlinx.coroutines.launch

@Suppress("MemberVisibilityCanBePrivate")
class BufferOutOfSyncException(val syncBuffer: SyncBuffer, val nextTick: Int) :
        IllegalStateException() {
    override val message: String?
        get() = "Buffer: ${syncBuffer.id} '${syncBuffer.path}' is out of sync.\n" +
                "Current changedtick is ${syncBuffer.synchronizer.changedtick}, the next changedtick should be $nextTick."
}

private val log = Logger.getInstance(Synchronizer::class.java)

/**
 * Handle both side (JetBrain & Neovim) changes and try to make both side buffers synchronized.
 */
class Synchronizer(private val syncBuffer: SyncBuffer, var isEnableSync: Boolean) : DocumentListener {
    var changedtick = -1
        private set
    private val pendingChanges = mutableMapOf<Int, BufferChange>()
    private val nvimInstance = syncBuffer.nvimInstance

    private var changeBuilder: BufferChange.JetBrainsChangeBuilder? = null
    private var changedByNvim = false
    var exceptionHandler: ((Throwable) -> Unit)? = null

    fun onChange(change: BufferChange) {
//        if (!isEnableSync) return
//        ApplicationManager.getApplication().assertIsDispatchThread()
//        try {
//            when (change.source) {
//                BufferChange.Source.NEOVIM -> {
//                    changedByNvim = true
//                    try {
//                        onNeovimChange(change)
//                    } finally {
//                        changedByNvim = false
//                    }
//                }
//                BufferChange.Source.JetBrain -> onJetBrainChange(change)
//            }
//        } catch (t: Throwable) {
//            handleException(t)
//        }
    }

    fun initFromJetBrain() {
    }

    private fun checkNeovimChangedTick(change: BufferChange): Boolean {
        val pendingChange = pendingChanges.remove(change.tick)
        // The change is made by JetBrain.
        if (pendingChange != null) {
            return true
        }

        if (changedtick == -1) {
            changedtick = change.tick
            return change.lines == null
        }

        if (changedtick + 1 == change.tick) {
            changedtick++
            return change.lines == null
        }

        throw BufferOutOfSyncException(syncBuffer, change.tick)
    }

    private fun handleException(t: Throwable) {
        if (exceptionHandler != null) {
            exceptionHandler?.invoke(t)
        } else {
            log.error("Exception in Synchronizer", t)
        }
    }

    /**
     * Public for mock.
     */
    fun onJetBrainChange(change: BufferChange) {
    }

    private fun onNeovimChange(change: BufferChange) {
    }

    override fun beforeDocumentChange(event: DocumentEvent) {
        if (changedByNvim) return
        // Should not happen
        assert(changeBuilder == null)

        val builder = BufferChange.JetBrainsChangeBuilder(syncBuffer)
        builder.beforeChange(event)
        changeBuilder = builder
    }

    override fun documentChanged(event: DocumentEvent) {
        if (changedByNvim) return
        val builder = changeBuilder ?: return
        builder.afterChange(event)
        val change = builder.build()
        changedtick++
        pendingChanges[change.tick] = change
        onChange(change)
        changeBuilder = null
    }
}
