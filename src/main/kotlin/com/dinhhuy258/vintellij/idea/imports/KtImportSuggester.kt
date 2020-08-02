package com.dinhhuy258.vintellij.idea.imports

import com.intellij.codeInsight.ImportFilter
import com.intellij.packageDependencies.DependencyValidationManager
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptorWithVisibility
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.idea.caches.resolve.resolveToCall
import org.jetbrains.kotlin.idea.caches.resolve.util.getResolveScope
import org.jetbrains.kotlin.idea.codeInsight.DescriptorToSourceUtilsIde
import org.jetbrains.kotlin.idea.core.KotlinIndicesHelper
import org.jetbrains.kotlin.idea.core.isVisible
import org.jetbrains.kotlin.idea.project.TargetPlatformDetector
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.idea.util.CallType
import org.jetbrains.kotlin.idea.util.CallTypeAndReceiver
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.jvm.isJvm
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtEnumEntry
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtPsiUtil.isSelectorInQualified
import org.jetbrains.kotlin.psi.KtSimpleNameExpression
import org.jetbrains.kotlin.psi.psiUtil.isImportDirectiveExpression
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode

class KtImportSuggester : ImportSuggester {
    override fun collectSuggestions(element: KtSimpleNameExpression): List<String> {
        if (!element.isValid) {
            return emptyList()
        }

        val callTypeAndReceiver = getCallTypeAndReceiver(element) ?: return emptyList()
        if (callTypeAndReceiver is CallTypeAndReceiver.UNKNOWN) {
            return emptyList()
        }

        val importNames = element.mainReference.resolvesByNames ?: emptyList()
        if (importNames.isEmpty()) {
            return emptyList()
        }

        return importNames
                .flatMap { collectSuggestionsForName(element, it, callTypeAndReceiver) }
                .asSequence()
                .distinct()
                .map { it.fqNameSafe.asString() }
                .distinct()
                .toList()
    }

    private fun collectSuggestionsForName(element: KtSimpleNameExpression, name: Name, callTypeAndReceiver: CallTypeAndReceiver<*, *>): Collection<DeclarationDescriptor> {
        val nameStr = name.asString()
        if (nameStr.isEmpty()) {
            return emptyList()
        }

        val file = element.containingKtFile
        val bindingContext = element.analyze(BodyResolveMode.PARTIAL_WITH_DIAGNOSTICS)
        val searchScope = getResolveScope(file)
        val resolutionFacade = file.getResolutionFacade()

        fun isVisible(descriptor: DeclarationDescriptor): Boolean {
            if (descriptor is DeclarationDescriptorWithVisibility) {
                return descriptor.isVisible(element, callTypeAndReceiver.receiver as? KtExpression, bindingContext, resolutionFacade)
            }

            return true
        }

        val indicesHelper = KotlinIndicesHelper(resolutionFacade, searchScope, ::isVisible, file = file)

        var result = fillCandidates(element, nameStr, callTypeAndReceiver, bindingContext, indicesHelper)

        if (callTypeAndReceiver is CallTypeAndReceiver.DEFAULT) {
            val isCall = element.parent is KtCallExpression
            if (!isCall) {
                result = result.filter { it !is FunctionDescriptor }
            }
        }

        result = result.filter { ImportFilter.shouldImport(file, it.fqNameSafe.asString()) }

        return if (result.size > 1)
            reduceCandidatesBasedOnDependencyRuleViolation(result, file)
        else
            result
    }

    private fun reduceCandidatesBasedOnDependencyRuleViolation(
        candidates: Collection<DeclarationDescriptor>,
        file: PsiFile
    ): Collection<DeclarationDescriptor> {
        val project = file.project
        val validationManager = DependencyValidationManager.getInstance(project)
        return candidates.filter {
            val targetFile = DescriptorToSourceUtilsIde.getAnyDeclaration(project, it)?.containingFile
                    ?: return@filter true
            validationManager.getViolatorDependencyRules(file, targetFile).isEmpty()
        }
    }

    private fun fillCandidates(
        expression: KtSimpleNameExpression,
        name: String,
        callTypeAndReceiver: CallTypeAndReceiver<*, *>,
        bindingContext: BindingContext,
        indicesHelper: KotlinIndicesHelper
    ): List<DeclarationDescriptor> {
        val result = ArrayList<DeclarationDescriptor>()

        if (!expression.isImportDirectiveExpression() && !isSelectorInQualified(expression)) {
            val filterByCallType = callTypeAndReceiver.toFilter()

            indicesHelper.getClassesByName(expression, name).filterTo(result, filterByCallType)
            indicesHelper.getTopLevelTypeAliases { it == name }.filterTo(result, filterByCallType)
            indicesHelper.getTopLevelCallablesByName(name).filterTo(result, filterByCallType)
        }
        if (callTypeAndReceiver.callType == CallType.OPERATOR) {
            val type = expression.getCallableDescriptor()?.returnType
            if (type != null) {
                result.addAll(indicesHelper.getCallableTopLevelExtensions(callTypeAndReceiver, listOf(type), { it == name }))
            }
        }

        result.addAll(indicesHelper.getCallableTopLevelExtensions(callTypeAndReceiver, expression, bindingContext) { it == name })
        return result
    }

    private fun getCallTypeAndReceiver(element: KtSimpleNameExpression) = element.let { CallTypeAndReceiver.detect(it) }

    private fun KtExpression.getCallableDescriptor() = resolveToCall()?.resultingDescriptor

    private fun KotlinIndicesHelper.getClassesByName(
        expressionForPlatform: KtExpression,
        name: String
    ): Collection<ClassDescriptor> {
        val platform = TargetPlatformDetector.getPlatform(expressionForPlatform.containingKtFile)
        return when {
            platform.isJvm() -> getJvmClassesByName(name)
            else -> getKotlinClasses({ it == name },
                    psiFilter = { ktDeclaration -> ktDeclaration !is KtEnumEntry },
                    kindFilter = { kind -> kind != ClassKind.ENUM_ENTRY })
        }
    }

    private fun CallTypeAndReceiver<*, *>.toFilter() = { descriptor: DeclarationDescriptor ->
        callType.descriptorKindFilter.accepts(descriptor)
    }
}
