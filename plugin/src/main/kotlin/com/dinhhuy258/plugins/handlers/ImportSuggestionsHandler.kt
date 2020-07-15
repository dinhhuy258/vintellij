package com.dinhhuy258.plugins.handlers

import com.dinhhuy258.plugins.exceptions.FileTypeNotSupportException
import com.dinhhuy258.plugins.idea.ImportSuggester
import com.dinhhuy258.plugins.utils.FileUtils
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtSimpleNameExpression

class ImportSuggestionsHandler : BaseHandler<ImportSuggestionsHandler.Request, List<String>>() {
    data class Request(val file: String, val offset: Int)

    override fun requestClass(): Class<Request> {
        return Request::class.java
    }

    override fun handle(request: Request): List<String> {
        val psiFile = FileUtils.getPsiFile(request.file)
        if (psiFile !is KtFile) {
            throw FileTypeNotSupportException("File type not supported: ${psiFile.fileType.name}")
        }

        val psiKtFile: KtFile = psiFile as KtFile
        val element = psiKtFile.findElementAt(request.offset) ?: return emptyList()
        if (element.parent !is KtSimpleNameExpression) {
            return emptyList()
        }

        val importSuggester = ImportSuggester(element.parent as KtSimpleNameExpression)
        return importSuggester.collectSuggestions()
    }
}
