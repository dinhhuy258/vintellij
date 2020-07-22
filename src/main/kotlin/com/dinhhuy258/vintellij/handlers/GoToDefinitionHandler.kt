package com.dinhhuy258.vintellij.handlers

import com.dinhhuy258.vintellij.exceptions.VIException
import com.dinhhuy258.vintellij.idea.IdeaUtils
import com.dinhhuy258.vintellij.utils.PathUtils

class GoToDefinitionHandler : BaseHandler<GoToDefinitionHandler.Request, GoToDefinitionHandler.Response>() {
    data class Request(val file: String, val offset: Int)

    data class Response(val file: String?, val offset: Int?)

    override fun requestClass(): Class<Request> {
        return Request::class.java
    }

    override fun handleInternal(request: Request): Response {
        val psiFile = IdeaUtils.getPsiFile(request.file)
        val psiReference = psiFile.findReferenceAt(request.offset) ?: return Response(null, null)
        val psiElement = psiReference.resolve() ?: return Response(null, null)
        val virtualFile = psiElement.parent.containingFile.virtualFile ?: return Response(null, null)

        val pathWithOffset = PathUtils.getPathWithOffsetFromVirtualFileAndPsiElement(virtualFile, psiElement)
                ?: throw VIException("Can not find source file: ${virtualFile.path}. Please using intellij to download the missing source.")
        return Response(pathWithOffset.first, pathWithOffset.second)
    }
}
