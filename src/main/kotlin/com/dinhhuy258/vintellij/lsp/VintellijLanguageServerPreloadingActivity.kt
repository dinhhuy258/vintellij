package com.dinhhuy258.vintellij.lsp

import com.intellij.openapi.application.PreloadingActivity
import com.intellij.openapi.progress.ProgressIndicator

class VintellijLanguageServerPreloadingActivity : PreloadingActivity() {
    override fun preload(indicator: ProgressIndicator) {
        val languageServerRunner = VintellijLanguageServerRunner()
        languageServerRunner.start()
    }
}
