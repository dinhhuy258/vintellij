package com.dinhhuy258.plugins.handlers

import com.dinhhuy258.plugins.exceptions.FileTypeNotSupportException
import com.dinhhuy258.plugins.idea.files.FileFinder
import com.dinhhuy258.plugins.idea.imports.KtImportSuggester
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtSimpleNameExpression

class ImportSuggestionsHandler : BaseHandler<ImportSuggestionsHandler.Request, List<String>>() {
    private val fileFinder = FileFinder()

    private val ktImportSuggester = KtImportSuggester()

    data class Request(val file: String, val offset: Int)

    override fun requestClass(): Class<Request> {
        return Request::class.java
    }

    override fun handle(request: Request): List<String> {
        val psiKtFile = fileFinder.getPsiFile(request.file)
        if (psiKtFile !is KtFile) {
            throw FileTypeNotSupportException("File type not supported: ${psiKtFile.fileType.name}")
        }

        val element = psiKtFile.findElementAt(request.offset) ?: return emptyList()
        if (element.parent !is KtSimpleNameExpression) {
            return emptyList()
        }
        
        return ktImportSuggester.collectSuggestions(element.parent as KtSimpleNameExpression)
    }
}
