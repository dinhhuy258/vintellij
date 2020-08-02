package com.dinhhuy258.vintellij.idea.completions.kotlin

import com.dinhhuy258.vintellij.idea.IdeaUtils
import com.dinhhuy258.vintellij.idea.completions.AbstractCompletion
import com.dinhhuy258.vintellij.idea.completions.CompletionKind
import com.intellij.openapi.application.ApplicationManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.idea.completion.CompletionBindingContextProvider
import org.jetbrains.kotlin.idea.references.AbstractKtReference
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.idea.util.CallTypeAndReceiver
import org.jetbrains.kotlin.nj2k.postProcessing.type
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtSimpleNameExpression
import org.jetbrains.kotlin.resolve.calls.callUtil.getType

class KtCompletion(onSuggest: (word: String, kind: CompletionKind, menu: String) -> Unit) : AbstractCompletion(onSuggest) {
    override fun doCompletion(psiFile: PsiFile, offset: Int, prefix: String) {
        val ktFile = psiFile as KtFile
        val completionElement = ktFile.findElementAt(offset)?.parent ?: return
        val application = ApplicationManager.getApplication()

        val callTypeAndReceiver = getCallTypeAndReceiver(completionElement)

        if (callTypeAndReceiver is CallTypeAndReceiver.ANNOTATION) {
            val classNameCompletion = ClassNameCompletion(onSuggest)
            application.runReadAction {
                classNameCompletion.findAllAnnotationClasses(completionElement, prefix)
            }
        }
        else if (callTypeAndReceiver is CallTypeAndReceiver.TYPE) {
            val classNameCompletion = ClassNameCompletion(onSuggest)
            application.runReadAction {
                classNameCompletion.findAllClasses(completionElement, prefix)
            }
        }
        else if (completionElement is KtElement) {
            val reference = completionElement.mainReference
            val bindingContext =
                    CompletionBindingContextProvider.getInstance(IdeaUtils.getProject())
                            .getBindingContext(completionElement, completionElement.containingKtFile.getResolutionFacade())
            if (reference != null && reference is AbstractKtReference<*>) {
                val expression = reference.expression
                val parent = expression.parent
                if (parent is KtBinaryExpression) {
                    val leftExpression = parent.left ?: return
                    val type = leftExpression.getType(bindingContext) ?: return
                    val classNameCompletion = ClassNameCompletion(onSuggest)
                    application.runReadAction {
                        classNameCompletion.findAllClassesMatchType(completionElement, type, prefix)
                    }
                }
                else if (parent is KtProperty) {
                    val type = parent.type() ?: return
                    val classNameCompletion = ClassNameCompletion(onSuggest)
                    application.runReadAction {
                        classNameCompletion.findAllClassesMatchType(completionElement, type, prefix)
                    }
                }
            }
        }
    }

    private fun getCallTypeAndReceiver(context: PsiElement): CallTypeAndReceiver<*, *>? {
        if (context is KtSimpleNameExpression) {
            return CallTypeAndReceiver.detect(context)
        }

        return null
    }
}
