package com.dinhhuy258.vintellij.comrade.completion

import com.intellij.codeInsight.completion.CodeCompletionHandlerBase
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.codeInsight.completion.impl.CompletionServiceImpl
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import kotlinx.coroutines.launch
import com.dinhhuy258.vintellij.comrade.ComradeScope
import com.dinhhuy258.vintellij.comrade.buffer.SyncBufferManager
import com.dinhhuy258.vintellij.comrade.core.FUN_COC_AUTOCOMPLETE_CALLBACK
import com.dinhhuy258.vintellij.neovim.annotation.RequestHandler
import com.dinhhuy258.vintellij.neovim.rpc.Request
import java.lang.Thread.sleep

private val log = Logger.getInstance(SyncBufferManager::class.java)

class CompletionManager(private val bufManager: SyncBufferManager) {
    private class Results {

        private var candidates = mutableListOf<Map<String, String>>()

        @Synchronized
        fun add(candidate: Candidate) {
            candidates.add(candidate.toMap())
        }

        @Synchronized
        private fun retrieve() : List<Map<String, String>> {
            val ret = candidates.toList()
            candidates.clear()
            return ret
        }

        fun toResponseArgs() : Map<Any, Any> {
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
        ApplicationManager.getApplication().invokeLater {
            try {
                doComplete(req, tmpResults)
            }
            catch (t: Throwable) {
                log.warn("Completion failed.", t)
            }
        }
    }

    private fun doComplete(req: Request, results: Results) {
        val map = req.args.first() as Map<*, *>
        val bufId = map["buf_id"] as Int
        val row = map["row"] as Int
        val col = map["col"] as Int

        val syncedBuf = bufManager.findBufferById(bufId) ?: throw IllegalStateException()
        syncedBuf.moveCaretToPosition(row, col)
        // We need the real editor instead of the delegate here since the completion needs caret.
        val editor = syncedBuf.editor.editor

        // Stop the running completion
        CompletionServiceImpl.getCurrentCompletionProgressIndicator()?.closeAndFinish(false)

        val handler =
                CodeCompletionHandlerBase(CompletionType.BASIC, false, false, false)

        handler.invokeCompletion(syncedBuf.project, editor)

        val indicator = CompletionServiceImpl.getCurrentCompletionProgressIndicator() ?: return
        while(indicator.isRunning && !indicator.isCanceled && results == this.results) {
            sleep(100)
        }

        if (results != this.results) {
            return
        }

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

        val nvimInstance = syncedBuf.nvimInstance
        ComradeScope.launch {
            nvimInstance.client.api.callFunction(FUN_COC_AUTOCOMPLETE_CALLBACK,
                    listOf(results.toResponseArgs()))
        }
    }
}
