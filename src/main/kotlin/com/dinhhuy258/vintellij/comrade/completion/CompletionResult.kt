package com.dinhhuy258.vintellij.comrade.completion

abstract class CompletionResult {
    private var candidates = mutableListOf<Map<String, String>>()

    @Synchronized
    fun add(candidate: Candidate) {
        candidates.add(candidate.toMap())
    }

    @Synchronized
    protected fun retrieve(): List<Map<String, String>> {
        val result = candidates.toList()
        candidates.clear()

        return result
    }

    abstract fun toResponseArgs(): Map<Any, Any>
}

class AsyncCompletionResult : CompletionResult() {
    override fun toResponseArgs(): Map<Any, Any> {
        return mapOf("candidates" to retrieve())
    }
}

class SyncCompletionResult(@Volatile var isFinished: Boolean = false) : CompletionResult() {
    override fun toResponseArgs(): Map<Any, Any> {
        return mapOf("is_finished" to isFinished, "candidates" to retrieve())
    }
}
