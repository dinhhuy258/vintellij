package com.dinhhuy258.vintellij

import com.dinhhuy258.vintellij.buffer.BufferManager
import com.dinhhuy258.vintellij.buffer.BufferSynchronization
import com.dinhhuy258.vintellij.listeners.VintellijWindowFocusListener
import com.dinhhuy258.vintellij.notifications.VintellijEventType
import com.dinhhuy258.vintellij.notifications.VintellijNotification
import com.dinhhuy258.vintellij.utils.invokeAndWait
import com.dinhhuy258.vintellij.utils.normalizeUri
import com.dinhhuy258.vintellij.utils.uriToPath
import com.intellij.notification.NotificationDisplayType
import com.intellij.notification.NotificationGroup
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ex.ProjectManagerEx
import com.intellij.openapi.util.Ref
import com.intellij.openapi.wm.WindowManager
import java.io.File
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletableFuture.completedFuture
import org.eclipse.lsp4j.CompletionOptions
import org.eclipse.lsp4j.ExecuteCommandOptions
import org.eclipse.lsp4j.InitializeParams
import org.eclipse.lsp4j.InitializeResult
import org.eclipse.lsp4j.MessageParams
import org.eclipse.lsp4j.MessageType
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
    private val VINTELLIJ_NOTIFICATION_GROUP =
        NotificationGroup("Vintellij", NotificationDisplayType.BALLOON, true)

    private var project: Project? = null

    private lateinit var bufferManager: BufferManager

    private lateinit var bufferSynchronization: BufferSynchronization

    private lateinit var client: VintellijLanguageClient

    private val workspaceService: VintellijWorkspaceService = VintellijWorkspaceService()

    private val textDocumentService = VintellijTextDocumentService(this)

    override fun initialize(params: InitializeParams): CompletableFuture<InitializeResult> {
        return CompletableFuture.supplyAsync {
            invokeAndWait {
                project = getProject(params.rootUri)
                if (project == null) {
                    VINTELLIJ_NOTIFICATION_GROUP
                        .createNotification("Failed to open project: ${params.rootUri}", NotificationType.ERROR)
                        .notify(null)

                    client.showMessage(
                        MessageParams(
                            MessageType.Error,
                            "Failed to open project: ${params.rootUri}"
                        )
                    )

                    client.sendNotification(VintellijNotification(VintellijEventType.CLOSE_CONNECTION))
                    return@invokeAndWait
                }
                bufferSynchronization = BufferSynchronization(client)
                bufferManager = BufferManager(project!!, bufferSynchronization::onDocumentChanged)
                workspaceService.setBufferManager(bufferManager)
                textDocumentService.onProjectOpen(project!!)
                project!!.putUserData(VINTELLIJ_CLIENT, client)
                WindowManager.getInstance().getFrame(project)?.let { frame ->
                    // Don't want to add duplicate listeners
                    frame.removeWindowFocusListener(VintellijWindowFocusListener)
                    frame.addWindowFocusListener(VintellijWindowFocusListener)
                }

                VINTELLIJ_NOTIFICATION_GROUP
                    .createNotification("Connected to LSP client: ${project!!.name}", NotificationType.INFORMATION)
                    .notify(project)
                client.showMessage(
                    MessageParams(
                        MessageType.Info,
                        "LSP connected: ${project!!.name}"
                    )
                )
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
        workspaceService.connect(client)
        this.client = client as VintellijLanguageClient
    }

    fun getBufferManager(): BufferManager {
        return bufferManager
    }

    fun getBufferSynchronization(): BufferSynchronization {
        return bufferSynchronization
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

    private fun getProject(projectUri: String): Project? {
        val newUri = normalizeUri(projectUri)
        val directory = File(uriToPath(newUri))
        if (!directory.isDirectory) {
            return null
        }

        val projectPath = directory.absolutePath
        if (!File(projectPath).exists()) {
            return null
        }

        val projectManager = ProjectManagerEx.getInstanceEx()
        val projectRef = Ref<Project>()
        ApplicationManager.getApplication().runWriteAction {
            try {
                val isProjectOpened = projectManager.openProjects.find {
                    uriToPath(it.baseDir.path).equals(projectPath.replace("\\", "/"), true)
                }

                val project = isProjectOpened ?: projectManager.loadAndOpenProject(projectPath)
                projectRef.set(project)
            } catch (e: Throwable) {
                projectRef.set(null)
            }
        }

        val project = projectRef.get()
        if (project == null || project.isDisposed) {
            return null
        }

        while (!project.isInitialized) {
            Thread.sleep(1000)
        }
        return project
    }
}
