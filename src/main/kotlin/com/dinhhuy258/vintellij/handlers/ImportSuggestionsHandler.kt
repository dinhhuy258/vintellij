package com.dinhhuy258.vintellij.handlers

import com.dinhhuy258.vintellij.idea.IdeaUtils
import com.dinhhuy258.vintellij.idea.imports.ImportSuggesterFactory
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.util.Ref

class ImportSuggestionsHandler : BaseHandler<ImportSuggestionsHandler.Request, ImportSuggestionsHandler.Response>() {
    data class Request(val file: String, val offset: Int)

    data class Response(val imports: List<String>)

    override fun requestClass(): Class<Request> {
        return Request::class.java
    }

    override fun handleInternal(request: Request): Response {
        val psiFile = IdeaUtils.getPsiFile(request.file)

        val application = ApplicationManager.getApplication()
        val responseRef = Ref<Response>()
        application.runReadAction {
            val element = psiFile.findElementAt(request.offset)?.parent ?: return@runReadAction

            val importSuggester = ImportSuggesterFactory.createImportSuggester(psiFile.language)
            responseRef.set(Response(importSuggester.collectSuggestions(element)))
        }

        return responseRef.get() ?: return Response(emptyList())
    }
}
