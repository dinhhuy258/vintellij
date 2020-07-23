package com.dinhhuy258.vintellij.handlers

import com.dinhhuy258.vintellij.idea.IdeaUtils
import com.dinhhuy258.vintellij.utils.PathUtils
import com.intellij.psi.PsiClass
import com.intellij.psi.search.ProjectAndLibrariesScope
import com.intellij.psi.search.searches.ClassInheritorsSearch
import org.jetbrains.kotlin.asJava.toLightClassWithBuiltinMapping
import org.jetbrains.kotlin.asJava.unwrapped
import org.jetbrains.kotlin.idea.refactoring.fqName.getKotlinFqName
import org.jetbrains.kotlin.psi.KtClassOrObject

class FindHierarchyHandler : BaseHandler<FindHierarchyHandler.Request, FindHierarchyHandler.Response>() {
    data class Request(val file: String, val offset: Int)

    data class SubClassData(val file: String, val offset: Int, val name: String)

    data class Response(val classes: List<SubClassData>)

    override fun requestClass(): Class<Request> {
        return Request::class.java
    }

    override fun handleInternal(request: Request): Response {
        val psiFile = IdeaUtils.getPsiFile(request.file)
        val psiElement = psiFile.findElementAt(request.offset) ?: return Response(emptyList())
        if (psiElement.context is KtClassOrObject || psiElement.context is PsiClass) {
            val psiClass = if (psiElement.context is PsiClass) {
                psiElement.context as PsiClass
            }
            else {
                (psiElement.context as KtClassOrObject).toLightClassWithBuiltinMapping()
            } ?: return Response(emptyList())

            val scope = ProjectAndLibrariesScope(IdeaUtils.getProject())
            val classes = ClassInheritorsSearch.search(psiClass, scope, true, true, true)
            val subClasses = classes.mapNotNull {
                val subClass = it.unwrapped ?: return@mapNotNull null
                val pathWithOffset = PathUtils.getPathWithOffsetFromVirtualFileAndPsiElement(subClass.containingFile.virtualFile, subClass)
                        ?: Pair(subClass.containingFile.virtualFile.path, subClass.textOffset)
                val fqName = subClass.getKotlinFqName() ?: return@mapNotNull null

                SubClassData(pathWithOffset.first, pathWithOffset.second, fqName.asString())
            }

            return Response(subClasses)
        }

        return Response(emptyList())
    }
}