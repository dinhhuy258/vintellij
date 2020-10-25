package com.dinhhuy258.vintellij.comrade

import com.dinhhuy258.vintellij.comrade.core.NvimInfo
import com.dinhhuy258.vintellij.comrade.core.NvimInstanceManager
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.Separator
import com.intellij.openapi.actionSystem.ToggleAction

class NvimInstanceAction(private val nvimInfo: NvimInfo, private val connected: Boolean) : ToggleAction() {

    init {
        this.templatePresentation.text = nvimInfo.address
        this.templatePresentation.description = nvimInfo.address
    }

    override fun setSelected(e: AnActionEvent, state: Boolean) {
        if (state) {
            NvimInstanceManager.connect(nvimInfo)
        } else {
            NvimInstanceManager.disconnect(nvimInfo)
        }
    }

    override fun isSelected(e: AnActionEvent): Boolean {
        return connected
    }
}

class MainAction : ActionGroup() {
    override fun getChildren(e: AnActionEvent?): Array<AnAction> {
        val list = NvimInstanceManager.list()
        val ret = list.map {
            NvimInstanceAction(it.first, it.second) as AnAction }.toMutableList()
        ret.add(Separator())
        return ret.toTypedArray()
    }
}
