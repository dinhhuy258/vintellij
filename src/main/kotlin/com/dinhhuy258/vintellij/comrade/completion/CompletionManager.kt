package com.dinhhuy258.vintellij.comrade.completion

import com.dinhhuy258.vintellij.comrade.ComradeScope
import com.dinhhuy258.vintellij.comrade.buffer.SyncBuffer
import com.dinhhuy258.vintellij.comrade.buffer.SyncBufferManager
import com.dinhhuy258.vintellij.comrade.core.FUN_COC_AUTOCOMPLETE_CALLBACK
import com.dinhhuy258.vintellij.comrade.core.NvimInstance
import com.dinhhuy258.vintellij.neovim.annotation.RequestHandler
import com.dinhhuy258.vintellij.neovim.rpc.Request
import com.intellij.codeInsight.completion.CodeCompletionHandlerBase
import com.intellij.codeInsight.completion.CompletionProgressIndicator
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.codeInsight.completion.impl.CompletionServiceImpl
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import java.lang.Thread.sleep
import kotlinx.coroutines.launch

class CompletionManager(private val nvimInstance: NvimInstance, private val bufManager: SyncBufferManager) {
    private val logger = Logger.getInstance(CompletionManager::class.java)

    @Volatile
    private var completionResult: CompletionResult = AsyncCompletionResult()

    @RequestHandler("comrade_complete")
    fun intellijComplete(request: Request): Map<Any, Any> {
        val requestArgs = request.args.first() as Map<*, *>
        if (requestArgs["new_request"] as Boolean) {
            val tempCompletionResult = SyncCompletionResult()
            completionResult = tempCompletionResult
            ApplicationManager.getApplication().invokeLater {
                try {
                    doSyncComplete(request, tempCompletionResult)
                } catch (t: Throwable) {
                    logger.warn("Completion failed.", t)
                } finally {
                    tempCompletionResult.isFinished = true
                }
            }
        }

        return completionResult.toResponseArgs()
    }

    @RequestHandler("comrade_async_complete")
    fun intellijAsyncComplete(request: Request) {
        val tempCompletionResult = AsyncCompletionResult()
        completionResult = tempCompletionResult

        val requestArgs = request.args.first() as Map<*, *>
        val bufId = requestArgs["buf_id"] as Int
        val row = requestArgs["row"] as Int
        val col = requestArgs["col"] as Int
        var acceptableNumOfCandidates = requestArgs["acceptable_num_candidates"] as Int
        if (acceptableNumOfCandidates <= 0) {
            acceptableNumOfCandidates = Int.MAX_VALUE
        }

        val syncedBuf = bufManager.findBufferById(bufId) ?: throw IllegalStateException()

        doAsyncComplete(syncedBuf, row, col, acceptableNumOfCandidates, tempCompletionResult)
    }

    private fun doSyncComplete(request: Request, completionResult: SyncCompletionResult) {
        val map = request.args.first() as Map<*, *>
        val bufId = map["buf_id"] as Int
        val row = map["row"] as Int
        val col = map["col"] as Int

        val syncedBuf = bufManager.findBufferById(bufId) ?: throw IllegalStateException()
        syncedBuf.moveCaretToPosition(row, col)
        // We need the real editor instead of the delegate here since the completion needs caret.
        val editor = syncedBuf.editor.editor

        // Stop the running completion
        CompletionServiceImpl.getCurrentCompletionProgressIndicator()?.closeAndFinish(false)
        val handler = CodeCompletionHandler {
            it.forEach { lookupElement ->
                val candidate = Candidate(lookupElement)
                if (candidate.valuable) {
                    completionResult.add(candidate)
                }
            }
        }

        handler.invokeCompletion(syncedBuf.project, editor)
    }

    private fun doAsyncComplete(
        syncedBuf: SyncBuffer,
        row: Int,
        col: Int,
        acceptableNumOfCandidates: Int,
        completionResult: AsyncCompletionResult
    ) {
        scheduleAsyncCompletion(syncedBuf, row, col)

        val indicator = CompletionServiceImpl.getCurrentCompletionProgressIndicator() ?: return
        // Wait until completion begins
        while (indicator.parameters == null && completionResult == this.completionResult) {
            sleep(50)
        }

        getCompletionResult(indicator, acceptableNumOfCandidates, completionResult)
    }

    private fun scheduleAsyncCompletion(syncedBuf: SyncBuffer, row: Int, col: Int) {
        ApplicationManager.getApplication().invokeAndWait {
            try {
                syncedBuf.moveCaretToPosition(row, col)
                val editor = syncedBuf.editor.editor

                // Stop the running completion
                CompletionServiceImpl.getCurrentCompletionProgressIndicator()?.closeAndFinish(false)

                val handler = CodeCompletionHandlerBase(CompletionType.BASIC, false, false, false)
                handler.invokeCompletion(syncedBuf.project, editor)
            } catch (t: Throwable) {
                logger.warn("Completion failed.", t)
            }
        }
    }

    private fun getCompletionResult(
        indicator: CompletionProgressIndicator,
        acceptableNumOfCandidates: Int,
        completionResult: AsyncCompletionResult
    ) {
        if (completionResult != this.completionResult) {
            return
        }
        ApplicationManager.getApplication().invokeLater {
            try {
                while (indicator.isRunning &&
                        !indicator.isCanceled &&
                        indicator.lookup.arranger.matchingItems.size < acceptableNumOfCandidates &&
                        completionResult == this.completionResult) {
                    sleep(50)
                }
                if (completionResult == this.completionResult) {
                    onIndicatorCompletionFinish(indicator, completionResult)
                }
            } catch (t: Throwable) {
                logger.warn("Completion failed.", t)
            }
        }
    }

    private fun onIndicatorCompletionFinish(
        indicator: CompletionProgressIndicator,
        completionResult: AsyncCompletionResult
    ) {
        val lookupArranger = indicator.lookup?.arranger ?: return
        val matchingItems = lookupArranger.matchingItems
        if (matchingItems.isEmpty()) {
            return
        }

        matchingItems.forEach { lookupElement ->
            val candidate = Candidate(lookupElement)
            if (candidate.valuable) {
                completionResult.add(candidate)
            }
        }

        if (completionResult == this.completionResult) {
            ComradeScope.launch {
                nvimInstance.client.api.callFunction(FUN_COC_AUTOCOMPLETE_CALLBACK,
                        listOf(completionResult.toResponseArgs()))
            }
        }
    }
}
