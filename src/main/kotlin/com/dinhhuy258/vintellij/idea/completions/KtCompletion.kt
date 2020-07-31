package com.dinhhuy258.vintellij.idea.completions

import com.intellij.psi.CommonClassNames
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.search.searches.DirectClassInheritorsSearch
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.idea.core.KotlinIndicesHelper
import org.jetbrains.kotlin.idea.refactoring.fqName.getKotlinFqName
import org.jetbrains.kotlin.idea.util.CallTypeAndReceiver
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtSimpleNameExpression
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameOrNull

class KtCompletion(onSuggest: (word: String, kind: CompletionKind, menu: String) -> Unit) : AbstractCompletion(onSuggest) {
    override fun doCompletion(psiFile: PsiFile, offset: Int, prefix: String) {
        val ktFile = psiFile as KtFile
        val completionElement = ktFile.findElementAt(offset)?.parent ?: return

        if (completionElement is KtAnnotationEntry ||
                (completionElement is KtSimpleNameExpression && CallTypeAndReceiver.detect(completionElement) is CallTypeAndReceiver.ANNOTATION)) {
            findAllAnnotationClasses(completionElement, prefix)
        }
    }

    private fun findAllAnnotationClasses(context: PsiElement, prefix: String) {
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
                    val menu = psiClass.getKotlinFqName()?.asString() ?: ""
                    onSuggest(name, CompletionKind.TYPE, menu)
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
