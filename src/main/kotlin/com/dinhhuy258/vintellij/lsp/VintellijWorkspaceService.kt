package com.dinhhuy258.vintellij.lsp

import com.dinhhuy258.vintellij.comrade.ComradeScope
import com.dinhhuy258.vintellij.comrade.core.FUN_ADD_IMPORT
import com.google.gson.JsonPrimitive
import java.util.concurrent.CompletableFuture
import kotlinx.coroutines.launch
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
        val nvimInstance = languageServer.getNvimInstance()

        if (params.command == "importFix" && nvimInstance != null && params.arguments.isNotEmpty()) {
            val firstArgument = params.arguments[0]
            if (firstArgument is JsonPrimitive && firstArgument.isString) {
                val classToImport = firstArgument.asString
                ComradeScope.launch {
                    nvimInstance.client.api.callFunction(FUN_ADD_IMPORT, listOf(classToImport))
                }
            }
        }

        return CompletableFuture<Any>()
    }
}
