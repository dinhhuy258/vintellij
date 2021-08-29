package com.dinhhuy258.vintellij.completion

import com.dinhhuy258.vintellij.buffer.Buffer
import com.dinhhuy258.vintellij.utils.invokeAndWait
import com.intellij.codeInsight.completion.CodeCompletionHandlerBase
import com.intellij.codeInsight.completion.CompletionProgressIndicator
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.codeInsight.completion.impl.CompletionServiceImpl
import com.intellij.openapi.diagnostic.Logger
import org.eclipse.lsp4j.CompletionItem
import org.eclipse.lsp4j.CompletionList
import org.eclipse.lsp4j.Position
import java.util.concurrent.atomic.AtomicBoolean

private const val ACCEPTABLE_NUM_OF_COMPLETION_ITEMS = 5

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

    getCompletionResult(indicator, completionList)
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
    indicator: CompletionProgressIndicator,
    completionList: CompletionList
) {
    if (indicator.isCanceled) {
        onIndicatorCompletionFinish(indicator, completionList)
        return
    }

    invokeAndWait {
        try {
            while (indicator.isRunning &&
                    !indicator.isCanceled &&
                    indicator.lookup.arranger.matchingItems.size < ACCEPTABLE_NUM_OF_COMPLETION_ITEMS &&
                    !shouldStopCompletion.get()) {
                Thread.sleep(50)
            }

            if (shouldStopCompletion.get()) {
                // Stop the running completion
                CompletionServiceImpl.getCurrentCompletionProgressIndicator()?.closeAndFinish(false)
                return@invokeAndWait
            }

            onIndicatorCompletionFinish(indicator, completionList)
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
            var candidateLabel = candidate.itemText
            if (candidate.tailText != null) {
                candidateLabel += candidate.tailText
            }
            val completionItem = CompletionItem(candidateLabel)
            completionItem.insertText = candidate.itemText
            completionItems.add(completionItem)
        }
    }

    completionList.setIsIncomplete(indicator.isRunning || shouldStopCompletion.get())
    completionList.items = completionItems
}
