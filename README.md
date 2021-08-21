# Vintellij
Make IntelliJ as a server language protocol.

## Vintellij versions

- `master`: This is an original version of vintellij
- `comrade`: Same as `master` branch that including comrade plugin
- `lsp`: This is currently the main branch of vintellij which supports Language Server Protocol

## Installation

### Vim

#### Dependencies

- [fzf](https://github.com/junegunn/fzf.vim)

#### Install

```
Plug 'dinhhuy258/vintellij', {'branch': 'lsp'}
```

The default mapping is
- <Leader>co to open the current file from vim by intelliJ
- <Leader>ci to suggest possible imports by the symbol under cursor

if you want to make your own custom keymap, then put the following in your `.vimrc`

```
let g:vintellij_use_default_keymap = 0
nnoremap <Leader>co :VintellijOpenFile<CR>
nnoremap <Leader>ci :VintellijSuggestImports<CR>
...
```

For better syntax support

```
Plug 'udalov/kotlin-vim'
```
### Intellij plugin

1. Import the project into Intellij
2. Modify intellij plugin version and intellij kotlin version in `build.gradle` file based on your version of Intellij
3. Run `gradle buildPlugin`
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

- `VintellijToggle`: Enable vintellij LSP, the LSP is not enabled by default

- `VintellijOpenFile`: open the current file in Intellij

- `VintellijSuggestImports`: suggest a list of importable classes for an element at the current cursor

## Coc setup

```
...
  "languageserver": {
    "vintellij":{
        "enable": false,
        "host": "127.0.0.1",
        "port": 6969,
        "rootPatterns": [
          ".vim/",
          ".git/",
          ".hg/"
        ],
        "filetypes": [
            "java",
            "kotlin"
        ],
        "additionalSchemes": ["jar", "zipfile"]
    }
  }
```

Set value `g:vintellij_nvim_lsp` to `0` (default `1`)

## Neovim LSP setup

```lua
{
  provider = "kotlin_language_server",
  setup = {
    cmd = { "nc", "localhost", "6969" },
    root_dir = function(fname)
      local util = require "lspconfig/util"

      local root_files = {
        "settings.gradle", -- Gradle (multi-project)
        "settings.gradle.kts", -- Gradle (multi-project)
        "build.xml", -- Ant
        "pom.xml", -- Maven
      }

      local fallback_root_files = {
        "build.gradle", -- Gradle
        "build.gradle.kts", -- Gradle
      }
      return util.root_pattern(unpack(root_files))(fname) or util.root_pattern(unpack(fallback_root_files))(fname)
    end,
    filetypes = { "kotlin" },
    autostart = false, -- We need to disable auto start
    on_attach = common_on_attach,
    on_init = common_on_init,
    capabilities = common_capabilities,
  },
}
```

## Features

| Name | Kotlin | Java |
| ---- | ------ | ---- |
| Go to definition | :white_check_mark: | :white_check_mark: |
| Suggest imports | :white_check_mark: | :white_check_mark: |
| Find hierarchies | :white_check_mark: | :white_check_mark: |
| Find usages | :white_check_mark: | :white_check_mark: |
| Auto complete | :white_check_mark: | :white_check_mark: |

## Credits

- [Comrade](https://github.com/beeender/Comrade)
- [ComradeNeovim](https://github.com/beeender/ComradeNeovim)

Recently I just integrated the Comrade IntelliJ plugin source code into my plugin. It makes the vintelliJ plugin stronger and smarter. I would like to say thank to the author of the Comrade plugin for such a wonderful plugin that he made.

## Issue

- To make it work, the Intellij must open the same project as Vim.
- Always open Intellij otherwise everything will be slow - the workarround maybe:
  - Get IntelliJ focused by having it in your secondary screen
  - Get vim transparent and putting IntelliJ behind
  - Open IntelliJ in headless mode
  
