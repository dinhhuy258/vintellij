package com.dinhhuy258.vintellij.handlers

import com.dinhhuy258.vintellij.idea.IdeaUtils
import com.dinhhuy258.vintellij.idea.completions.CompletionFactory
import com.dinhhuy258.vintellij.idea.completions.CompletionKind
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
                return 1
            }
            if (!isUnknown && isOtherUnknown) {
                return -1
            }

            // Keyword always in the first
            val isKeyWord = this.kind == CompletionKind.KEYWORD
            val isOtherKeyWord = other.kind == CompletionKind.KEYWORD
            if (isKeyWord && !isOtherKeyWord) {
                return -1
            }
            if (!isKeyWord && isOtherKeyWord) {
                return 1
            }

            return this.word.compareTo(other.word)
        }
    }

    data class Response(val completions: List<Completion>)

    override fun requestClass(): Class<Request> {
        return Request::class.java
    }

    override fun handleInternal(request: Request): Response {
        val psiFile = IdeaUtils.getPsiFile(request.file)
        val completions = ArrayList<Completion>()
        val base = request.base.trim()
        val onSuggest =  { item: String, word: String, kind: CompletionKind, menu: String ->
            if (item.startsWith(base)) {
                completions.add(Completion(word, kind, menu))
            }
        }

        val completion = CompletionFactory.createCompletion(psiFile.language, onSuggest)
        completion.doCompletion(psiFile, request.offset)

        completions.sort()
        return Response(completions.subList(0, min(completions.size, MAX_COMPLETIONS)))
    }
}
