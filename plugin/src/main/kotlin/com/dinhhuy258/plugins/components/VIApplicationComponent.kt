package com.dinhhuy258.plugins.components

import com.intellij.openapi.components.ApplicationComponent

class VIApplicationComponent: ApplicationComponent {
    override fun initComponent() {
        println("Application start")
    }
}