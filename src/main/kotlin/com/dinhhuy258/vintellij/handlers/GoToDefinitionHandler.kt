package com.dinhhuy258.vintellij.handlers

import com.dinhhuy258.vintellij.exceptions.VIException
import com.dinhhuy258.vintellij.idea.IdeaUtils
import com.dinhhuy258.vintellij.utils.UrlUtils
import com.intellij.codeEditor.JavaEditorFileSwapper
import com.intellij.ide.highlighter.JavaClassFileType
import com.intellij.openapi.util.Comparing
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMember
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.idea.decompiler.builtIns.KotlinBuiltInFileType
import org.jetbrains.kotlin.idea.decompiler.navigation.SourceNavigationHelper
import org.jetbrains.kotlin.psi.KtDeclaration

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
        if (virtualFile.fileType is JavaClassFileType && psiElement !is KtDeclaration) {
            return goToDefinitionJavaClassFile(virtualFile, psiElement)
        } else if ((virtualFile.fileType is KotlinBuiltInFileType || virtualFile.fileType is JavaClassFileType) && psiElement is KtDeclaration) {
            return goToDefinitionKtClassFile(virtualFile, psiElement)
        }

        // Go to definition to the file in the project
        return Response(virtualFile.path, psiElement.textOffset)
    }

    private fun goToDefinitionJavaClassFile(virtualFile: VirtualFile, psiElement: PsiElement): Response {
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
        throw VIException("Can not find source file: ${virtualFile.path}. Please using intellij to download the missing source.")
    }

    private fun goToDefinitionKtClassFile(virtualFile: VirtualFile, psiElement: KtDeclaration): Response {
        val sourceFile = SourceNavigationHelper.getNavigationElement(psiElement).containingFile.virtualFile
        if (sourceFile != null) {
            // Recalculate the text offset for source file
            var textOffset = 0
            val navigationElement = psiElement.navigationElement
            if (navigationElement != null && Comparing.equal(navigationElement.containingFile.virtualFile, sourceFile)) {
                textOffset = navigationElement.textOffset
            }
            return Response(UrlUtils.toVimFilePath(sourceFile.path), textOffset)
        }
        throw VIException("Can not find source file: ${virtualFile.path}. Please using intellij to download the missing source.")
    }
}
