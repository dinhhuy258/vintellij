package com.dinhhuy258.vintellij.commands

import com.dinhhuy258.vintellij.VintellijLanguageClient
import com.dinhhuy258.vintellij.buffer.BufferManager
import org.eclipse.lsp4j.ExecuteCommandParams

abstract class Command(
    protected val client: VintellijLanguageClient,
    protected val bufferManager: BufferManager
) {
    fun execute(params: ExecuteCommandParams) {
        if (isValidCommand(params)) {
            executeInternal(params)
        }
    }

    protected abstract fun executeInternal(params: ExecuteCommandParams)

    protected abstract fun isValidCommand(params: ExecuteCommandParams): Boolean
}
