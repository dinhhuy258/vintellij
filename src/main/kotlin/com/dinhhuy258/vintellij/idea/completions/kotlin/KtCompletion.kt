package com.dinhhuy258.vintellij.idea.completions.kotlin

import com.dinhhuy258.vintellij.idea.IdeaUtils
import com.dinhhuy258.vintellij.idea.completions.AbstractCompletion
import com.dinhhuy258.vintellij.idea.completions.CompletionKind
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.util.TextRange
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiModifier
import org.jetbrains.kotlin.idea.debugger.sequence.psi.resolveType
import org.jetbrains.kotlin.idea.refactoring.fqName.fqName
import org.jetbrains.kotlin.idea.references.AbstractKtReference
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.idea.util.CallTypeAndReceiver
import org.jetbrains.kotlin.nj2k.postProcessing.type
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtSimpleNameExpression
import org.jetbrains.kotlin.psi.KtValueArgumentList
import org.jetbrains.kotlin.types.KotlinType

class KtCompletion(onSuggest: (word: String, kind: CompletionKind, menu: String) -> Unit) :
    AbstractCompletion(onSuggest) {
    override fun doCompletion(psiFile: PsiFile, offset: Int, prefix: String) {
        val ktFile = psiFile as KtFile
        val application = ApplicationManager.getApplication()
        application.runReadAction {
            if (prefix.isEmpty()) {
                val document = PsiDocumentManager.getInstance(IdeaUtils.getProject()).getDocument(psiFile) ?: return@runReadAction
                val lineNumber = document.getLineNumber(offset - 1)
                val line = document.getText(TextRange(document.getLineStartOffset(lineNumber), document.getLineEndOffset(lineNumber)))
                var index = line.length - 1
                if (index >= 0 && line[index] == '.') {
                    var element = ktFile.findElementAt(offset - 2)?.parent ?: return@runReadAction
                    if (element is KtValueArgumentList) {
                        element = element.parent
                    }

                    if (element is KtExpression) {
                        val type = resolveType(element) ?: return@runReadAction
                        completeMethods(element, type, prefix)
                    }
                } else {
                    var newOffset = offset
                    while (index >= 0 && (line[index] == ' ' || line[index] == '=')) {
                        --index
                        --newOffset
                    }
                    if (index == 0) {
                        return@runReadAction
                    }
                    val element = ktFile.findElementAt(newOffset)?.parent ?: return@runReadAction
                    if (element is KtExpression) {
                        completeExpression(element, element, prefix)
                    }
                }
                return@runReadAction
            }

            val completionElement = ktFile.findElementAt(offset)?.parent ?: return@runReadAction
            val callTypeAndReceiver = getCallTypeAndReceiver(completionElement)
            if (callTypeAndReceiver is CallTypeAndReceiver.ANNOTATION) {
                val classNameCompletion = ClassNameCompletion(onSuggest)
                classNameCompletion.findAllAnnotationClasses(completionElement, prefix)
            } else if (callTypeAndReceiver is CallTypeAndReceiver.TYPE) {
                val classNameCompletion = ClassNameCompletion(onSuggest)
                classNameCompletion.findAllClasses(completionElement, prefix)
            } else if (callTypeAndReceiver is CallTypeAndReceiver.DOT) {
                val receiver = callTypeAndReceiver.receiver
                val type = resolveType(receiver) ?: return@runReadAction
                completeMethods(completionElement, type, prefix)
            } else if (completionElement is KtElement) {
                val reference = completionElement.mainReference
                if (reference != null && reference is AbstractKtReference<*>) {
                    val expression = reference.expression.parent
                    if (expression is KtExpression) {
                        completeExpression(completionElement, expression, prefix)
                    }
                }
            }
        }
    }

    private fun resolveType(expression: KtExpression): KotlinType? {
        return try {
            expression.resolveType()
        } catch (e: Throwable) {
            null
        }
    }

    private fun completeMethods(context: PsiElement, type: KotlinType, prefix: String) {
        val scope = context.resolveScope
        val typeFqName = type.fqName?.asString() ?: return
        val psiClass = JavaPsiFacade.getInstance(IdeaUtils.getProject()).findClass(typeFqName, scope) ?: return
        psiClass.allMethods.forEach { psiMethod ->
            val methodName = psiMethod.name
            if (psiMethod.isConstructor ||
                psiMethod.modifierList.hasModifierProperty(PsiModifier.PRIVATE) ||
                !methodName.startsWith(prefix)
            ) {
                return@forEach
            }
            val menu = StringBuilder()
            menu.append(psiMethod.returnType?.canonicalText ?: "void").append(" ")
            menu.append(methodName).append("(")
            var firstParameter = true
            psiMethod.parameterList.parameters.forEach { psiParameter ->
                if (!firstParameter) {
                    menu.append(", ")
                }
                menu.append(psiParameter.type.canonicalText).append(" ")
                menu.append(psiParameter.name)
                firstParameter = false
            }
            menu.append(")")
            val suggestName = if (firstParameter) {
                "$methodName()"
            } else {
                "$methodName("
            }
            onSuggest(suggestName, CompletionKind.FUNCTION, menu.toString())
        }
    }

    private fun completeExpression(context: KtElement, expression: KtExpression, prefix: String) {
        if (expression is KtBinaryExpression) {
            val leftExpression = expression.left ?: return
            val type = resolveType(leftExpression) ?: return
            val classNameCompletion = ClassNameCompletion(onSuggest)
            classNameCompletion.findAllClassesMatchType(context, type, prefix)
        } else if (expression is KtProperty || expression is KtDeclaration) {
            val type = if (expression is KtProperty) {
                expression.type()
            } else {
                (expression as KtDeclaration).type()
            } ?: return
            val classNameCompletion = ClassNameCompletion(onSuggest)
            classNameCompletion.findAllClassesMatchType(context, type, prefix)
        }
    }

    private fun getCallTypeAndReceiver(context: PsiElement): CallTypeAndReceiver<*, *>? {
        if (context is KtSimpleNameExpression) {
            return CallTypeAndReceiver.detect(context)
        }

        return null
    }
}
