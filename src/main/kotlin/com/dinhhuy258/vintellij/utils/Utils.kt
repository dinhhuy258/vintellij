@file:Suppress("ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE")

package com.dinhhuy258.vintellij.utils

import com.intellij.codeEditor.JavaEditorFileSwapper
import com.intellij.codeInsight.javadoc.JavaDocInfoGenerator.generateType
import com.intellij.ide.highlighter.JavaClassFileType
import com.intellij.ide.plugins.PluginManager
import com.intellij.lang.jvm.JvmModifier
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Document
import com.intellij.openapi.util.Comparing
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiClassInitializer
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiEnumConstant
import com.intellij.psi.PsiField
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiImportStatement
import com.intellij.psi.PsiLiteralExpression
import com.intellij.psi.PsiMember
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.psi.PsiPackageStatement
import com.intellij.psi.PsiParameter
import com.intellij.psi.PsiType
import com.intellij.psi.PsiVariable
import com.intellij.psi.util.PsiTreeUtil
import org.eclipse.lsp4j.Location
import org.eclipse.lsp4j.Position
import org.eclipse.lsp4j.Range
import org.eclipse.lsp4j.SymbolKind
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.asJava.classes.KtLightClassForFacade
import org.jetbrains.kotlin.asJava.elements.KtLightMethod
import org.jetbrains.kotlin.daemon.common.experimental.log
import org.jetbrains.kotlin.idea.decompiler.builtIns.KotlinBuiltInFileType
import org.jetbrains.kotlin.idea.decompiler.navigation.SourceNavigationHelper
import org.jetbrains.kotlin.idea.refactoring.memberInfo.qualifiedClassNameForRendering
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtClassInitializer
import org.jetbrains.kotlin.psi.KtConstantExpression
import org.jetbrains.kotlin.psi.KtConstructor
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtImportDirective
import org.jetbrains.kotlin.psi.KtObjectDeclaration
import org.jetbrains.kotlin.psi.KtPackageDirective
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import org.jetbrains.kotlin.psi.KtVariableDeclaration
import org.jetbrains.kotlin.psi.psiUtil.containingClass
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject

fun Position.toOffset(doc: Document) = doc.getLineStartOffset(this.line) + this.character

fun Range.toTextRange(document: Document) =
        TextRange(
                this.start.toOffset(document),
                this.end.toOffset(document)
        )

fun offsetToPosition(document: Document, offset: Int): Position {
    if (offset == -1) {
        return Position(0, 0)
    }
    val line = document.getLineNumber(offset)
    val lineStartOffset = document.getLineStartOffset(line)
    val column = offset - lineStartOffset
    return Position(line, column)
}

fun getLocation(psiElement: PsiElement): Location? {
    val psiFile = psiElement.containingFile
    val virtualFile = psiFile.virtualFile
    if (virtualFile.fileType is JavaClassFileType && psiElement !is KtDeclaration) {
        val sourceFile = JavaEditorFileSwapper.findSourceFile(psiElement.project, virtualFile)
        if (sourceFile != null) {
            // Recalculate the text offset for source file
            val member = PsiTreeUtil.getParentOfType(psiElement, PsiMember::class.java, false)
            val position = Position(0, 0)
            var range = Range(position, position)
            if (member != null) {
                val navigationElement = member.originalElement.navigationElement
                if (navigationElement != null && Comparing.equal(navigationElement.containingFile.virtualFile, sourceFile)) {
                    val content = navigationElement.containingFile.text
                    range = getRange(content, navigationElement.textOffset)
                }
            }

            return Location(toVimJarFilePath(sourceFile.path), range)
        }

        return null
    } else if ((virtualFile.fileType is KotlinBuiltInFileType || virtualFile.fileType is JavaClassFileType) && psiElement is KtDeclaration) {
        val sourceFile = SourceNavigationHelper.getNavigationElement(psiElement).containingFile.virtualFile
        if (sourceFile != null) {
            // Recalculate the text offset for source file
            val position = Position(0, 0)
            var range = Range(position, position)
            val navigationElement = psiElement.navigationElement
            if (navigationElement != null && Comparing.equal(navigationElement.containingFile.virtualFile, sourceFile)) {
                val content = navigationElement.containingFile.text
                range = getRange(content, navigationElement.textOffset)
            }
            return Location(toVimJarFilePath(sourceFile.path), range)
        }

        return null
    }

    if (isIntellijJarFile(virtualFile.path)) {
        return Location(toVimJarFilePath(virtualFile.path), getRange(psiFile.text, psiElement.textOffset))
    }

    return Location(toVimFilePath(virtualFile.path), getRange(psiFile.text, psiElement.textOffset))
}

fun getRange(content: String, offset: Int): Range {
    val position = offsetToPosition(content, offset)
    return Range(position, position)
}

fun offsetToPosition(content: String, offset: Int): Position {
    val reader = content.reader()
    var line = 0
    var char = 0

    var find = 0
    while (find < offset) {
        val nextChar = reader.read()

        if (nextChar == -1) {
            throw RuntimeException("Reached end of file before reaching offset $offset")
        }

        find++
        char++

        if (nextChar.toChar() == '\n') {
            line++
            char = 0
        }
    }

    return Position(line, char)
}

fun PsiElement.symbolKind(): SymbolKind? =
        when (this) {
            is KtElement -> this.ktSymbolKind()

            is KtLightMethod -> if (this.containingClass is KtLightClassForFacade)
                SymbolKind.Function
            else
                SymbolKind.Method

            is PsiFile -> SymbolKind.File
            is PsiPackageStatement -> SymbolKind.Package
            is PsiImportStatement -> SymbolKind.Module
            is PsiClass -> when {
                isAnnotationType || isInterface -> SymbolKind.Interface
                isEnum -> SymbolKind.Enum
                else -> SymbolKind.Class
            }
            is PsiClassInitializer -> SymbolKind.Constructor
            is PsiMethod -> if (isConstructor) SymbolKind.Constructor else SymbolKind.Method
            is PsiEnumConstant -> SymbolKind.Enum // TODO: Replace when lsp4j has EnumMember
            is PsiField ->
                if (hasModifier(JvmModifier.STATIC) && hasModifier(JvmModifier.FINAL)) {
                    SymbolKind.Constant
                } else {
                    SymbolKind.Field
                }
            is PsiVariable -> SymbolKind.Variable
            is PsiAnnotation -> SymbolKind.Property
            is PsiLiteralExpression -> {
                (type as? PsiClassType)?.let { if (it.name == "String") SymbolKind.String else null }
                        ?: when (this.type) {
                            PsiType.BOOLEAN -> SymbolKind.Boolean
                            PsiType.BYTE, PsiType.DOUBLE, PsiType.FLOAT, PsiType.INT, PsiType.LONG, PsiType.SHORT ->
                                SymbolKind.Number
                            PsiType.CHAR -> SymbolKind.String
                            // PsiType.NULL, PsiType.VOID -> SymbolKind.Null // TODO: Add when lsp4j has Null
                            else -> SymbolKind.Constant
                        }
            }
            else -> null
        }

fun PsiElement.containerName(): String? =
        generateSequence(parent, { it.parent })
                .firstOrNull { it.symbolKind() != null && it.symbolName() != null }
                ?.symbolName()

fun PsiElement.symbolName(): String? =
        when (this) {
            is KtElement -> ktSymbolName()

            is PsiFile -> name
            is PsiPackageStatement -> packageName
            is PsiImportStatement -> qualifiedName ?: "<error>"
            is PsiClass -> name ?: qualifiedName ?: "<anonymous>"
            is PsiClassInitializer -> name ?: "<init>"
            is PsiMethod -> methodLabel(this)
            is PsiEnumConstant -> name
            is PsiField -> name
            is PsiVariable -> name ?: "<unknown>"
            is PsiAnnotation -> annotationLabel(this)
            is PsiLiteralExpression -> text

            else -> null
        }

fun PsiElement.ktSymbolKind(): SymbolKind? =
        when (this) {
            is KtFile -> SymbolKind.File
            is KtPackageDirective -> SymbolKind.Package
            is KtImportDirective -> SymbolKind.Module
            is KtClass -> when {
                isInterface() -> SymbolKind.Interface
                isEnum() -> SymbolKind.Enum
                else -> SymbolKind.Class
            }
            is KtConstructor<*> -> SymbolKind.Constructor
            is KtFunction -> when {
                isInsideCompanion() -> SymbolKind.Function
                containingClass() != null -> SymbolKind.Method
                else -> SymbolKind.Function
            }
            is KtLightMethod -> when {
                this.containingClass !is KtLightClassForFacade -> SymbolKind.Method
                else -> SymbolKind.Function
            }
            is KtProperty -> when {
                isConstant(this) -> SymbolKind.Constant
                isMember -> SymbolKind.Field
                else -> SymbolKind.Variable
            }
            is KtVariableDeclaration -> SymbolKind.Variable
            is KtParameter -> SymbolKind.Variable
            is KtAnnotationEntry -> SymbolKind.Property
            is KtObjectDeclaration -> SymbolKind.Class
            is KtConstantExpression ->
                when (this.node.elementType) {
                    KtNodeTypes.BOOLEAN_CONSTANT -> SymbolKind.Boolean
                    KtNodeTypes.INTEGER_CONSTANT, KtNodeTypes.FLOAT_CONSTANT ->
                        SymbolKind.Number
                    KtNodeTypes.STRING_TEMPLATE -> SymbolKind.String
                    else -> SymbolKind.Constant
                }
            is KtStringTemplateExpression -> SymbolKind.String
            else -> null
        }

fun PsiElement.ktSymbolName(): String? =
        when (this) {
            is KtFile -> name
            is KtPackageDirective -> qualifiedName
            is KtClass -> name ?: qualifiedClassNameForRendering()
            is KtImportDirective -> importedFqName?.asString() ?: "<error>"
            is KtClassInitializer -> name ?: "<init>"
            is KtFunction -> methodLabel(this)
            is KtProperty -> name
            is KtVariableDeclaration -> name
            is KtParameter -> name
            is KtAnnotationEntry -> annotationLabel(this)
            is KtObjectDeclaration -> name
            is KtConstantExpression -> text
            is KtStringTemplateExpression -> text

            is KtLightMethod -> methodLabel(this)

            else -> null
        }

private fun annotationLabel(annotation: PsiAnnotation): String =
        (annotation.nameReferenceElement?.text ?: annotation.qualifiedName)?.let { "@$it" }
                ?: "<unknown>"

private fun annotationLabel(annotation: KtAnnotationEntry): String =
        (annotation.typeReference?.text ?: annotation.name)?.let { "@$it" }
                ?: "<unknown>"

/** Return a method label including simplified parameter types. */
private fun methodLabel(method: PsiMethod): String =
        method.name + "(" + method.parameterList.parameters.joinToString(", ") { param ->
            methodParameterLabel(method, param)
        } + ")"

/** Return a method label including simplified parameter types. */
private fun methodLabel(method: KtFunction): String =
        method.name + "(" + method.valueParameters.joinToString(", ") { param ->
            param.typeReference?.text ?: "<unknown>"
        } + ")"

private fun methodParameterLabel(method: PsiMethod, parameter: PsiParameter): String =
        StringBuilder().apply { generateType(this, parameter.type, method, false, true) }.toString()

private fun isConstant(elt: KtProperty) = elt.modifierList?.getModifier(KtTokens.CONST_KEYWORD) != null

private fun KtDeclaration.isInsideCompanion() =
        (containingClassOrObject as? KtObjectDeclaration)?.isCompanion() == true

fun TextRange.toRange(doc: Document): Range =
        Range(
                offsetToPosition(doc, this.startOffset),
                offsetToPosition(doc, this.endOffset)
        )

fun PsiElement.toRange(document: Document) =
        ((this as? PsiNameIdentifierOwner)?.nameIdentifier ?: this).textRange.toRange(document)

fun invokeAndWait(runnable: () -> Unit) {
    ApplicationManager.getApplication().invokeAndWait {
        runnable.invoke()
    }
}

fun runWriteAction(runnable: () -> Unit) {
    ApplicationManager.getApplication().runWriteAction {
        runnable()
    }
}

fun runReadAction(runnable: () -> Unit) {
    ApplicationManager.getApplication().runReadAction {
        runnable()
    }
}
