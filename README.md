# Vintellij
Make Intellij as a server language protocol.

## Installation

### Vim

#### Dependencies
- [fzf](https://github.com/junegunn/fzf.vim)

#### Intall

Plug 'dinhhuy258/vintellij'

### Intellij plugin

1. Import the project into Intellij
2. Run `gradle buildPlugin`
3. Install your plugin into Intellij. (Preferences -> Plugins -> Install Plugin from Disk...)

## Vim functions

- `vintellij#SuggestImports()`: suggest a list of importable classes for an element at the current cursor

- `vintellij#GoToDefinition()`: go to the position where an element at the current cursor is defined

- `vintellij#OpenFile()`: open the current file in Intellij

- `vintellij#RefreshFile()`: tell IntelliJ to refresh the VirtualFile element otherwise the `suggest imports` and `go to definition` features will not work correctly. This method will be called automatically each time the java/kotlin buffer is saved to file. If you want to disable it, set variable `g:vintellij_refresh_on_save` to 0 (It may cause the problem on the `suggest imports` and `go to definition` features)

- `vintellij#HealthCheck()`: check if the plugin server is working or not. This method will be called automatically each time the java/kotlin buffer is loaded. If you want to disable it, set variable `g:vintellij_health_check_on_load` to 0.

## Features

| Name | Kotlin | Java |
| ---- | ------ | ---- |
| Go to definition | [x] | [x] |
| Suggest imports | [x] | [] |
| Find references | [] | [] |
| Auto complete | [] | [] |

## Issues

- To make it work, the Intellij must open the same project as Vim.
- Saving file before using `suggest imports`, `go to definition`... features otherwise the features will not work correctly.
- Always open Intellij otherwise everything will be slow.
