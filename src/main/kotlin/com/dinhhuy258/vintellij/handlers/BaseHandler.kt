package com.dinhhuy258.vintellij.handlers

import com.dinhhuy258.vintellij.exceptions.VIException
import com.google.common.util.concurrent.SettableFuture
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.intellij.openapi.progress.PerformInBackgroundOption
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task.Backgroundable
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicReference

abstract class BaseHandler<RequestType, ResponseType> : VIHandler {
    companion object {
        private const val DEFAULT_TIMEOUT_IN_SECONDS = 10L
    }

    private val gson = Gson()

    protected abstract fun requestClass(): Class<RequestType>

    protected abstract fun handleInternal(request: RequestType): ResponseType

    private fun validate(request: RequestType) {
    }

    override fun handle(data: JsonElement): JsonElement {
        val responseFuture = SettableFuture.create<Any>()
        val indicatorRef: AtomicReference<ProgressIndicator> = AtomicReference()

        ProgressManager.getInstance().run(object : Backgroundable(
                null, this.javaClass.canonicalName, true, PerformInBackgroundOption.DEAF) {
            override fun run(indicator: ProgressIndicator) {
                indicatorRef.set(indicator)
                val request = gson.fromJson(data, requestClass())
                validate(request)
                try {
                    responseFuture.set(gson.toJsonTree(handleInternal(request)))
                } catch (e: Throwable) {
                    responseFuture.set(e)
                }
            }
        })

        try {
            val response = responseFuture.get(DEFAULT_TIMEOUT_IN_SECONDS, TimeUnit.SECONDS)
            if (response is Throwable) {
                throw response
            } else if (response is JsonElement) {
                return response
            }
            throw VIException("Response type is invalid")
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            throw e
        } catch (e: TimeoutException) {
            val indicator = indicatorRef.get()
            indicator?.cancel()
            throw e
        } catch (e: ExecutionException) {
            val indicator = indicatorRef.get()
            indicator?.cancel()
            throw e
        }
    }
}
