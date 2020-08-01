package com.dinhhuy258.vintellij.idea.completions.kotlin

import com.dinhhuy258.vintellij.idea.completions.CompletionKind
import com.intellij.openapi.project.DumbService
import com.intellij.psi.CommonClassNames
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.search.AllClassesSearchExecutor
import com.intellij.psi.search.PsiShortNamesCache
import com.intellij.psi.search.searches.DirectClassInheritorsSearch
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.idea.core.KotlinIndicesHelper
import org.jetbrains.kotlin.idea.refactoring.fqName.getKotlinFqName
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameOrNull

class ClassNameCompletion(private val onSuggest: (word: String, kind: CompletionKind, menu: String) -> Unit) {
    fun findAllClasses(context: PsiElement, prefix: String) {
        val scope = context.resolveScope
        val project = context.project

        val dumbService = DumbService.getInstance(project)
        val cache = PsiShortNamesCache.getInstance(project)
        AllClassesSearchExecutor.processClassNames(project, scope) { className: String ->
            if (className.startsWith(prefix)) {
                dumbService.runReadActionInSmartMode {
                    val psiClasses = cache.getClassesByName(className, scope)
                    psiClasses.forEach { psiClass ->
                        val menu = psiClass.getKotlinFqName()?.asString()
                        if (menu != null) {
                            onSuggest(className, CompletionKind.TYPE, menu)
                        }
                    }
                }
            }
            true
        }
    }

    fun findAllAnnotationClasses(context: PsiElement, prefix: String) {
        val scope = context.resolveScope

        // Get java annotations
        val javaAnnotation = JavaPsiFacade.getInstance(context.project)
                .findClass(CommonClassNames.JAVA_LANG_ANNOTATION_ANNOTATION, scope)
        if (javaAnnotation != null) {
            DirectClassInheritorsSearch.search(javaAnnotation, scope, false).forEach { psiClass ->
                if (!psiClass.isAnnotationType || psiClass.qualifiedName == null) {
                    return@forEach
                }

                val name = psiClass.name ?: return@forEach

                if (name.startsWith(prefix)) {
                    val menu = psiClass.getKotlinFqName()?.asString()
                    if (menu != null) {
                        onSuggest(name, CompletionKind.TYPE, menu)
                    }
                }
            }
        }

        // Get kotlin annotation
        val kotlinFile = context.containingFile as KtFile
        val resolutionFacade = kotlinFile.getResolutionFacade()
        fun isVisible(descriptor: DeclarationDescriptor): Boolean {
            // TODO: Handle the visibility
            return true
        }

        val indicesHelper = KotlinIndicesHelper(resolutionFacade, scope, ::isVisible, file = kotlinFile)

        val nameFilter: (name: String) -> Boolean = { name ->
            name.startsWith(prefix)
        }
        val kindFilter: (classKind: ClassKind) -> Boolean = { classKind ->
            classKind == ClassKind.ANNOTATION_CLASS
        }

        val kotlinAnnotations = indicesHelper.getKotlinClasses(nameFilter, kindFilter = kindFilter).distinct()
        kotlinAnnotations.forEach { classDescriptor ->
            val menu = classDescriptor.fqNameOrNull()?.asString() ?: ""
            onSuggest(classDescriptor.name.asString(), CompletionKind.TYPE, menu)
        }
    }
}
