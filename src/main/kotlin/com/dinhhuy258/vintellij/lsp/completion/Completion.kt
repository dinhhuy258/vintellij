package com.dinhhuy258.vintellij.lsp.completion

import com.dinhhuy258.vintellij.comrade.buffer.SyncBuffer
import com.dinhhuy258.vintellij.comrade.completion.Candidate
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
private var currentCompletionList = CompletionList()

private val logger = Logger.getInstance("COMPLETION")

fun doCompletion(buffer: SyncBuffer?, position: Position): CompletionList {
    if (buffer == null) {
        return CompletionList(false, emptyList())
    }

    val completionList = CompletionList()
    completionList.setIsIncomplete(false)
    currentCompletionList = completionList

    doAsyncComplete(buffer, position, completionList)

    return completionList
}

@Synchronized
private fun doAsyncComplete(buffer: SyncBuffer, position: Position, completionList: CompletionList) {
    scheduleAsyncCompletion(buffer, position)

    val indicator = CompletionServiceImpl.getCurrentCompletionProgressIndicator() ?: return
    // Wait until completion begins
    while (indicator.parameters == null && completionList == currentCompletionList) {
        Thread.sleep(50)
    }

    getCompletionResult(indicator, completionList)
}

private fun scheduleAsyncCompletion(buffer: SyncBuffer, position: Position) {
    ApplicationManager.getApplication().invokeAndWait {
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
    indicator: CompletionProgressIndicator,
    completionList: CompletionList
) {
    if (completionList != currentCompletionList) {
        return
    }

    ApplicationManager.getApplication().invokeAndWait {
        try {
            while (indicator.isRunning &&
                    !indicator.isCanceled &&
                    indicator.lookup.arranger.matchingItems.size < ACCEPTABLE_NUM_OF_COMPLETION_ITEMS &&
                    completionList == currentCompletionList) {
                Thread.sleep(50)
            }

            if (completionList == currentCompletionList) {
                onIndicatorCompletionFinish(indicator, completionList)
            }
        } catch (t: Throwable) {
            logger.warn("Completion failed.", t)
        }
    }
}

private fun onIndicatorCompletionFinish(
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
            completionItems.add(CompletionItem(candidate.itemText))
        }
    }

    if (completionList == currentCompletionList) {
        completionList.setIsIncomplete(indicator.isRunning)
        completionList.items = completionItems
    }
}
