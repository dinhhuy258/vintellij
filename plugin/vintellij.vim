command! VintellijGoToDefinition call vintellij#GoToDefinition()
command! VintellijOpenFile call vintellij#OpenFile()
command! VintellijSuggestImports call vintellij#SuggestImports()
command! VintellijFindHierarchy call vintellij#FindHierarchy()
command! VintellijFindUsage call vintellij#FindUsage()

"=============================================================================
" AUTHOR:  beeender <chenmulong at gmail.com>
" License: GPLv3
"=============================================================================

let s:path = fnamemodify(resolve(expand('<sfile>:p')), ':h')
let s:init_path = s:path . '/vintellij_init.py'

function! s:VintellijComradeEnable() abort
  if exists('s:vintellij_loaded')
    echomsg "Vintellij was already loaded"
    return
  endif

  let s:vintellij_loaded = 1

  exe 'py3file' s:init_path
  call vintellij#events#Init()
  call vintellij#events#RegisterAutoImportOnCompletionDone()

  let s:vintellij_loaded = v:true
endfunction

call <SID>VintellijComradeEnable()

if get(g:, 'vintellij_use_default_keymap', 1) == 1
  nnoremap <Leader>cgd :VintellijGoToDefinition<CR>
  nnoremap <Leader>cgh :VintellijFindHierarchy<CR>
  nnoremap <Leader>cgu :VintellijFindUsage<CR>
  nnoremap <Leader>co :VintellijOpenFile<CR>
  nnoremap <Leader>ci :VintellijSuggestImports<CR>
endif

