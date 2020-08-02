package com.dinhhuy258.vintellij.handlers

import com.dinhhuy258.vintellij.exceptions.VIException
import com.dinhhuy258.vintellij.idea.IdeaUtils
import com.dinhhuy258.vintellij.utils.PathUtils
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.util.Ref

class GoToDefinitionHandler : BaseHandler<GoToDefinitionHandler.Request, GoToDefinitionHandler.Response>() {
    data class Request(val file: String, val offset: Int)

    data class Response(val file: String?, val offset: Int?)

    override fun requestClass(): Class<Request> {
        return Request::class.java
    }

    override fun handleInternal(request: Request): Response {
        val psiFile = IdeaUtils.getPsiFile(request.file)
        val application = ApplicationManager.getApplication()
        val pathWithOffsetRef = Ref<Pair<String, Int>>()
        application.runReadAction {
            val psiReference = psiFile.findReferenceAt(request.offset) ?: return@runReadAction
            val psiElement = psiReference.resolve() ?: return@runReadAction
            val virtualFile = psiElement.parent.containingFile.virtualFile ?: return@runReadAction

            val pathWithOffset = PathUtils.getPathWithOffsetFromVirtualFileAndPsiElement(virtualFile, psiElement)
                    ?: throw VIException("Can not find source file: ${virtualFile.path}. Please using intellij to download the missing source.")
            pathWithOffsetRef.set(pathWithOffset)
        }

        val pathWithOffset = pathWithOffsetRef.get() ?: return Response(null, null)
        return Response(pathWithOffset.first, pathWithOffset.second)
    }
}
