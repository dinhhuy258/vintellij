package com.dinhhuy258.vintellij.completion

import com.dinhhuy258.vintellij.buffer.Buffer
import com.dinhhuy258.vintellij.utils.invokeAndWait
import com.intellij.codeInsight.completion.CodeCompletionHandlerBase
import com.intellij.codeInsight.completion.CompletionProgressIndicator
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.codeInsight.completion.impl.CompletionServiceImpl
import com.intellij.lang.java.JavaLanguage
import com.intellij.openapi.diagnostic.Logger
import java.util.concurrent.atomic.AtomicBoolean
import org.eclipse.lsp4j.Command
import org.eclipse.lsp4j.CompletionItem
import org.eclipse.lsp4j.CompletionList
import org.eclipse.lsp4j.Position

private const val ACCEPTABLE_NUM_OF_COMPLETION_ITEMS = 5

private val IMPORT_PACKAGE_REGEX = "^([A-Za-z]{1}[A-Za-z\\d_]*\\.)+[A-Za-z][A-Za-z\\d_]*$".toRegex()

private val logger = Logger.getInstance("COMPLETION")

private val shouldStopCompletion = AtomicBoolean(false)

fun stopCompletion() {
    shouldStopCompletion.set(true)
}

fun doCompletion(buffer: Buffer?, position: Position): CompletionList {
    shouldStopCompletion.set(false)
    if (buffer == null) {
        return CompletionList(false, emptyList())
    }

    val completionList = CompletionList()
    completionList.setIsIncomplete(true)

    doAsyncComplete(buffer, position, completionList)

    return completionList
}

private fun doAsyncComplete(buffer: Buffer, position: Position, completionList: CompletionList) {
    scheduleAsyncCompletion(buffer, position)

    val indicator = CompletionServiceImpl.getCurrentCompletionProgressIndicator() ?: return
    // Wait until completion begins
    while (indicator.parameters == null && !indicator.isCanceled && !shouldStopCompletion.get()) {
        Thread.sleep(50)
    }

    getCompletionResult(buffer, indicator, completionList)
}

private fun scheduleAsyncCompletion(buffer: Buffer, position: Position) {
    invokeAndWait {
        try {
            buffer.moveCaretToPosition(position.line, position.character)
            val editor = buffer.editor.editor

            // Stop the running completion
            CompletionServiceImpl.getCurrentCompletionProgressIndicator()?.closeAndFinish(false)

            val handler = CodeCompletionHandlerBase(CompletionType.BASIC, false, false, false)
            handler.invokeCompletion(buffer.project, editor)
        } catch (t: Throwable) {
            logger.warn("Completion failed.", t)
        }
    }
}

private fun getCompletionResult(
    buffer: Buffer,
    indicator: CompletionProgressIndicator,
    completionList: CompletionList
) {
    if (indicator.isCanceled) {
        onIndicatorCompletionFinish(buffer, indicator, completionList)
        return
    }

    invokeAndWait {
        try {
            while (indicator.isRunning &&
                !indicator.isCanceled &&
                indicator.lookup.arranger.matchingItems.size < ACCEPTABLE_NUM_OF_COMPLETION_ITEMS &&
                !shouldStopCompletion.get()
            ) {
                Thread.sleep(50)
            }

            if (shouldStopCompletion.get()) {
                // Stop the running completion
                CompletionServiceImpl.getCurrentCompletionProgressIndicator()?.closeAndFinish(false)
                return@invokeAndWait
            }

            onIndicatorCompletionFinish(buffer, indicator, completionList)
        } catch (t: Throwable) {
            logger.warn("Completion failed.", t)
        }
    }
}

private fun onIndicatorCompletionFinish(
    buffer: Buffer,
    indicator: CompletionProgressIndicator,
    completionList: CompletionList
) {
    val lookupArranger = indicator.lookup?.arranger ?: return
    val matchingItems = lookupArranger.matchingItems
    if (matchingItems.isEmpty()) {
        return
    }

    val completionItems = ArrayList<CompletionItem>()
    matchingItems.forEach { lookupElement ->
        val candidate = Candidate(lookupElement)
        if (candidate.valuable) {
            var candidateLabel = candidate.itemText
            if (candidate.tailText != null) {
                candidateLabel += candidate.tailText
            }
            val completionItem = CompletionItem(candidateLabel)
            completionItem.insertText = candidate.itemText
            if (candidate.tailText != null) {
                val importCandidate = getImportCandidate(buffer, candidate.tailText!!.trim(), candidate.itemText!!)
                if (importCandidate != null) {
                    completionItem.command = Command("import", "importFix", listOf(importCandidate, buffer.path))
                }
            }

            completionItems.add(completionItem)
        }
    }

    completionList.setIsIncomplete(indicator.isRunning || shouldStopCompletion.get())
    completionList.items = completionItems
}

private fun getImportCandidate(buffer: Buffer, tailText: String, itemText: String): String? {
    if (tailText.length <= 2 || tailText[0] != '(' || tailText[tailText.length - 1] != ')') {
        return null
    }

    var importPackage = tailText.substring(
        1,
        tailText.length - 1
    ) + "." + itemText

    if (!importPackage.matches(IMPORT_PACKAGE_REGEX) || importPackage.startsWith("kotlin.")) {
        return null
    }

    importPackage = "import $importPackage"
    if (buffer.psiFile.language == JavaLanguage.INSTANCE) {
        importPackage += ";"
    }

    return importPackage
}
