package com.dinhhuy258.plugins.handlers

import com.dinhhuy258.plugins.idea.IdeaUtils
import com.intellij.codeEditor.JavaEditorFileSwapper
import com.intellij.ide.highlighter.JavaClassFileType

class GoToDefinitionHandler : BaseHandler<GoToDefinitionHandler.Request, GoToDefinitionHandler.Response>() {
    data class Request(val file: String, val offset: Int)

    data class Response(val file: String?)

    override fun requestClass(): Class<Request> {
        return Request::class.java
    }

    override fun handleInternal(request: Request): Response {
        val psiFile = IdeaUtils.getPsiFile(request.file)
        val psiReference = psiFile.findReferenceAt(request.offset) ?: return Response(null)
        val psiElement = psiReference.resolve() ?: return Response(null)
        val virtualFile = psiElement.parent.containingFile.virtualFile ?: return Response(null)
        if (virtualFile.fileType is JavaClassFileType) {
            val project = IdeaUtils.getProject()
            val sourceFile = JavaEditorFileSwapper.findSourceFile(project, virtualFile)
            if (sourceFile != null) {
                return Response(sourceFile.path)
            }
        }

        return Response(virtualFile.path)
    }
}
