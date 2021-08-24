package com.dinhhuy258.vintellij.lsp

import com.dinhhuy258.vintellij.comrade.buffer.SyncBufferManager
import com.dinhhuy258.vintellij.comrade.invokeOnMainAndWait
import com.dinhhuy258.vintellij.lsp.completion.doCompletion
import com.dinhhuy258.vintellij.lsp.completion.shouldStopCompletion
import com.dinhhuy258.vintellij.lsp.diagnostics.DiagnosticsProcessor
import com.dinhhuy258.vintellij.lsp.formatting.formatDocument
import com.dinhhuy258.vintellij.lsp.hover.getHoverDoc
import com.dinhhuy258.vintellij.lsp.navigation.goToDefinition
import com.dinhhuy258.vintellij.lsp.navigation.goToImplementation
import com.dinhhuy258.vintellij.lsp.navigation.goToReferences
import com.dinhhuy258.vintellij.lsp.navigation.goToTypeDefinition
import com.dinhhuy258.vintellij.lsp.quickfix.getImportCandidates
import com.dinhhuy258.vintellij.lsp.symbol.getDocumentSymbols
import com.dinhhuy258.vintellij.lsp.utils.AsyncExecutor
import com.dinhhuy258.vintellij.utils.uriToPath
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.openapi.editor.LogicalPosition
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.util.messages.MessageBusConnection
import java.io.Closeable
import java.util.concurrent.CompletableFuture
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
import org.eclipse.lsp4j.TextDocumentContentChangeEvent
import org.eclipse.lsp4j.TextEdit
import org.eclipse.lsp4j.TypeDefinitionParams
import org.eclipse.lsp4j.WillSaveTextDocumentParams
import org.eclipse.lsp4j.jsonrpc.messages.Either
import org.eclipse.lsp4j.services.LanguageClient
import org.eclipse.lsp4j.services.LanguageClientAware
import org.eclipse.lsp4j.services.TextDocumentService

class VintellijTextDocumentService(private val languageServer: VintellijLanguageServer) : TextDocumentService,
        LanguageClientAware,
        Closeable {
    private lateinit var client: LanguageClient

    private val async = AsyncExecutor()

    private val documentAsync = AsyncExecutor()

    private var project: Project? = null

    private var messageBusConnection: MessageBusConnection? = null

    override fun connect(client: LanguageClient) {
        this.client = client
    }

    override fun didOpen(params: DidOpenTextDocumentParams) {
        documentAsync.compute {
            invokeOnMainAndWait {
                languageServer.getBufferManager().loadBuffer(uriToPath(params.textDocument.uri))
            }
        }
    }

    override fun didChange(params: DidChangeTextDocumentParams) {
        val buffer =
            languageServer.getBufferManager().findBufferByPath(uriToPath(params.textDocument.uri)) ?: return

        val contentChanges = params.contentChanges
        if (contentChanges.isEmpty()) {
            return
        }

        documentAsync.compute {
            invokeOnMainAndWait {
                contentChanges.forEach { contentChange ->
                    val startPosition = contentChange.range.start
                    val endPosition = contentChange.range.end

                    if (startPosition.equals(endPosition)) {
                        buffer.insertText(startPosition, contentChange.text)
                    }
                    else {
                        buffer.replaceText(startPosition, endPosition, contentChange.text)
                    }
                }
            }
        }
    }

    override fun didClose(params: DidCloseTextDocumentParams) {
        documentAsync.compute {
            invokeOnMainAndWait {
                languageServer.getBufferManager().releaseBuffer(uriToPath(params.textDocument.uri))
            }
        }
    }

    override fun willSave(params: WillSaveTextDocumentParams) {
        documentAsync.compute {
            invokeOnMainAndWait {
                val syncedBuffer =
                    languageServer.getBufferManager().findBufferByPath(uriToPath(params.textDocument.uri))
                        ?: return@invokeOnMainAndWait

                syncedBuffer.psiFile.virtualFile.refresh(true, true)
                FileDocumentManager.getInstance().saveDocument(syncedBuffer.document)
            }
        }
    }

    override fun didSave(params: DidSaveTextDocumentParams) {
    }

    override fun definition(params: DefinitionParams): CompletableFuture<Either<List<Location>, List<LocationLink>>> =
            async.compute {
                val syncBuffer =
                        languageServer.getBufferManager().findBufferByPath(uriToPath(params.textDocument.uri))

                Either.forLeft(tryCatch({
                    goToDefinition(syncBuffer, params.position)
                }, emptyList()))
            }

    override fun implementation(params: ImplementationParams): CompletableFuture<Either<List<Location>, List<LocationLink>>> =
            async.compute {
                val syncBuffer =
                        languageServer.getBufferManager().findBufferByPath(uriToPath(params.textDocument.uri))

                Either.forLeft(tryCatch({
                    goToImplementation(syncBuffer, params.position)
                }, emptyList()))
            }

    override fun references(params: ReferenceParams): CompletableFuture<List<Location>> =
            async.compute {
                val syncBuffer =
                        languageServer.getBufferManager().findBufferByPath(uriToPath(params.textDocument.uri))

                tryCatch({
                    goToReferences(syncBuffer, params.position)
                }, emptyList())
            }

    override fun typeDefinition(params: TypeDefinitionParams): CompletableFuture<Either<List<Location>, List<LocationLink>>> =
            async.compute {
                val syncBuffer =
                        languageServer.getBufferManager().findBufferByPath(uriToPath(params.textDocument.uri))

                Either.forLeft(tryCatch({
                    goToTypeDefinition(syncBuffer, params.position)
                }, emptyList()))
            }

    override fun completion(position: CompletionParams): CompletableFuture<Either<List<CompletionItem>, CompletionList>> {
        // Stop in-progress suggestion
        shouldStopCompletion.set(true)
        return async.compute {
                val syncBuffer =
                        languageServer.getBufferManager().findBufferByPath(uriToPath(position.textDocument.uri))

                Either.forRight(
                        tryCatch({
                            doCompletion(syncBuffer, position.position)
                        }, CompletionList(false, emptyList()))

                )
            }
    }

    override fun hover(params: HoverParams): CompletableFuture<Hover> = async.compute {
        val syncBuffer =
                languageServer.getBufferManager().findBufferByPath(uriToPath(params.textDocument.uri))

        Hover(tryCatch({
            getHoverDoc(syncBuffer, params.position)
        }, emptyList()))
    }

    override fun formatting(params: DocumentFormattingParams): CompletableFuture<List<TextEdit>> = async.compute {
        val syncBuffer =
                languageServer.getBufferManager().findBufferByPath(uriToPath(params.textDocument.uri))

        try {
            formatDocument(syncBuffer, null)
        } catch (e: Throwable) {
        }

        emptyList()
    }

    override fun rangeFormatting(params: DocumentRangeFormattingParams): CompletableFuture<List<TextEdit>> = async.compute {
        val syncBuffer =
                languageServer.getBufferManager().findBufferByPath(uriToPath(params.textDocument.uri))

        try {
            formatDocument(syncBuffer, params.range)
        } catch (e: Throwable) {
        }

        emptyList()
    }

    override fun codeAction(params: CodeActionParams): CompletableFuture<List<Either<Command, CodeAction>>> =
            async.compute {
                val documentPath = uriToPath(params.textDocument.uri)
                val syncBuffer =
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
                        getImportCandidates(syncBuffer, position)
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
                val syncBuffer =
                        languageServer.getBufferManager().findBufferByPath(uriToPath(params.textDocument.uri))

                getDocumentSymbols(syncBuffer, params.textDocument.uri)
            }

    override fun close() {
        async.shutdown(true)
        messageBusConnection?.disconnect()
    }

    fun onProjectOpen(project: Project) {
        val diagnosticsProcessor = DiagnosticsProcessor()
        diagnosticsProcessor.start(project, client)
        this.project = project
        messageBusConnection = project.messageBus.connect()
        messageBusConnection!!.subscribe(DaemonCodeAnalyzer.DAEMON_EVENT_TOPIC, diagnosticsProcessor)
        messageBusConnection!!.subscribe(SyncBufferManager.TOPIC, diagnosticsProcessor)
    }

    private inline fun <T> tryCatch(block: () -> T, fallback: T): T {
        return try {
            block()
        } catch (e: Throwable) {
            fallback
        }
    }
}
