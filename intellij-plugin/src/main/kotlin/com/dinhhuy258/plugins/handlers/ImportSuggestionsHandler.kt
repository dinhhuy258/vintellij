package com.dinhhuy258.plugins.handlers

import com.dinhhuy258.plugins.exceptions.FileTypeNotSupportException
import com.dinhhuy258.plugins.idea.IdeaUtils
import com.dinhhuy258.plugins.idea.imports.KtImportSuggester
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtSimpleNameExpression

class ImportSuggestionsHandler : BaseHandler<ImportSuggestionsHandler.Request, ImportSuggestionsHandler.Response>() {
    private val ktImportSuggester = KtImportSuggester()

    data class Request(val file: String, val offset: Int)

    data class Response(val importCandidates: List<String>) {
        override fun toString(): String {
            return importCandidates.joinToString("|")
        }
    }

    override fun requestClass(): Class<Request> {
        return Request::class.java
    }

    override fun handleInternal(request: Request): Response {
        val psiFile = IdeaUtils.getPsiFile(request.file)
        if (psiFile !is KtFile) {
            throw FileTypeNotSupportException("File type not supported: ${psiFile.fileType.name}")
        }

        val element = psiFile.findElementAt(request.offset) ?: return Response(emptyList())
        if (element.parent !is KtSimpleNameExpression) {
            return Response(emptyList())
        }
        
        return Response(ktImportSuggester.collectSuggestions(element.parent as KtSimpleNameExpression))
    }
}
