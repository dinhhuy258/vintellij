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

private val log = Logger.getInstance(SyncBufferManager::class.java)

class CompletionManager(private val bufManager: SyncBufferManager) {
    companion object {
        private const val ACCEPTABLE_NUM_CANDIDATES = 20
    }

    private class Results {

        private var candidates = mutableListOf<Map<String, String>>()

        @Synchronized
        fun add(candidate: Candidate) {
            candidates.add(candidate.toMap())
        }

        @Synchronized
        private fun retrieve(): List<Map<String, String>> {
            val ret = candidates.toList()
            candidates.clear()
            return ret
        }

        fun toResponseArgs(): Map<Any, Any> {
            return mapOf("candidates" to retrieve())
        }

        companion object {
            val EMPTY = Results()
        }
    }

    @Volatile
    private var results: Results = Results.EMPTY

    @RequestHandler("comrade_complete")
    fun intellijComplete(req: Request) {
        val tmpResults = Results()
        results = tmpResults

        val map = req.args.first() as Map<*, *>
        val bufId = map["buf_id"] as Int
        val row = map["row"] as Int
        val col = map["col"] as Int

        val syncedBuf = bufManager.findBufferById(bufId) ?: throw IllegalStateException()

        try {
            scheduleAsyncCompletion(syncedBuf, row, col)
            val indicator = CompletionServiceImpl.getCurrentCompletionProgressIndicator()
            // Wait until completion begins
            while (indicator.parameters == null && results == this.results) {
                sleep(50)
            }
            getCompletionResult(syncedBuf.nvimInstance, indicator, tmpResults)
        } catch (e: Exception) {
            log.warn("Completion failed")
        }
    }

    private fun getCompletionResult(nvimInstance: NvimInstance, indicator: CompletionProgressIndicator, results: Results) {
        if (results != this.results) {
            return
        }
        ApplicationManager.getApplication().invokeLater {
            while (indicator.isRunning &&
                    !indicator.isCanceled &&
                    indicator.lookup.arranger.matchingItems.size < ACCEPTABLE_NUM_CANDIDATES &&
                    results == this.results) {
                sleep(50)
            }
            if (results == this.results) {
                onIndicatorCompletionFinish(indicator, nvimInstance, results)
            }
        }
    }

    private fun onIndicatorCompletionFinish(indicator: CompletionProgressIndicator, nvimInstance: NvimInstance, results: Results) {
        val lookupArranger = indicator.lookup?.arranger ?: return
        val matchingItems = lookupArranger.matchingItems
        if (matchingItems.isEmpty()) {
            return
        }

        matchingItems.forEach { lookupElement ->
            val candidate = Candidate(lookupElement)
            if (candidate.valuable) {
                results.add(candidate)
            }
        }

        if (results == this.results) {
            ComradeScope.launch {
                nvimInstance.client.api.callFunction(FUN_COC_AUTOCOMPLETE_CALLBACK,
                        listOf(results.toResponseArgs()))
            }
        }
    }

    private fun scheduleAsyncCompletion(syncedBuf: SyncBuffer, row: Int, col: Int) {
        ApplicationManager.getApplication().invokeAndWait {
            syncedBuf.moveCaretToPosition(row, col)
            val editor = syncedBuf.editor.editor

            // Stop the running completion
            CompletionServiceImpl.getCurrentCompletionProgressIndicator()?.closeAndFinish(false)
            val handler = CodeCompletionHandlerBase(CompletionType.BASIC, false, false, false)
            handler.invokeCompletion(syncedBuf.project, editor)
        }
    }
}
