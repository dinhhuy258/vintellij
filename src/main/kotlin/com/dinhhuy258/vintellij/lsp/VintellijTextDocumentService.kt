package com.dinhhuy258.vintellij.lsp

import com.dinhhuy258.vintellij.comrade.buffer.SyncBuffer
import com.dinhhuy258.vintellij.comrade.buffer.SyncBufferManager
import com.dinhhuy258.vintellij.comrade.buffer.SyncBufferManagerListener
import com.dinhhuy258.vintellij.lsp.completion.doCompletion
import com.dinhhuy258.vintellij.lsp.diagnostics.getHighlights
import com.dinhhuy258.vintellij.lsp.diagnostics.toDiagnostic
import com.dinhhuy258.vintellij.lsp.hover.getHoverDoc
import com.dinhhuy258.vintellij.lsp.navigation.goToDefinition
import com.dinhhuy258.vintellij.lsp.navigation.goToImplementation
import com.dinhhuy258.vintellij.lsp.navigation.goToReferences
import com.dinhhuy258.vintellij.lsp.navigation.goToTypeDefinition
import com.dinhhuy258.vintellij.lsp.quickfix.getImportCandidates
import com.dinhhuy258.vintellij.lsp.symbol.getDocumentSymbols
import com.dinhhuy258.vintellij.lsp.utils.AsyncExecutor
import com.dinhhuy258.vintellij.lsp.utils.Debouncer
import com.dinhhuy258.vintellij.utils.getURIForFile
import com.dinhhuy258.vintellij.utils.uriToPath
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.codeInsight.daemon.impl.HighlightInfo
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.project.Project
import com.intellij.util.messages.MessageBusConnection
import java.io.Closeable
import java.time.Duration
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
import org.eclipse.lsp4j.DocumentSymbol
import org.eclipse.lsp4j.DocumentSymbolParams
import org.eclipse.lsp4j.Hover
import org.eclipse.lsp4j.HoverParams
import org.eclipse.lsp4j.ImplementationParams
import org.eclipse.lsp4j.Location
import org.eclipse.lsp4j.LocationLink
import org.eclipse.lsp4j.Position
import org.eclipse.lsp4j.PublishDiagnosticsParams
import org.eclipse.lsp4j.ReferenceParams
import org.eclipse.lsp4j.SymbolInformation
import org.eclipse.lsp4j.TypeDefinitionParams
import org.eclipse.lsp4j.jsonrpc.messages.Either
import org.eclipse.lsp4j.services.LanguageClient
import org.eclipse.lsp4j.services.LanguageClientAware
import org.eclipse.lsp4j.services.TextDocumentService

class VintellijTextDocumentService(private val languageServer: VintellijLanguageServer) : TextDocumentService,
        LanguageClientAware,
        SyncBufferManagerListener,
        DaemonCodeAnalyzer.DaemonListener,
        Closeable {
    companion object {
        private const val DEBOUNCE_TIME = 200L
    }

    private lateinit var client: LanguageClient

    private val debounceLint = Debouncer(Duration.ofMillis(DEBOUNCE_TIME))

    private val async = AsyncExecutor()

    private var bufferToLint: SyncBuffer? = null

    private var project: Project? = null

    private var messageBusConnection: MessageBusConnection? = null

    override fun connect(client: LanguageClient) {
        this.client = client
    }

    override fun bufferCreated(syncBuffer: SyncBuffer) {
        if ((project != null && syncBuffer.project != project) ||
                (project == null && bufferToLint != null)) {
            return
        }

        if (bufferToLint != null) {
            // Clear diagnostics
            client.publishDiagnostics(PublishDiagnosticsParams(getURIForFile(bufferToLint!!.psiFile), emptyList()))
        }

        bufferToLint = syncBuffer
        lintNow()
    }

    override fun bufferReleased(syncBuffer: SyncBuffer) {
        // Clear diagnostics
        reportDiagnostics(syncBuffer, emptyList())

        if (bufferToLint == syncBuffer) {
            bufferToLint = null
        }
    }

    override fun didOpen(params: DidOpenTextDocumentParams) {
    }

    override fun daemonFinished(fileEditors: Collection<FileEditor>) {
        if (bufferToLint == null) {
            return
        }

        fileEditors.forEach { fileEditor ->
            if (fileEditor.file == bufferToLint!!.psiFile.virtualFile) {
                lintLater()
                return@forEach
            }
        }
    }

    override fun didChange(params: DidChangeTextDocumentParams) {
    }

    override fun didClose(params: DidCloseTextDocumentParams) {
    }

    override fun didSave(params: DidSaveTextDocumentParams) {
    }

    override fun definition(params: DefinitionParams): CompletableFuture<Either<List<Location>, List<LocationLink>>> =
            async.compute {
                val syncBuffer =
                        languageServer.getNvimInstance()?.bufManager?.findBufferByPath(uriToPath(params.textDocument.uri))

                Either.forLeft(tryCatch({
                    goToDefinition(syncBuffer, params.position)
                }, emptyList()))
            }

    override fun implementation(params: ImplementationParams): CompletableFuture<Either<List<Location>, List<LocationLink>>> =
            async.compute {
                val syncBuffer =
                        languageServer.getNvimInstance()?.bufManager?.findBufferByPath(uriToPath(params.textDocument.uri))

                Either.forLeft(tryCatch({
                    goToImplementation(syncBuffer, params.position)
                }, emptyList()))
            }

    override fun references(params: ReferenceParams): CompletableFuture<List<Location>> =
            async.compute {
                val syncBuffer =
                        languageServer.getNvimInstance()?.bufManager?.findBufferByPath(uriToPath(params.textDocument.uri))

                tryCatch({
                    goToReferences(syncBuffer, params.position)
                }, emptyList())
            }

    override fun typeDefinition(params: TypeDefinitionParams): CompletableFuture<Either<List<Location>, List<LocationLink>>> =
            async.compute {
                val syncBuffer =
                        languageServer.getNvimInstance()?.bufManager?.findBufferByPath(uriToPath(params.textDocument.uri))

                Either.forLeft(tryCatch({
                    goToTypeDefinition(syncBuffer, params.position)
                }, emptyList()))
            }

    override fun completion(position: CompletionParams): CompletableFuture<Either<List<CompletionItem>, CompletionList>> =
            async.compute {
                val syncBuffer =
                        languageServer.getNvimInstance()?.bufManager?.findBufferByPath(uriToPath(position.textDocument.uri))

                Either.forRight(
                        tryCatch({
                            doCompletion(syncBuffer, position.position)
                        }, CompletionList(false, emptyList()))

                )
            }

    override fun hover(params: HoverParams): CompletableFuture<Hover> = async.compute {
        val syncBuffer =
                languageServer.getNvimInstance()?.bufManager?.findBufferByPath(uriToPath(params.textDocument.uri))

        Hover(tryCatch({
            getHoverDoc(syncBuffer, params.position)
        }, emptyList()))
    }

    override fun codeAction(params: CodeActionParams): CompletableFuture<List<Either<Command, CodeAction>>> =
            async.compute {
                val syncBuffer =
                        languageServer.getNvimInstance()?.bufManager?.findBufferByPath(uriToPath(params.textDocument.uri))

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
                        commands.add(Either.forLeft(Command(commandTitle, "importFix", listOf(candidate))))
                    }
                }

                commands
            }

    override fun documentSymbol(params: DocumentSymbolParams): CompletableFuture<List<Either<SymbolInformation, DocumentSymbol>>> =
            async.compute {
                val syncBuffer =
                        languageServer.getNvimInstance()?.bufManager?.findBufferByPath(uriToPath(params.textDocument.uri))

                getDocumentSymbols(syncBuffer, params.textDocument.uri)
            }

    override fun close() {
        async.shutdown(true)
        messageBusConnection?.disconnect()
    }

    fun onProjectOpen(project: Project) {
        this.project = project
        messageBusConnection = project.messageBus.connect()
        messageBusConnection!!.subscribe(DaemonCodeAnalyzer.DAEMON_EVENT_TOPIC, this)
        messageBusConnection!!.subscribe(SyncBufferManager.TOPIC, this)
        if (bufferToLint != null && bufferToLint!!.project != project) {
            bufferToLint = null
        }
    }

    private inline fun <T> tryCatch(block: () -> T, fallback: T): T {
        return try {
            block()
        } catch (e: Throwable) {
            fallback
        }
    }

    private fun lintLater() {
        debounceLint.schedule(::doLint)
    }

    private fun lintNow() {
        debounceLint.submitImmediately(::doLint)
    }

    private fun doLint(cancelCallback: () -> Boolean) {
        val buffer = bufferToLint ?: return
        if (buffer.isReleased) {
            return
        }

        val highlights = tryCatch({
            getHighlights(buffer)
        }, emptyList())

        if (!cancelCallback.invoke()) {
            reportDiagnostics(buffer, highlights)
        }
    }

    private fun reportDiagnostics(buffer: SyncBuffer, highlightInfos: List<HighlightInfo>) {
        val errorRows = HashSet<Int>()
        val diagnostics = highlightInfos.map {
            it.toDiagnostic(buffer.document)
        }.filterNotNull().sortedBy {
            it.severity
        }.filter {
            if (it.severity == DiagnosticSeverity.Error) {
                errorRows.add(it.range.start.line)
                true
            } else {
                !errorRows.contains(it.range.start.line)
            }
        }

        client.publishDiagnostics(PublishDiagnosticsParams(getURIForFile(buffer.psiFile), diagnostics))
    }
}
