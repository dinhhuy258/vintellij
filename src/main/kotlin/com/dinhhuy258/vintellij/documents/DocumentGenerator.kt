package com.dinhhuy258.vintellij.documents

import com.dinhhuy258.vintellij.buffer.Buffer
import com.dinhhuy258.vintellij.documents.kotlin.ClassKDocGenerator
import com.dinhhuy258.vintellij.documents.kotlin.NamedFunctionKDocGenerator
import com.dinhhuy258.vintellij.utils.invokeAndWait
import com.dinhhuy258.vintellij.utils.offsetToPosition
import com.dinhhuy258.vintellij.utils.runReadAction
import com.intellij.psi.PsiElement
import org.eclipse.lsp4j.Position
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtClassBody
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.psiUtil.startOffset

fun generateDoc(buffer: Buffer?, position: Position): String? {
    if (buffer == null || buffer.psiFile !is KtFile) {
        return null
    }

    var doc: String? = null

    invokeAndWait {
        runReadAction {
            val psiElements = ArrayList<PsiElement>()

            buffer.psiFile.children.forEach {
                psiElements.addAll(
                    when (it) {
                        is KtClass -> {
                            psiElements.add(it)
                            getClasses(it)
                        }
                        is KtNamedFunction -> {
                            psiElements.add(it)
                            getFunctions(it)
                        }
                        else -> arrayListOf()
                    }
                )
            }

            val psiElement = psiElements.firstOrNull {
                val elementPosition = offsetToPosition(buffer.document, it.startOffset)
                elementPosition.line == position.line
            } ?: return@runReadAction

            doc = when (psiElement) {
                is KtNamedFunction -> NamedFunctionKDocGenerator(psiElement)
                is KtClassOrObject -> ClassKDocGenerator(psiElement)
                else -> null
            }?.generate()
        }
    }

    return doc
}

private fun getClasses(ktClass: PsiElement): List<PsiElement> {
    val elements = ArrayList<PsiElement>(1)
    ktClass.children.forEach {
        elements.addAll(
            when (it) {
                is KtClass -> {

                    elements.add(it)
                    getClasses(it)
                }
                is KtClassBody -> getClasses(it)
                is KtNamedFunction -> {

                    elements.add(it)
                    getFunctions(it)
                }
                is KtProperty -> arrayListOf(it)
                else -> arrayListOf()
            }
        )
    }

    return elements.toList()
}

private fun getFunctions(ktNamedFunction: KtNamedFunction): List<PsiElement> {
    val elements = ArrayList<PsiElement>(1)
    ktNamedFunction.children.forEach {
        elements.addAll(
            when (it) {
                is KtClassOrObject -> {
                    elements.add(it)
                    getClasses(it)
                }
                is KtNamedFunction -> {
                    elements.add(it)
                    getFunctions(it)
                }
                is KtBlockExpression -> {
                    getClasses(it)
                }
                else -> arrayListOf()
            }
        )
    }
    return elements.toList()
}
