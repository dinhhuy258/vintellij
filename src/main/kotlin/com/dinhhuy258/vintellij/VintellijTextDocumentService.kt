package com.dinhhuy258.vintellij

import com.dinhhuy258.vintellij.buffer.BufferEventListener
import com.dinhhuy258.vintellij.completion.doCompletion
import com.dinhhuy258.vintellij.completion.stopCompletion
import com.dinhhuy258.vintellij.diagnostics.DiagnosticsProcessor
import com.dinhhuy258.vintellij.formatting.formatDocument
import com.dinhhuy258.vintellij.hover.getHoverDoc
import com.dinhhuy258.vintellij.navigation.goToDefinition
import com.dinhhuy258.vintellij.navigation.goToImplementation
import com.dinhhuy258.vintellij.navigation.goToReferences
import com.dinhhuy258.vintellij.navigation.goToTypeDefinition
import com.dinhhuy258.vintellij.notifications.VintellijNotification
import com.dinhhuy258.vintellij.notifications.VintellijEventType
import com.dinhhuy258.vintellij.quickfix.getImportCandidates
import com.dinhhuy258.vintellij.symbol.getDocumentSymbols
import com.dinhhuy258.vintellij.utils.AsyncExecutor
import com.dinhhuy258.vintellij.utils.invokeAndWait
import com.dinhhuy258.vintellij.utils.runWriteAction
import com.dinhhuy258.vintellij.utils.uriToPath
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.util.messages.MessageBusConnection
import org.eclipse.lsp4j.CodeAction
import org.eclipse.lsp4j.CodeActionParams
import org.eclipse.lsp4j.Command
import org.eclipse.lsp4j.CompletionItem
import org.eclipse.lsp4j.CompletionList
import org.eclipse.lsp4j.CompletionParams
import org.eclipse.lsp4j.DefinitionParams
import org.eclipse.lsp4j.DiagnosticSeverity
import org.eclipse.lsp4j.DidChangeTextDocumentParams
import org.eclipse.lsp4j.DidCloseTextDocumentParams
import org.eclipse.lsp4j.DidOpenTextDocumentParams
import org.eclipse.lsp4j.DidSaveTextDocumentParams
import org.eclipse.lsp4j.DocumentFormattingParams
import org.eclipse.lsp4j.DocumentRangeFormattingParams
import org.eclipse.lsp4j.DocumentSymbol
import org.eclipse.lsp4j.DocumentSymbolParams
import org.eclipse.lsp4j.Hover
import org.eclipse.lsp4j.HoverParams
import org.eclipse.lsp4j.ImplementationParams
import org.eclipse.lsp4j.Location
import org.eclipse.lsp4j.LocationLink
import org.eclipse.lsp4j.Position
import org.eclipse.lsp4j.ReferenceParams
import org.eclipse.lsp4j.SymbolInformation
import org.eclipse.lsp4j.TextEdit
import org.eclipse.lsp4j.TypeDefinitionParams
import org.eclipse.lsp4j.WillSaveTextDocumentParams
import org.eclipse.lsp4j.jsonrpc.messages.Either
import org.eclipse.lsp4j.services.LanguageClient
import org.eclipse.lsp4j.services.LanguageClientAware
import org.eclipse.lsp4j.services.TextDocumentService
import java.io.Closeable
import java.util.concurrent.CompletableFuture

class VintellijTextDocumentService(private val languageServer: VintellijLanguageServer) : TextDocumentService,
    LanguageClientAware,
    Closeable {
    companion object {
        private val publisher = ApplicationManager.getApplication().messageBus.syncPublisher(BufferEventListener.TOPIC)
    }

    private lateinit var client: VintellijLanguageClient

    private val async = AsyncExecutor()

    private val documentAsync = AsyncExecutor()

    private val diagnosticsProcessor = DiagnosticsProcessor()

    private var project: Project? = null

    private var messageBusConnection: MessageBusConnection? = null

    override fun connect(client: LanguageClient) {
        this.client = client as VintellijLanguageClient
    }

    override fun completion(params: CompletionParams): CompletableFuture<Either<List<CompletionItem>, CompletionList>> {
        // Stop in-progress suggestion
        stopCompletion()

        return documentAsync.compute {
            val buffer =
                languageServer.getBufferManager().findBufferByPath(uriToPath(params.textDocument.uri))

            Either.forRight(
                tryCatch({
                    doCompletion(buffer, params.position)
                }, CompletionList(false, emptyList()))
            )
        }
    }

    override fun didChange(params: DidChangeTextDocumentParams) {
        val buffer =
            languageServer.getBufferManager().findBufferByPath(uriToPath(params.textDocument.uri)) ?: return

        val contentChanges = params.contentChanges
        if (contentChanges.isEmpty()) {
            return
        }

        // Stop in-progress suggestion
        stopCompletion()

        documentAsync.compute {
            // Invoke this one here to avoid requesting to the event dispatch thread many times
            invokeAndWait {
                runWriteAction {
                    contentChanges.forEach { contentChange ->
                        val startPosition = contentChange.range.start
                        val endPosition = contentChange.range.end

                        if (startPosition.equals(endPosition)) {
                            buffer.insertText(startPosition, contentChange.text)
                        } else {
                            buffer.replaceText(startPosition, endPosition, contentChange.text)
                        }
                    }
                }
            }
        }
    }

    override fun didOpen(params: DidOpenTextDocumentParams) {
        val path = uriToPath(params.textDocument.uri)

        async.compute {
            invokeAndWait {
                val buffer = languageServer.getBufferManager().loadBuffer(path)
                if (buffer != null) {
                    publisher.bufferCreated(buffer)
                } else {
                    // Synchronize project in the case buffer not found
                    languageServer.getBufferSynchronization().performSync(project!!) {
                        val loadedBuffer = languageServer.getBufferManager().loadBuffer(path)
                        if (loadedBuffer != null) {
                            publisher.bufferCreated(loadedBuffer)
                        }
                    }
                }
            }
        }
    }

    override fun didClose(params: DidCloseTextDocumentParams) {
        async.compute {
            invokeAndWait {
                val buffer = languageServer.getBufferManager().releaseBuffer(uriToPath(params.textDocument.uri))
                if (buffer != null) {
                    publisher.bufferReleased(buffer)
                }
            }
        }
    }

    override fun willSave(params: WillSaveTextDocumentParams) {
        async.compute {
            invokeAndWait {
                val syncedBuffer =
                    languageServer.getBufferManager().findBufferByPath(uriToPath(params.textDocument.uri))
                        ?: return@invokeAndWait

                syncedBuffer.psiFile.virtualFile.refresh(true, true)
                FileDocumentManager.getInstance().saveDocument(syncedBuffer.document)
            }
        }
    }

    override fun didSave(params: DidSaveTextDocumentParams) {
    }

    override fun definition(params: DefinitionParams): CompletableFuture<Either<List<Location>, List<LocationLink>>> =
        async.compute {
            val buffer =
                languageServer.getBufferManager().findBufferByPath(uriToPath(params.textDocument.uri))

            Either.forLeft(tryCatch({
                goToDefinition(buffer, params.position)
            }, emptyList()))
        }

    override fun implementation(params: ImplementationParams): CompletableFuture<Either<List<Location>, List<LocationLink>>> =
        async.compute {
            val buffer =
                languageServer.getBufferManager().findBufferByPath(uriToPath(params.textDocument.uri))

            Either.forLeft(tryCatch({
                goToImplementation(buffer, params.position)
            }, emptyList()))
        }

    override fun references(params: ReferenceParams): CompletableFuture<List<Location>> =
        async.compute {
            val buffer =
                languageServer.getBufferManager().findBufferByPath(uriToPath(params.textDocument.uri))

            tryCatch({
                goToReferences(buffer, params.position)
            }, emptyList())
        }

    override fun typeDefinition(params: TypeDefinitionParams): CompletableFuture<Either<List<Location>, List<LocationLink>>> =
        async.compute {
            val buffer =
                languageServer.getBufferManager().findBufferByPath(uriToPath(params.textDocument.uri))

            Either.forLeft(tryCatch({
                goToTypeDefinition(buffer, params.position)
            }, emptyList()))
        }

    override fun hover(params: HoverParams): CompletableFuture<Hover> = async.compute {
        val buffer =
            languageServer.getBufferManager().findBufferByPath(uriToPath(params.textDocument.uri))

        Hover(tryCatch({
            getHoverDoc(buffer, params.position)
        }, emptyList()))
    }

    override fun formatting(params: DocumentFormattingParams): CompletableFuture<List<TextEdit>> = async.compute {
        val buffer =
            languageServer.getBufferManager().findBufferByPath(uriToPath(params.textDocument.uri))

        formatDocument(buffer, null)
        client.sendNotification(VintellijNotification(VintellijEventType.BUFFER_SAVED))

        emptyList()
    }

    override fun rangeFormatting(params: DocumentRangeFormattingParams): CompletableFuture<List<TextEdit>> =
        async.compute {
            val buffer =
                languageServer.getBufferManager().findBufferByPath(uriToPath(params.textDocument.uri))

            formatDocument(buffer, params.range)
            client.sendNotification(VintellijNotification(VintellijEventType.BUFFER_SAVED))

            emptyList()
        }

    override fun codeAction(params: CodeActionParams): CompletableFuture<List<Either<Command, CodeAction>>> =
        async.compute {
            val documentPath = uriToPath(params.textDocument.uri)
            val buffer =
                languageServer.getBufferManager().findBufferByPath(documentPath)

            val importDiagnostics = params.context.diagnostics.filter {
                it.severity == DiagnosticSeverity.Error && it.code != null
            }.filterNotNull()

            val commands = ArrayList<Either<Command, CodeAction>>()
            importDiagnostics.forEach { diagnostic ->
                val startPosition = diagnostic.range.start
                val endPosition = diagnostic.range.end
                val position = Position(startPosition.line, (endPosition.character + startPosition.character) / 2)
                val importCandidates = tryCatch({
                    getImportCandidates(buffer, position)
                }, emptyList())
                importCandidates.forEach { candidate ->
                    val commandTitle = "Import " + candidate.removePrefix("import").removeSuffix(";")
                    commands.add(Either.forLeft(Command(commandTitle, "importFix", listOf(candidate, documentPath))))
                }
            }

            commands
        }

    override fun documentSymbol(params: DocumentSymbolParams): CompletableFuture<List<Either<SymbolInformation, DocumentSymbol>>> =
        async.compute {
            val buffer =
                languageServer.getBufferManager().findBufferByPath(uriToPath(params.textDocument.uri))

            getDocumentSymbols(buffer, params.textDocument.uri)
        }

    override fun close() {
        async.shutdown(true)
        documentAsync.shutdown(true)
        messageBusConnection?.disconnect()
        diagnosticsProcessor.stop()
    }

    fun onProjectOpen(project: Project) {
        diagnosticsProcessor.start(project, client)
        this.project = project
        messageBusConnection = project.messageBus.connect()
        messageBusConnection!!.subscribe(DaemonCodeAnalyzer.DAEMON_EVENT_TOPIC, diagnosticsProcessor)
        messageBusConnection!!.subscribe(BufferEventListener.TOPIC, diagnosticsProcessor)
    }

    private inline fun <T> tryCatch(block: () -> T, fallback: T): T {
        return try {
            block()
        } catch (e: Throwable) {
            fallback
        }
    }
}
