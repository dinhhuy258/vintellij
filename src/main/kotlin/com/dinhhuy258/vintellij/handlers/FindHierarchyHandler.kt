package com.dinhhuy258.vintellij.handlers

import com.dinhhuy258.vintellij.idea.IdeaUtils
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMethod
import com.intellij.psi.search.ProjectScopeBuilder
import com.intellij.psi.search.searches.ClassInheritorsSearch
import com.intellij.psi.search.searches.OverridingMethodsSearch
import org.jetbrains.kotlin.asJava.getRepresentativeLightMethod
import org.jetbrains.kotlin.asJava.toLightClassWithBuiltinMapping
import org.jetbrains.kotlin.asJava.unwrapped
import org.jetbrains.kotlin.idea.refactoring.fqName.getKotlinFqName
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtFunction

class FindHierarchyHandler : BaseHandler<FindHierarchyHandler.Request, FindHierarchyHandler.Response>() {
    data class Request(val file: String, val offset: Int)

    data class HandlerData(val file: String, val offset: Int, val name: String)

    data class Response(val classes: List<HandlerData>)

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

            val scope = ProjectScopeBuilder.getInstance(IdeaUtils.getProject()).buildProjectScope()
            val subClasses = ClassInheritorsSearch.search(psiClass, scope, true, true, true)
            
            return Response(subClasses.mapNotNull {
                val subClass = it.unwrapped ?: return@mapNotNull null
                val className = subClass.getKotlinFqName() ?: return@mapNotNull null

                HandlerData(subClass.containingFile.virtualFile.path, subClass.textOffset, className.asString())
            })
        } else if (psiElement.context is KtFunction || psiElement.context is PsiMethod) {
            val psiMethod = if (psiElement.context is KtFunction) {
                (psiElement.context as KtFunction).getRepresentativeLightMethod()
            }
            else {
                psiElement.context as PsiMethod
            } ?: return Response(emptyList())

            val scope = ProjectScopeBuilder.getInstance(IdeaUtils.getProject()).buildProjectScope()
            val overridingMethods = OverridingMethodsSearch.search(psiMethod, scope, true)

            return Response(overridingMethods.mapNotNull {
                val overridingMethod = it.unwrapped ?: return@mapNotNull null
                val className = overridingMethod.getKotlinFqName()?.parent()?.asString() ?: return@mapNotNull null
                val methodName = psiMethod.nameIdentifier?.text ?: return@mapNotNull null

                HandlerData(overridingMethod.containingFile.virtualFile.path, overridingMethod.textOffset, "$className:$methodName")
            })
        }

        return Response(emptyList())
    }
}