package com.dinhhuy258.vintellij.lsp

import com.dinhhuy258.vintellij.lsp.buffer.BufferManager
import com.dinhhuy258.vintellij.lsp.buffer.changeDocument
import com.dinhhuy258.vintellij.lsp.completion.doCompletion
import com.dinhhuy258.vintellij.lsp.navigation.goToDefinition
import com.dinhhuy258.vintellij.lsp.utils.AsyncExecutor
import com.dinhhuy258.vintellij.utils.uriToPath
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileDocumentManager
import java.util.concurrent.CompletableFuture
import org.eclipse.lsp4j.CompletionItem
import org.eclipse.lsp4j.CompletionList
import org.eclipse.lsp4j.CompletionParams
import org.eclipse.lsp4j.DefinitionParams
import org.eclipse.lsp4j.DidChangeTextDocumentParams
import org.eclipse.lsp4j.DidCloseTextDocumentParams
import org.eclipse.lsp4j.DidOpenTextDocumentParams
import org.eclipse.lsp4j.DidSaveTextDocumentParams
import org.eclipse.lsp4j.Location
import org.eclipse.lsp4j.LocationLink
import org.eclipse.lsp4j.WillSaveTextDocumentParams
import org.eclipse.lsp4j.jsonrpc.messages.Either
import org.eclipse.lsp4j.services.TextDocumentService

class VintellijTextDocumentService(private val languageServer: VintellijLanguageServer) : TextDocumentService {
    private val bufferManager: BufferManager = BufferManager(languageServer)

    private val async = AsyncExecutor()
    override fun didOpen(params: DidOpenTextDocumentParams) {
        bufferManager.loadBuffer(uriToPath(params.textDocument.uri))
    }

    override fun didChange(params: DidChangeTextDocumentParams) {
        val project = languageServer.getProject() ?: return
        val buffer = bufferManager.getBuffer(uriToPath(params.textDocument.uri)) ?: return

        changeDocument(project, buffer, params.contentChanges)
    }

    override fun didClose(params: DidCloseTextDocumentParams) {
        bufferManager.releaseBuffer(uriToPath(params.textDocument.uri))
    }

    override fun willSave(params: WillSaveTextDocumentParams) {
        val buffer = bufferManager.getBuffer(uriToPath(params.textDocument.uri)) ?: return
        val document = buffer.getDocument()

        val application = ApplicationManager.getApplication()
        application.invokeAndWait {
            application.runWriteAction {
                FileDocumentManager.getInstance().saveDocumentAsIs(document)
            }
        }
    }

    override fun didSave(params: DidSaveTextDocumentParams) {
    }

    override fun definition(params: DefinitionParams): CompletableFuture<Either<List<Location>, List<LocationLink>>> = async.compute {
        val buffer = bufferManager.getBuffer(uriToPath(params.textDocument.uri))

        Either.forLeft(goToDefinition(buffer, params.position))
    }

    override fun completion(position: CompletionParams): CompletableFuture<Either<List<CompletionItem>, CompletionList>> = async.compute {
        val buffer = bufferManager.getBuffer(uriToPath(position.textDocument.uri))
        Either.forRight(doCompletion(buffer, position.position))
    }
}
