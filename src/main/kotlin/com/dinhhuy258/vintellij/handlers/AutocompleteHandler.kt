package com.dinhhuy258.vintellij.handlers

import com.dinhhuy258.vintellij.idea.IdeaUtils
import com.dinhhuy258.vintellij.idea.completions.CompletionKind
import com.dinhhuy258.vintellij.idea.completions.VICodeCompletionHandler
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.psi.PsiDocumentManager
import kotlin.collections.ArrayList
import kotlin.math.min

class AutocompleteHandler : BaseHandler<AutocompleteHandler.Request, AutocompleteHandler.Response>() {
    companion object {
        private const val MAX_COMPLETIONS = 10
    }

    data class Request(val file: String, val offset: Int)

    data class Completion(val word: String, val kind: CompletionKind, val menu: String) : Comparable<Completion> {
        override fun compareTo(other: Completion): Int {
            // Sort by keyword first
            val isKeyWord = this.kind == CompletionKind.KEYWORD
            val isOtherKeyWord = other.kind == CompletionKind.KEYWORD
            if (isKeyWord && !isOtherKeyWord) {
                return 1
            }
            if (!isKeyWord && isOtherKeyWord) {
                return -1
            }

            return this.word.compareTo(other.word)
        }
    }

    data class Response(val completions: List<Completion>)

    override fun requestClass(): Class<Request> {
        return Request::class.java
    }

    override fun handleInternal(request: Request): Response {
        val application = ApplicationManager.getApplication()
        val project = IdeaUtils.getProject()
        val psiFile = IdeaUtils.getPsiFile(request.file)
        val document = PsiDocumentManager.getInstance(project).getDocument(psiFile) ?: return Response(emptyList())

        val completions = ArrayList<Completion>()
        val codeCompletionHandler = VICodeCompletionHandler { word: String, kind: CompletionKind, menu: String ->
            completions.add(Completion(word, kind, menu))
        }

        application.invokeAndWait {
            val editor = EditorFactory.getInstance().createEditor(document, project)
            editor.caretModel.moveToOffset(request.offset)
            CommandProcessor.getInstance().executeCommand(project, {
                codeCompletionHandler.invokeCompletion(project, editor)
            }, null, null)
        }

        completions.sort()

        return Response(completions.subList(0, min(completions.size, MAX_COMPLETIONS)))
    }
}
