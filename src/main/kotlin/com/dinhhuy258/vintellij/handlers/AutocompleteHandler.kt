package com.dinhhuy258.vintellij.handlers

import com.dinhhuy258.vintellij.idea.IdeaUtils
import com.dinhhuy258.vintellij.idea.completions.CompletionKind
import com.dinhhuy258.vintellij.idea.completions.VICodeCompletionHandler
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.lang.java.JavaLanguage
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFileFactory
import org.jetbrains.kotlin.idea.KotlinLanguage
import kotlin.math.min

class AutocompleteHandler : BaseHandler<AutocompleteHandler.Request, AutocompleteHandler.Response>() {
    companion object {
        private const val MAX_COMPLETIONS = 50
    }

    data class Request(val file: String, val offset: Int, val base: String)

    data class Completion(val word: String, val kind: CompletionKind, val menu: String) : Comparable<Completion> {
        override fun compareTo(other: Completion): Int {
            // Unknown is always in the last
            val isUnknown = this.kind == CompletionKind.UNKNOWN
            val isOtherUnknown = other.kind == CompletionKind.UNKNOWN
            if (isUnknown && !isOtherUnknown) {
                return -1
            }
            if (!isUnknown && isOtherUnknown) {
                return 1
            }

            // Keyword always in the first
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
        val base = request.base.trim()
        val onSuggest =  { item: String, word: String, kind: CompletionKind, menu: String ->
            if (item.startsWith(base)) {
                completions.add(Completion(word, kind, menu))
            }
        }
        val basicCompletionHandler = VICodeCompletionHandler(CompletionType.BASIC, onSuggest)
        val classNameCompleteHandler = VICodeCompletionHandler(CompletionType.CLASS_NAME, onSuggest)

        application.invokeAndWait {
            // Currently, I haven't found any way to do autocomplete in Kotlin language
            // This is a little trick, to convert the content to Java file then do autocomplete on this file
            // The accuracy of course is not going to be good
            val editor = if (psiFile.language == KotlinLanguage.INSTANCE) {
                val psiFileInJavaLanguage = PsiFileFactory.getInstance(project).createFileFromText(JavaLanguage.INSTANCE, document.text)
                val documentInJavaLanguage = PsiDocumentManager.getInstance(project).getDocument(psiFileInJavaLanguage)
                if (documentInJavaLanguage != null) {
                    EditorFactory.getInstance().createEditor(documentInJavaLanguage, project)
                }
                else {
                    null
                }
            }
            else {
                EditorFactory.getInstance().createEditor(document, project)
            }

            if (editor != null) {
                editor.caretModel.moveToOffset(request.offset)
                CommandProcessor.getInstance().executeCommand(project, {
                    basicCompletionHandler.invokeCompletion(project, editor)
                    classNameCompleteHandler.invokeCompletion(project, editor)
                }, null, null)
            }
        }

        completions.sort()

        return Response(completions.subList(0, min(completions.size, MAX_COMPLETIONS)))
    }
}
