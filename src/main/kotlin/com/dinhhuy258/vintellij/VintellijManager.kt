package com.dinhhuy258.vintellij

import com.dinhhuy258.vintellij.comrade.ComradeScope
import com.dinhhuy258.vintellij.comrade.core.FUN_VINTELLIJ_RESPONSE_CALLBACK
import com.dinhhuy258.vintellij.comrade.core.NvimInstance
import com.dinhhuy258.vintellij.exceptions.VIException
import com.dinhhuy258.vintellij.handlers.ImportSuggestionsHandler
import com.dinhhuy258.vintellij.handlers.OpenFileHandler
import com.dinhhuy258.vintellij.handlers.VIHandler
import com.dinhhuy258.vintellij.neovim.annotation.RequestHandler
import com.dinhhuy258.vintellij.neovim.rpc.Request
import com.google.gson.Gson
import com.google.gson.JsonElement
import kotlinx.coroutines.launch

class VintellijManager(private val nvimInstance: NvimInstance) {
    private val gson: Gson = Gson()

    private val handlers: Map<String, VIHandler> = mapOf(
            "import" to ImportSuggestionsHandler(),
            "open" to OpenFileHandler()
    )

    @RequestHandler("vintellij_handler")
    fun vintellijHandler(req: Request) {
        val map = req.args.first() as Map<*, *>
        val jsonString = gson.toJson(map)

        val response = try {
            val vintellijRequest: VintellijRequest = gson.fromJson(jsonString, VintellijRequest::class.java)
            VintellijResponse.success(processRequest(vintellijRequest), vintellijRequest.handler)
        } catch (e: VIException) {
            VintellijResponse.fail(e.message ?: "Internal server error.")
        } catch (e: Throwable) {
            VintellijResponse.fail(e.message ?: "Internal server error.")
        }

        val responseJsonString = gson.toJson(response)
        ComradeScope.launch {
            nvimInstance.client.api.callFunction(FUN_VINTELLIJ_RESPONSE_CALLBACK, listOf(responseJsonString))
        }
    }

    private fun processRequest(vintellijRequest: VintellijRequest): JsonElement {
        val handler = handlers[vintellijRequest.handler]
                ?: throw VIException("Handler ${vintellijRequest.handler} not found!!!")

        return handler.handle(vintellijRequest.data)
    }
}
