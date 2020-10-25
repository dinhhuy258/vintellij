package com.dinhhuy258.vintellij.comrade

import com.dinhhuy258.vintellij.comrade.core.NvimInfo
import com.dinhhuy258.vintellij.comrade.core.NvimInstanceManager
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.Separator
import com.intellij.openapi.actionSystem.ToggleAction

class NvimInstanceAction(nvimInfo: NvimInfo) : ToggleAction() {

    init {
        this.templatePresentation.text = nvimInfo.projectName
        this.templatePresentation.description = nvimInfo.projectName
    }

    override fun setSelected(e: AnActionEvent, state: Boolean) {
    }

    override fun isSelected(e: AnActionEvent): Boolean {
        return true
    }
}

class MainAction : ActionGroup() {
    override fun getChildren(e: AnActionEvent?): Array<AnAction> {
        val list = NvimInstanceManager.list()
        val ret = list.map {
            NvimInstanceAction(it.first) as AnAction }.toMutableList()
        ret.add(Separator())
        return ret.toTypedArray()
    }
}
