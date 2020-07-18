package com.dinhhuy258.plugins.handlers

import com.dinhhuy258.plugins.idea.IdeaUtils
import com.intellij.codeEditor.JavaEditorFileSwapper
import com.intellij.ide.highlighter.JavaClassFileType

class GoToDefinitionHandler : BaseHandler<GoToDefinitionHandler.Request, String>() {
    data class Request(val file: String, val offset: Int)

    override fun requestClass(): Class<Request> {
        return Request::class.java
    }

    override fun handleInternal(request: Request): String {
        val psiFile = IdeaUtils.getPsiFile(request.file)
        val psiReference = psiFile.findReferenceAt(request.offset) ?: return ""
        val psiElement = psiReference.resolve() ?: return ""
        val virtualFile = psiElement.parent.containingFile.virtualFile ?: return ""
        if (virtualFile.fileType is JavaClassFileType) {
            val project = IdeaUtils.getProject()
            val sourceFile = JavaEditorFileSwapper.findSourceFile(project, virtualFile)
            if (sourceFile != null) {
                return sourceFile.path
            }
            return virtualFile.path
        }

        return virtualFile.path
    }
}
