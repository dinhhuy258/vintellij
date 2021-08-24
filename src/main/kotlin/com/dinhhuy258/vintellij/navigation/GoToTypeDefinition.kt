package com.dinhhuy258.vintellij.navigation

import com.dinhhuy258.vintellij.buffer.Buffer
import com.dinhhuy258.vintellij.utils.getLocation
import com.intellij.codeInsight.navigation.actions.GotoTypeDeclarationAction
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.LogicalPosition
import com.intellij.openapi.util.Ref
import org.eclipse.lsp4j.Location
import org.eclipse.lsp4j.Position

fun goToTypeDefinition(buffer: Buffer?, position: Position): List<Location> {
    if (buffer == null) {
        return emptyList()
    }

    val locationsRef = Ref<List<Location>>()
    val application = ApplicationManager.getApplication()
    application.invokeAndWait {
        application.runReadAction {
            val editor = buffer.editor.editor
            val offset = editor.logicalPositionToOffset(LogicalPosition(position.line, position.character))

            val symbolTypes = GotoTypeDeclarationAction.findSymbolTypes(editor, offset) ?: return@runReadAction
            val locations = symbolTypes.mapNotNull {
                getLocation(it) ?: return@mapNotNull null
            }

            locationsRef.set(locations)
        }
    }

    return locationsRef.get()
}
