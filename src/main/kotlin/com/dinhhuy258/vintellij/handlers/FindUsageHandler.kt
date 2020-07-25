package com.dinhhuy258.vintellij.handlers

import com.dinhhuy258.vintellij.idea.IdeaUtils
import com.dinhhuy258.vintellij.idea.IdeaUtils.Companion.getPsiMethod
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.search.ProjectScopeBuilder
import com.intellij.psi.search.searches.MethodReferencesSearch
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.refactoring.suggested.startOffset
import org.jetbrains.kotlin.idea.core.util.getLineNumber
import org.jetbrains.kotlin.psi.KtFunction

class FindUsageHandler : BaseHandler<FindUsageHandler.Request, FindUsageHandler.Response>() {
    data class Request(val file: String, val offset: Int)

    data class Usage(val preview: String, val file: String, val offset: Int)

    data class Response(val usages: List<Usage>)

    override fun requestClass(): Class<Request> {
        return Request::class.java
    }

    override fun handleInternal(request: Request): Response {
        val psiFile = IdeaUtils.getPsiFile(request.file)
        val psiElement = psiFile.findElementAt(request.offset)?.context ?: return Response(emptyList())
        val scope = ProjectScopeBuilder.getInstance(IdeaUtils.getProject()).buildProjectScope()

        if (psiElement is KtFunction || psiElement is PsiMethod) {
            val psiMethod = getPsiMethod(psiElement) ?: return Response(emptyList())

            return Response(MethodReferencesSearch.search(psiMethod, scope, true).mapNotNull {
                val referenceMethod = it.element.parent
                getUsage(referenceMethod) ?: return@mapNotNull null
            })
        }

        return Response(ReferencesSearch.search(ReferencesSearch.SearchParameters(psiElement, scope, false)).mapNotNull {
            val referenceElement = it.element.parent
            getUsage(referenceElement) ?: return@mapNotNull null
        })
    }

    private fun getUsage(psiElement: PsiElement): Usage? {
        val referenceFile = psiElement.containingFile ?: return null
        val referenceDocument = PsiDocumentManager.getInstance(IdeaUtils.getProject()).getDocument(referenceFile) ?: return null
        val className = IdeaUtils.getClassName(referenceFile) ?: return null

        val filePath = referenceFile.virtualFile.path
        val offset = psiElement.startOffset
        val line = psiElement.getLineNumber()
        val statement = referenceDocument.getText(TextRange(referenceDocument.getLineStartOffset(line), referenceDocument.getLineEndOffset(line))).trim()

        return Usage("$className:${line + 1} ($statement)", filePath, offset)
    }
}
