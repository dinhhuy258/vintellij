# Vintellij
Make IntelliJ as a server language protocol.

## Installation

### Vim

[vim plug](https://github.com/junegunn/vim-plug)

```sh
Plug 'dinhhuy258/vintellij', {'branch': 'lsp'}
```

[packer.nvim](https://github.com/wbthomason/packer.nvim)

```sh
{
  "dinhhuy258/vintellij",
  branch = "lsp",
  ft = { "kotlin" },
  config = function()
    require("plugins.vintellij").setup()
  end,
}
```

### Intellij plugin

1. Import the project into Intellij
2. Modify intellij plugin version and intellij kotlin version in `build.gradle` file based on your version of Intellij
3. Run `./gradlew ktlinFormat`
4. Install your plugin into Intellij. (Preferences -> Plugins -> Install Plugin from Disk...)

## Run IntelliJ in headless mode

Create the following script file `vintellj.sh`

```sh
#!/bin/sh

IDE_BIN_HOME="/Applications/IntelliJ IDEA CE.app/Contents/MacOS"
exec "$IDE_BIN_HOME/idea" vintellij-inspect "$@"
```
Execute the script to run IntelliJ in headless mode

```console
./vintellij.sh ${project-path}
```

## Vintellij commands

- `VintellijGenDoc`: Generate kotlin doc at the current cursor. (Currently, only support Class and Function docs)

**Note:** Please move the cursor to the line of a class/function that you want to generate doc before running a command.
If your class/method contains annotations move the cursor to the top annotation instead.

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

## Neovim LSP setup

```lua
local status_ok, vintellij = pcall(require, "vintellij")
if not status_ok then
  return
end

local lsp = require "lsp"

local lib_dirs = {
  "/Users/dinhhuy258/.gradle/",
  "/Library/Java/JavaVirtualMachines",
}

vintellij.setup {
  debug = false,
  common_on_attach = lsp.common_on_attach,
  common_capabilities = lsp.common_capabilities(),
  common_on_init = lsp.common_on_init,
  lib_dirs = lib_dirs,
}
```

Where `lib_dirs` is the list of external library sources that uses by Intellij.

## Coc setup

Not supported at the moment please use branch `lsp_backup` instead.

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
  - Open IntelliJ in headless mode
