package com.dinhhuy258.vintellij

import com.dinhhuy258.vintellij.buffer.BufferManager
import com.dinhhuy258.vintellij.utils.getProject
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletableFuture.completedFuture
import org.eclipse.lsp4j.CompletionOptions
import org.eclipse.lsp4j.ExecuteCommandOptions
import org.eclipse.lsp4j.InitializeParams
import org.eclipse.lsp4j.InitializeResult
import org.eclipse.lsp4j.ServerCapabilities
import org.eclipse.lsp4j.TextDocumentSyncKind
import org.eclipse.lsp4j.TextDocumentSyncOptions
import org.eclipse.lsp4j.jsonrpc.messages.Either
import org.eclipse.lsp4j.services.LanguageClient
import org.eclipse.lsp4j.services.LanguageClientAware
import org.eclipse.lsp4j.services.LanguageServer
import org.eclipse.lsp4j.services.TextDocumentService
import org.eclipse.lsp4j.services.WorkspaceService

class VintellijLanguageServer : LanguageServer, LanguageClientAware {
    private var client: LanguageClient? = null

    private var project: Project? = null

    private val bufferManager = BufferManager()

    private val workspaceService = VintellijWorkspaceService(this)

    private val textDocumentService = VintellijTextDocumentService(this)

    override fun initialize(params: InitializeParams): CompletableFuture<InitializeResult> {
        return CompletableFuture.supplyAsync {
            ApplicationManager.getApplication().invokeAndWait {
                project = getProject(params.rootUri)
                if (project == null) {
                    VintellijApplicationService.instance.showBalloon("Failed to load project: ${params.rootUri}", NotificationType.ERROR)
                    // TODO: Let LSP client know the connection is failed
                    return@invokeAndWait
                }
                textDocumentService.onProjectOpen(project!!)
            }

            InitializeResult(getServerCapabilities())
        }
    }

    override fun shutdown(): CompletableFuture<Any> {
        return completedFuture(null)
    }

    override fun exit() {
    }

    override fun getTextDocumentService(): TextDocumentService = textDocumentService

    override fun getWorkspaceService(): WorkspaceService = workspaceService

    override fun connect(client: LanguageClient) {
        textDocumentService.connect(client)
        this.client = client
    }

    private fun getServerCapabilities() = ServerCapabilities().apply {
        textDocumentSync = Either.forRight(TextDocumentSyncOptions().apply {
            openClose = true
            change = TextDocumentSyncKind.Incremental
            willSave = true
            save = null
        })
        hoverProvider = true
        completionProvider = CompletionOptions(false, listOf(".", "@"))
        signatureHelpProvider = null
        definitionProvider = true
        typeDefinitionProvider = Either.forLeft(true)
        implementationProvider = Either.forLeft(true)
        referencesProvider = true
        documentHighlightProvider = false
        documentSymbolProvider = true
        workspaceSymbolProvider = false
        codeActionProvider = Either.forLeft(true)
        codeLensProvider = null
        documentFormattingProvider = true
        documentRangeFormattingProvider = true
        documentOnTypeFormattingProvider = null
        renameProvider = Either.forLeft(false)
        documentLinkProvider = null
        executeCommandProvider = ExecuteCommandOptions()
        experimental = null
    }

    fun getBufferManager(): BufferManager {
        return bufferManager
    }
}
