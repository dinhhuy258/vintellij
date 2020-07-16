package com.dinhhuy258.plugins.handlers

import com.google.gson.JsonElement

interface VIHandler {
    fun handle(data: JsonElement): String
}
