package com.dinhhuy258.vintellij

import com.dinhhuy258.vintellij.buffer.BufferManager
import com.dinhhuy258.vintellij.commands.CommandFactory
import java.util.concurrent.CompletableFuture
import org.eclipse.lsp4j.DidChangeConfigurationParams
import org.eclipse.lsp4j.DidChangeWatchedFilesParams
import org.eclipse.lsp4j.ExecuteCommandParams
import org.eclipse.lsp4j.services.LanguageClient
import org.eclipse.lsp4j.services.LanguageClientAware
import org.eclipse.lsp4j.services.WorkspaceService

class VintellijWorkspaceService : WorkspaceService,
    LanguageClientAware {

    private lateinit var client: VintellijLanguageClient

    private lateinit var bufferManager: BufferManager

    override fun didChangeConfiguration(params: DidChangeConfigurationParams) {
    }

    override fun didChangeWatchedFiles(params: DidChangeWatchedFilesParams) {
    }

    override fun executeCommand(params: ExecuteCommandParams): CompletableFuture<Any> {
        val command = CommandFactory.getInstance().createCommand(params.command, client, bufferManager)
        command.execute(params)

        return CompletableFuture.completedFuture(null)
    }

    override fun connect(client: LanguageClient) {
        this.client = client as VintellijLanguageClient
    }

    fun setBufferManager(bufferManager: BufferManager) {
        this.bufferManager = bufferManager
    }
}
