package com.dinhhuy258.vintellij.lsp

import com.dinhhuy258.vintellij.lsp.completion.doCompletion
import com.dinhhuy258.vintellij.lsp.navigation.goToDefinition
import com.dinhhuy258.vintellij.lsp.utils.AsyncExecutor
import com.dinhhuy258.vintellij.utils.uriToPath
import java.io.Closeable
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
import org.eclipse.lsp4j.jsonrpc.messages.Either
import org.eclipse.lsp4j.services.TextDocumentService

class VintellijTextDocumentService(private val languageServer: VintellijLanguageServer) : TextDocumentService, Closeable {
    private val async = AsyncExecutor()

    override fun didOpen(params: DidOpenTextDocumentParams) {
    }

    override fun didChange(params: DidChangeTextDocumentParams) {
    }

    override fun didClose(params: DidCloseTextDocumentParams) {
    }

    override fun didSave(params: DidSaveTextDocumentParams) {
    }

    override fun definition(params: DefinitionParams): CompletableFuture<Either<List<Location>, List<LocationLink>>> = async.compute {
        val syncBuffer = languageServer.getNvimInstance()?.bufManager?.findBufferByPath(uriToPath(params.textDocument.uri))

        Either.forLeft(goToDefinition(syncBuffer, params.position))
    }

    override fun completion(position: CompletionParams): CompletableFuture<Either<List<CompletionItem>, CompletionList>> = async.compute {
        val syncBuffer = languageServer.getNvimInstance()?.bufManager?.findBufferByPath(uriToPath(position.textDocument.uri))

        Either.forRight(doCompletion(syncBuffer, position.position))
    }

    override fun close() {
        async.shutdown(true)
    }
}
