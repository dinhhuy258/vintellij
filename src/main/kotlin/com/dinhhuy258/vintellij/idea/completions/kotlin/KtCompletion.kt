package com.dinhhuy258.vintellij.idea.completions.kotlin

import com.dinhhuy258.vintellij.idea.completions.AbstractCompletion
import com.dinhhuy258.vintellij.idea.completions.CompletionKind
import com.intellij.openapi.application.ApplicationManager
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.idea.util.CallTypeAndReceiver
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtSimpleNameExpression

class KtCompletion(onSuggest: (word: String, kind: CompletionKind, menu: String) -> Unit) : AbstractCompletion(onSuggest) {
    override fun doCompletion(psiFile: PsiFile, offset: Int, prefix: String) {
        val ktFile = psiFile as KtFile
        val completionElement = ktFile.findElementAt(offset)?.parent ?: return
        val application = ApplicationManager.getApplication()

        if (completionElement is KtAnnotationEntry ||
                (completionElement is KtSimpleNameExpression &&
                        CallTypeAndReceiver.detect(completionElement) is CallTypeAndReceiver.ANNOTATION)) {
            val classNameCompletion = ClassNameCompletion(onSuggest)
            application.runReadAction {
                classNameCompletion.findAllAnnotationClasses(completionElement, prefix)
            }
        } else if (completionElement is KtSimpleNameExpression &&
                CallTypeAndReceiver.detect(completionElement) is CallTypeAndReceiver.TYPE) {
            val classNameCompletion = ClassNameCompletion(onSuggest)
            application.runReadAction {
                classNameCompletion.findAllClasses(completionElement, prefix)
            }
        }
    }
}
