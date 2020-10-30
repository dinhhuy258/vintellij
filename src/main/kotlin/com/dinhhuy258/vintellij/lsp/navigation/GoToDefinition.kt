package com.dinhhuy258.vintellij.lsp.navigation

import com.dinhhuy258.vintellij.comrade.buffer.SyncBuffer
import com.dinhhuy258.vintellij.lsp.utils.getLocation
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.LogicalPosition
import org.eclipse.lsp4j.Location
import org.eclipse.lsp4j.Position

fun goToDefinition(buffer: SyncBuffer?, position: Position): List<Location> {
    val locations = ArrayList<Location>()
    if (buffer == null) {
        return locations
    }
    val application = ApplicationManager.getApplication()
    application.invokeAndWait {
        application.runReadAction {
            val psiFile = buffer.psiFile
            val editor = buffer.editor.editor
            val offset = editor.logicalPositionToOffset(LogicalPosition(position.line, position.character))
            val psiReference = psiFile.findReferenceAt(offset) ?: return@runReadAction
            val psiElement = psiReference.resolve() ?: return@runReadAction
            val location = getLocation(psiElement)
            if (location != null) {
                locations.add(location)
            }
        }
    }

    return locations
}
