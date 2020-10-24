package com.dinhhuy258.vintellij.lsp.completion

import com.dinhhuy258.vintellij.comrade.completion.Candidate
import com.dinhhuy258.vintellij.lsp.buffer.Buffer
import com.intellij.codeInsight.completion.CodeCompletionHandlerBase
import com.intellij.codeInsight.completion.CompletionProgressIndicator
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.codeInsight.completion.impl.CompletionServiceImpl
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import org.eclipse.lsp4j.CompletionItem
import org.eclipse.lsp4j.CompletionList
import org.eclipse.lsp4j.Position

private const val ACCEPTABLE_NUM_OF_COMPLETION_ITEMS = 15

@Volatile
private var currentCompletionItems = ArrayList<CompletionItem>()

private val logger = Logger.getInstance("COMPLETION")

fun doCompletion(buffer: Buffer?, position: Position): CompletionList {
    if (buffer == null) {
        return CompletionList(false, emptyList())
    }

    val completionItems = ArrayList<CompletionItem>()
    currentCompletionItems = completionItems

    doAsyncComplete(buffer, position, completionItems)

    return CompletionList(true, completionItems)
}

@Synchronized
private fun doAsyncComplete(buffer: Buffer, position: Position, completionItems: ArrayList<CompletionItem>) {
    scheduleAsyncCompletion(buffer, position)

    val indicator = CompletionServiceImpl.getCurrentCompletionProgressIndicator() ?: return
    // Wait until completion begins
    while (indicator.parameters == null && completionItems == currentCompletionItems) {
        Thread.sleep(50)
    }

    getCompletionResult(indicator, completionItems)
}

private fun scheduleAsyncCompletion(buffer: Buffer, position: Position) {
    ApplicationManager.getApplication().invokeAndWait {
        try {
            buffer.moveCaretToPosition(position.line, position.character)
            val editor = buffer.editor.editor

            // Stop the running completion
            CompletionServiceImpl.getCurrentCompletionProgressIndicator()?.closeAndFinish(false)

            val handler = CodeCompletionHandlerBase(CompletionType.BASIC, false, false, false)
            handler.invokeCompletion(buffer.getProject(), editor)
        } catch (t: Throwable) {
            logger.warn("Completion failed.", t)
        }
    }
}

private fun getCompletionResult(
    indicator: CompletionProgressIndicator,
    completionItems: ArrayList<CompletionItem>
) {
    if (completionItems != currentCompletionItems) {
        return
    }

    ApplicationManager.getApplication().invokeAndWait {
        try {
            while (indicator.isRunning &&
                    !indicator.isCanceled &&
                    indicator.lookup.arranger.matchingItems.size < ACCEPTABLE_NUM_OF_COMPLETION_ITEMS &&
                    completionItems == currentCompletionItems) {
                Thread.sleep(50)
            }

            if (completionItems == currentCompletionItems) {
                onIndicatorCompletionFinish(indicator, completionItems)
            }
        } catch (t: Throwable) {
            logger.warn("Completion failed.", t)
        }
    }
}

private fun onIndicatorCompletionFinish(
    indicator: CompletionProgressIndicator,
    completionResult: ArrayList<CompletionItem>
) {
    val lookupArranger = indicator.lookup?.arranger ?: return
    val matchingItems = lookupArranger.matchingItems
    if (matchingItems.isEmpty()) {
        return
    }

    matchingItems.forEach { lookupElement ->
        val candidate = Candidate(lookupElement)
        if (candidate.valuable) {
            completionResult.add(CompletionItem(candidate.itemText))
        }
    }
}
