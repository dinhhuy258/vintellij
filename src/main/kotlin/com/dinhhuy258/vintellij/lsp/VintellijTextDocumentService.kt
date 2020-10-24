package com.dinhhuy258.vintellij.lsp

import com.dinhhuy258.vintellij.lsp.buffer.BufferManager
import com.dinhhuy258.vintellij.lsp.utils.AsyncExecutor
import com.dinhhuy258.vintellij.lsp.utils.toTextRange
import com.dinhhuy258.vintellij.utils.PathUtils
import com.dinhhuy258.vintellij.utils.uriToPath
import com.intellij.codeInsight.navigation.GotoImplementationHandler
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.command.UndoConfirmationPolicy
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.psi.PsiDocumentManager
import org.eclipse.lsp4j.DefinitionParams
import org.eclipse.lsp4j.DidChangeTextDocumentParams
import org.eclipse.lsp4j.DidCloseTextDocumentParams
import org.eclipse.lsp4j.DidOpenTextDocumentParams
import org.eclipse.lsp4j.DidSaveTextDocumentParams
import org.eclipse.lsp4j.Location
import org.eclipse.lsp4j.LocationLink
import org.eclipse.lsp4j.Position
import org.eclipse.lsp4j.Range
import org.eclipse.lsp4j.TextDocumentContentChangeEvent
import org.eclipse.lsp4j.jsonrpc.messages.Either
import org.eclipse.lsp4j.services.TextDocumentService
import java.util.concurrent.CompletableFuture

class VintellijTextDocumentService(private val languageServer: VintellijLanguageServer) : TextDocumentService {
    private val bufferManager: BufferManager = BufferManager(languageServer)

    private val async = AsyncExecutor()

    override fun didOpen(params: DidOpenTextDocumentParams) {
        bufferManager.loadBuffer(uriToPath(params.textDocument.uri))
    }

    override fun didChange(params: DidChangeTextDocumentParams) {
        onDocumentChange(params)
    }

    override fun didClose(params: DidCloseTextDocumentParams) {
        bufferManager.releaseBuffer(uriToPath(params.textDocument.uri))
    }

    override fun didSave(params: DidSaveTextDocumentParams) {
        val buffer = bufferManager.getBuffer(uriToPath(params.textDocument.uri)) ?: return
        val document = buffer.getDocument()

        val application = ApplicationManager.getApplication()
        application.invokeAndWait {
            application.runWriteAction {
                FileDocumentManager.getInstance().saveDocumentAsIs(document)
            }
        }
    }

    override fun definition(params: DefinitionParams): CompletableFuture<Either<List<Location>, List<LocationLink>>> = async.compute {
        val buffer = bufferManager.getBuffer(uriToPath(params.textDocument.uri))
        if (buffer != null) {
            val locations: ArrayList<Location> = ArrayList<Location>()
            ApplicationManager.getApplication().invokeAndWait {
                buffer.moveCaretToPosition(params.position.line, params.position.character)
                val editor = buffer.editor
                val gotoImplementationHandler = GotoImplementationHandler()
                val gotoData = gotoImplementationHandler.getSourceAndTargetElements(editor.editor, buffer.getPsiFile())
                gotoData?.targets?.forEach { target ->
                    val pathWithOffset = PathUtils.getPathWithOffsetFromVirtualFileAndPsiElement(target.containingFile.virtualFile, target)
                    if (pathWithOffset != null) {
                        val position = Position(0, 0)
                        val location = Location(pathWithOffset.first, Range(position, position))
                        locations.add(location)
                    }
                }
            }

            Either.forLeft(locations)
        }
        else {
            Either.forLeft(emptyList())
        }
    }

    @Synchronized
    private fun onDocumentChange(params: DidChangeTextDocumentParams) {
        val project = languageServer.getProject() ?: return

        val buffer = bufferManager.getBuffer(uriToPath(params.textDocument.uri)) ?: return
        val document = buffer.getDocument()
        val changes = params.contentChanges.sortedWith(compareBy({ it.range.start.line }, { it.range.start.character })).reversed()
        val application = ApplicationManager.getApplication()
        application.invokeAndWait {
            CommandProcessor.getInstance().executeCommand(project, {
                application.runWriteAction {
                    changes.forEach { change ->
                        applyDocumentChange(document, change)
                    }

                    PsiDocumentManager.getInstance(project).commitDocument(document)
                }
            }, "update-document", "", UndoConfirmationPolicy.REQUEST_CONFIRMATION)
        }
    }

    private fun applyDocumentChange(document: Document, change: TextDocumentContentChangeEvent) {
        if (change.range == null) {
            document.setText(change.text)
        } else {
            val textRange = change.range.toTextRange(document)
            document.replaceString(textRange.startOffset, textRange.endOffset, change.text)
        }
    }
}