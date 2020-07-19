package com.dinhhuy258.vintellij.handlers

import com.google.gson.JsonElement

interface VIHandler {
    fun handle(data: JsonElement): JsonElement
}
