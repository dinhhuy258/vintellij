package com.dinhhuy258.plugins.handlers

import com.dinhhuy258.plugins.exceptions.VIException
import com.dinhhuy258.plugins.idea.IdeaUtils
import com.dinhhuy258.plugins.utils.UrlUtils
import com.intellij.codeEditor.JavaEditorFileSwapper
import com.intellij.ide.highlighter.JavaClassFileType
import com.intellij.openapi.util.Comparing
import com.intellij.psi.PsiMember
import com.intellij.psi.util.PsiTreeUtil

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
        if (virtualFile.fileType is JavaClassFileType) {
            val project = IdeaUtils.getProject()
            val sourceFile = JavaEditorFileSwapper.findSourceFile(project, virtualFile)
            if (sourceFile != null) {
                // Recalculate the text offset for source file
                val member = PsiTreeUtil.getParentOfType(psiElement, PsiMember::class.java, false)
                var textOffset = 0
                if (member != null) {
                    val navigationElement = member.originalElement.navigationElement
                    if (navigationElement != null && Comparing.equal(navigationElement.containingFile.virtualFile, sourceFile)) {
                        textOffset = navigationElement.textOffset
                    }
                }
                return Response(UrlUtils.toVimFilePath(sourceFile.path), textOffset)
            }
            throw VIException("Can not find source file: $sourceFile.path. Please using intellij to download the missing source.")
        }

        return Response(virtualFile.path, psiElement.textOffset)
    }
}
