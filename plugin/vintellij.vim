command! VintellijOpenFile call vintellij#OpenFile()
command! VintellijSuggestImports call vintellij#SuggestImports()
command! -bang VintellijSyncBufferToggle call vintellij#SyncBufferToggle(<bang>0)

let s:path = fnamemodify(resolve(expand('<sfile>:p')), ':h')
let s:init_path = s:path . '/vintellij_init.py'

function! s:VintellijToggle(bang) abort
  if a:bang
    let l:vintellij_enabled = v:false
    call vintellij#buffer#UnregisterCurrent()
  else
    let l:vintellij_enabled = v:true
  endif

  exe 'py3file' s:init_path

  call vintellij#events#Init()

  call coc#config('languageserver.vintellij.enable', l:vintellij_enabled)
  execute 'silent! edit'

  echom "Vintellij enable is " . l:vintellij_enabled
endfunction

command! -bang VintellijToggle call <SID>VintellijToggle(<bang>0)

if get(g:, 'vintellij_use_default_keymap', 1) == 1
  nnoremap <Leader>co :VintellijOpenFile<CR>
  nnoremap <Leader>ci :VintellijSuggestImports<CR>
endif

