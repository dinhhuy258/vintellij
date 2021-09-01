package com.dinhhuy258.vintellij

import com.dinhhuy258.vintellij.buffer.BufferManager
import com.dinhhuy258.vintellij.documents.generateDoc
import com.dinhhuy258.vintellij.notifications.VintellijSyncBuffer
import com.dinhhuy258.vintellij.utils.uriToPath
import com.google.gson.JsonPrimitive
import java.util.concurrent.CompletableFuture
import org.eclipse.lsp4j.DidChangeConfigurationParams
import org.eclipse.lsp4j.DidChangeWatchedFilesParams
import org.eclipse.lsp4j.ExecuteCommandParams
import org.eclipse.lsp4j.Position
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
        if (params.command == "importFix" && params.arguments.size == 2) {
            val firstArgument = params.arguments[0]
            val secondArgument = params.arguments[1]
            if (firstArgument is JsonPrimitive && firstArgument.isString &&
                secondArgument is JsonPrimitive && secondArgument.isString
            ) {
                val classToImport = firstArgument.asString
                val documentPath = secondArgument.asString

                client.syncBuffer(
                    VintellijSyncBuffer(documentPath, 1, 2, listOf(classToImport), false)
                )
            }
        } else if (params.command == "gen_doc" && params.arguments.size == 3) {
            val firstArgument = params.arguments[0]
            val secondArgument = params.arguments[1]
            val thirdArgument = params.arguments[2]

            if (firstArgument is JsonPrimitive && firstArgument.isString &&
                secondArgument is JsonPrimitive && secondArgument.isNumber &&
                thirdArgument is JsonPrimitive && thirdArgument.isNumber
            ) {
                val uri = firstArgument.asString
                val path = uriToPath(uri)
                val row = secondArgument.asInt
                val col = thirdArgument.asInt

                val doc = generateDoc(bufferManager.loadBuffer(path), Position(row - 1, col))
                if (doc != null) {
                    val lines = doc.split("\n").filter { line ->
                        line != ""
                    }.mapIndexed { idx: Int, line: String ->
                        if (idx == 0) {
                            line
                        } else {
                            " $line"
                        }
                    }
                    client.syncBuffer(
                        VintellijSyncBuffer(path, row - 1, row + lines.size, lines, false)
                    )
                }
            }
        }

        return CompletableFuture<Any>()
    }

    override fun connect(client: LanguageClient) {
        this.client = client as VintellijLanguageClient
    }

    fun setBufferManager(bufferManager: BufferManager) {
        this.bufferManager = bufferManager
    }
}
