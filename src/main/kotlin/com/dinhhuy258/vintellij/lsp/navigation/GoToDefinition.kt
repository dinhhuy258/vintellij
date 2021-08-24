package com.dinhhuy258.vintellij.lsp.navigation

import com.dinhhuy258.vintellij.lsp.Buffer
import com.dinhhuy258.vintellij.lsp.utils.getLocation
import com.intellij.lang.java.JavaLanguage
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.LogicalPosition
import com.intellij.openapi.project.IndexNotReadyException
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.search.searches.SuperMethodsSearch
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.containers.ContainerUtil
import org.eclipse.lsp4j.Location
import org.eclipse.lsp4j.Position
import org.jetbrains.kotlin.builtins.KotlinBuiltIns.isAny
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.idea.caches.resolve.unsafeResolveToDescriptor
import org.jetbrains.kotlin.idea.codeInsight.DescriptorToSourceUtilsIde
import org.jetbrains.kotlin.idea.core.getDirectlyOverriddenDeclarations
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtObjectDeclaration
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode

fun goToDefinition(buffer: Buffer?, position: Position): List<Location> {
    val locations = ArrayList<Location>()
    if (buffer == null) {
        return locations
    }
    val application = ApplicationManager.getApplication()
    application.invokeAndWait {
        application.runReadAction {
            val psiFile = buffer.psiFile
            val editor = buffer.editor.editor
            val offset = editor.logicalPositionToOffset(LogicalPosition(position.line, position.character))
            val project = buffer.project
            val psiReference = psiFile.findReferenceAt(offset)
            if (psiReference == null) {
                if (psiFile.language == KotlinLanguage.INSTANCE) {
                    locations.addAll(findKotlinSuperMethod(project, psiFile.findElementAt(offset)))
                } else if (psiFile.language == JavaLanguage.INSTANCE) {
                    locations.addAll(findJavaSuperMethod(psiFile.findElementAt(offset)))
                }
                return@runReadAction
            }
            val psiElement = psiReference.resolve() ?: return@runReadAction
            val location = getLocation(psiElement)
            if (location != null) {
                locations.add(location)
            }
        }
    }

    return locations
}

private fun findJavaSuperMethod(psiElement: PsiElement?): List<Location> {
    if (psiElement == null || psiElement.parent == null || psiElement.parent !is PsiMethod) {
        return emptyList()
    }

    val psiMethod = psiElement.parent as PsiMethod
    val superSignature = SuperMethodsSearch.search(psiMethod, null, true, false).findFirst()
    val superMethod = superSignature?.method ?: return emptyList()

    val location = getLocation(superMethod) ?: return emptyList()

    return listOf(location)
}

private fun findKotlinSuperMethod(project: Project, psiElement: PsiElement?): List<Location> {
    if (psiElement == null || psiElement.parent == null) {
        return emptyList()
    }
    val declaration = PsiTreeUtil.getParentOfType<PsiElement>(psiElement.parent,
            KtNamedFunction::class.java,
            KtClass::class.java,
            KtProperty::class.java,
            KtObjectDeclaration::class.java) as KtDeclaration? ?: return emptyList()
    try {
        val descriptor = declaration.unsafeResolveToDescriptor(BodyResolveMode.PARTIAL)
        val superDeclarations = findSuperDeclarations(project, descriptor) ?: return emptyList()
        val locations = ArrayList<Location>()
        superDeclarations.forEach {
            val location = getLocation(it)
            if (location != null) {
                locations.add(location)
            }
        }
        return locations
    } catch (e: IndexNotReadyException) {
        return emptyList()
    }
}

private fun findSuperDeclarations(project: Project, descriptor: DeclarationDescriptor): List<PsiElement>? {
    val superDescriptors: Collection<DeclarationDescriptor> = when (descriptor) {
        is ClassDescriptor -> {
            val supertypes = descriptor.typeConstructor.supertypes
            val superclasses = supertypes.mapNotNull { type ->
                type.constructor.declarationDescriptor as? ClassDescriptor
            }
            ContainerUtil.removeDuplicates(superclasses)
            superclasses
        }
        is CallableMemberDescriptor -> descriptor.getDirectlyOverriddenDeclarations()
        else -> return null
    }

    return superDescriptors.mapNotNull { superDescriptor ->
        if (superDescriptor is ClassDescriptor && isAny(superDescriptor)) {
            null
        } else
            DescriptorToSourceUtilsIde.getAnyDeclaration(project, superDescriptor)
    }
}
