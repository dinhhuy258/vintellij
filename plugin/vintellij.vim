command! VintellijOpenFile call vintellij#OpenFile()
command! VintellijSuggestImports call vintellij#SuggestImports()
command! -bang VintellijSyncBufferToggle call vintellij#SyncBufferToggle(<bang>0)

let s:path = fnamemodify(resolve(expand('<sfile>:p')), ':h')
let s:init_path = s:path . '/vintellij_init.py'

function! s:VintellijEnable() abort
  if exists('s:vintellij_enabled')
    echomsg "Vintellij was already enabled"
    return
  endif

  let s:vintellij_enabled = v:true
  exe 'py3file' s:init_path

  call vintellij#events#Init()

  call coc#config('languageserver.vintellij.enable', v:true)
  execute 'silent! edit'

  echomsg "Vintellij is enabled"
endfunction

command! VintellijEnable call <SID>VintellijEnable()

if get(g:, 'vintellij_use_default_keymap', 1) == 1
  nnoremap <Leader>co :VintellijOpenFile<CR>
  nnoremap <Leader>ci :VintellijSuggestImports<CR>
endif

