package com.dinhhuy258.vintellij

import com.dinhhuy258.vintellij.notifications.VintellijSyncBuffer
import com.google.gson.JsonPrimitive
import org.eclipse.lsp4j.DidChangeConfigurationParams
import org.eclipse.lsp4j.DidChangeWatchedFilesParams
import org.eclipse.lsp4j.ExecuteCommandParams
import org.eclipse.lsp4j.services.LanguageClient
import org.eclipse.lsp4j.services.LanguageClientAware
import org.eclipse.lsp4j.services.WorkspaceService
import java.util.concurrent.CompletableFuture

class VintellijWorkspaceService : WorkspaceService,
    LanguageClientAware {

    private lateinit var client: VintellijLanguageClient

    override fun didChangeConfiguration(params: DidChangeConfigurationParams) {
    }

    override fun didChangeWatchedFiles(params: DidChangeWatchedFilesParams) {
    }

    override fun executeCommand(params: ExecuteCommandParams): CompletableFuture<Any> {
        if (params.command == "importFix" && params.arguments.size == 2) {
            val firstArgument = params.arguments[0]
            val secondArgument = params.arguments[1]
            if (firstArgument is JsonPrimitive && firstArgument.isString &&
                secondArgument is JsonPrimitive && secondArgument.isString
            ) {
                val classToImport = firstArgument.asString
                val documentPath = secondArgument.asString

                client.syncBuffer(
                    VintellijSyncBuffer(documentPath, 1, 2, listOf(classToImport))
                )
            }
        }

        return CompletableFuture<Any>()
    }

    override fun connect(client: LanguageClient) {
        this.client = client as VintellijLanguageClient
    }
}
