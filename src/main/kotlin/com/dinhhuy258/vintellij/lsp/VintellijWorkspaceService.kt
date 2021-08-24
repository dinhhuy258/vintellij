package com.dinhhuy258.vintellij.lsp

import com.google.gson.JsonPrimitive
import java.util.concurrent.CompletableFuture
import org.eclipse.lsp4j.DidChangeConfigurationParams
import org.eclipse.lsp4j.DidChangeWatchedFilesParams
import org.eclipse.lsp4j.ExecuteCommandParams
import org.eclipse.lsp4j.services.WorkspaceService

class VintellijWorkspaceService(private val languageServer: VintellijLanguageServer) : WorkspaceService {

    override fun didChangeConfiguration(params: DidChangeConfigurationParams) {
    }

    override fun didChangeWatchedFiles(params: DidChangeWatchedFilesParams) {
    }

    override fun executeCommand(params: ExecuteCommandParams): CompletableFuture<Any> {
        if (params.command == "importFix" && params.arguments.size == 2) {
            val firstArgument = params.arguments[0]
            val secondArgument = params.arguments[1]
            if (firstArgument is JsonPrimitive && firstArgument.isString &&
                    secondArgument is JsonPrimitive && secondArgument.isString) {
                val classToImport = firstArgument.asString
                val documentPath = secondArgument.asString

                languageServer.getBufferManager().findBufferByPath(documentPath)?.insertText(
                        "\n$classToImport", 1)
            }
        }

        return CompletableFuture<Any>()
    }
}
