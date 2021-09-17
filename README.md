# Vintellij
Make IntelliJ as a server language protocol.

## Installation

### Vim

[packer.nvim](https://github.com/wbthomason/packer.nvim)

```sh
{
  "dinhhuy258/vintellij",
  branch = "lsp",
  config = function()
    require("vintellij").setup({
      debug = false,
      common_capabilities = vim.lsp.protocol.make_client_capabilities(),
      common_on_attach = function() 
        print("your on_attach function here")
      end,
      common_on_init = function()
        print("Vintellij started ...")
      end,
      filetypes = {
        "kotlin"
      },
      lib_dirs = {
        "~/.gradle",
        "/Library/Java/JavaVirtualMachines",
      },
    })
  end,
}
```
`lib_dirs` is the list of external library sources that uses by Intellij.

### Intellij plugin

1. Modify intellij plugin version and intellij kotlin version in `build.gradle` file based on your Intellij version (default is 2020.2) (optional)
2. Run `./gradlew buildPlugin`
3. Install the vintellij plugin into Intellij. (Preferences -> Plugins -> Install Plugin from Disk...)

## Vintellij commands

- `VintellijGenDoc`: Generate kotlin doc at the current cursor. (Currently, only support Class and Function docs)

**Note:** Please move the cursor to the line of a class/function that you want to generate doc before running a command.
If your class/method contains annotations then move the cursor to the top annotation.

Eg:

```kotlin
class TestKotlin { <------------ Cursor must be in this line
}
```

```kotlin
@Controler   <------------ Cursor must be in this line
@TestAnnotation
class TestKotlin {
}
```

## Coc setup

Not supported at the moment please use branch `lsp_backup` instead.

## Start Vintellij LSP Client

Vintellij LSP Client does not start automatically, please open the project in IntelliJ same as the project in Neovim then open any Kolin or Java file in project and run command `:LspStart`

**Note:** If you disable `zip` and `zipPlugin` then the go to implementation feature will not work properly.

## Features

- Go to definition
- Go to type definition
- Go to implementation
- Go to reference
- Document symbol
- Import code action
- Document formatting
- Autocompletion
- Generate doc (Kotlin only)

## Issue

- The buffer sync between NeoVim and Intellij still have a problem
- To make it work, the Intellij must open the same project as Vim.
- Always open Intellij otherwise everything will be slow - the workarround maybe:
  - Get IntelliJ focused by having it in your secondary screen
  - Get vim transparent and putting IntelliJ behind

## Development

1. Disable Vintellij plugin from your Intellij
2. From Intellij (on Vintellij project), select Gradle panel => run `runIde` task as debug.
3. An Intellij with Vintellij plugin installed will be spawned. Open a java/kotlin project from that Intellij
4. Open the same project by vim => open a java/kotlin file => `LSP start`
5. Set any debugger from Vintelli project

