package com.dinhhuy258.vintellij.handlers

import com.dinhhuy258.vintellij.exceptions.VIException
import com.dinhhuy258.vintellij.idea.IdeaUtils
import com.dinhhuy258.vintellij.idea.imports.KtImportSuggester
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.util.Ref
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtSimpleNameExpression

class ImportSuggestionsHandler : BaseHandler<ImportSuggestionsHandler.Request, ImportSuggestionsHandler.Response>() {
    private val ktImportSuggester = KtImportSuggester()

    data class Request(val file: String, val offset: Int)

    data class Response(val imports: List<String>)

    override fun requestClass(): Class<Request> {
        return Request::class.java
    }

    override fun handleInternal(request: Request): Response {
        val psiFile = IdeaUtils.getPsiFile(request.file)
        if (psiFile !is KtFile) {
            throw VIException("File type not supported: ${psiFile.fileType.name}.")
        }

        val application = ApplicationManager.getApplication()
        val responseRef = Ref<Response>()
        application.runReadAction {
            val element = psiFile.findElementAt(request.offset) ?: return@runReadAction
            if (element.parent !is KtSimpleNameExpression) {
                return@runReadAction
            }

            responseRef.set(Response(ktImportSuggester.collectSuggestions(element.parent as KtSimpleNameExpression)))
        }

        return responseRef.get() ?: return Response(emptyList())
    }
}
