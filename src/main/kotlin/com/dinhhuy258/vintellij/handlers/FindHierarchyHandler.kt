package com.dinhhuy258.vintellij.handlers

import com.dinhhuy258.vintellij.idea.IdeaUtils
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMethod
import com.intellij.psi.search.ProjectScopeBuilder
import com.intellij.psi.search.searches.ClassInheritorsSearch
import com.intellij.psi.search.searches.OverridingMethodsSearch
import org.jetbrains.kotlin.asJava.unwrapped
import org.jetbrains.kotlin.idea.refactoring.fqName.getKotlinFqName
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtFunction

class FindHierarchyHandler : BaseHandler<FindHierarchyHandler.Request, FindHierarchyHandler.Response>() {
    data class Request(val file: String, val offset: Int)

    data class Hierarchy(val preview: String, val file: String, val offset: Int)

    data class Response(val hierarchies: List<Hierarchy>)

    override fun requestClass(): Class<Request> {
        return Request::class.java
    }

    override fun handleInternal(request: Request): Response {
        val psiFile = IdeaUtils.getPsiFile(request.file)
        val application = ApplicationManager.getApplication()
        val responseRef = Ref<Response>()
        application.runReadAction {
            val psiElement = psiFile.findElementAt(request.offset) ?: return@runReadAction
            if (psiElement.context is KtClassOrObject || psiElement.context is PsiClass) {
                val psiClass = IdeaUtils.getPsiClass(psiElement.context!!) ?: return@runReadAction

                val scope = ProjectScopeBuilder.getInstance(IdeaUtils.getProject()).buildProjectScope()
                val subClasses = ClassInheritorsSearch.search(psiClass, scope, true, true, true)

                responseRef.set(Response(subClasses.mapNotNull {
                    val subClass = it.unwrapped ?: return@mapNotNull null
                    val className = subClass.getKotlinFqName() ?: return@mapNotNull null

                    Hierarchy(className.asString(), subClass.containingFile.virtualFile.path, subClass.textOffset)
                }))
            } else if (psiElement.context is KtFunction || psiElement.context is PsiMethod) {
                val psiMethod = IdeaUtils.getPsiMethod(psiElement.context!!) ?: return@runReadAction

                val scope = ProjectScopeBuilder.getInstance(IdeaUtils.getProject()).buildProjectScope()
                val overridingMethods = OverridingMethodsSearch.search(psiMethod, scope, true)

                responseRef.set(Response(overridingMethods.mapNotNull {
                    val overridingMethod = it.unwrapped ?: return@mapNotNull null
                    val className = overridingMethod.getKotlinFqName()?.parent()?.asString() ?: return@mapNotNull null
                    val methodName = psiMethod.nameIdentifier?.text ?: return@mapNotNull null

                    Hierarchy("$className:$methodName", overridingMethod.containingFile.virtualFile.path, overridingMethod.textOffset)
                }))
            }
        }

        return responseRef.get() ?: return Response(emptyList())
    }
}