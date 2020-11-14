command! VintellijOpenFile call vintellij#OpenFile()
command! VintellijSuggestImports call vintellij#SuggestImports()
command! -bang VintellijSyncBufferToggle call vintellij#SyncBufferToggle(<bang>0)

let s:path = fnamemodify(resolve(expand('<sfile>:p')), ':h')
let s:init_path = s:path . '/vintellij_init.py'

function! s:VintellijToggle(bang) abort
  exe 'py3file' s:init_path

  call vintellij#events#Init()

  if a:bang
    call chanclose(vintellij#bvar#get(bufnr('%'), 'channel'))
    call vintellij#buffer#UnregisterCurrent()
    call coc#config('languageserver.vintellij.enable', v:false)
    call coc#rpc#notify('toggleService', ['languageserver.vintellij'])
  else
    call coc#config('languageserver.vintellij.enable', v:true)
    call coc#rpc#notify('toggleService', ['languageserver.vintellij'])
  endif

  execute 'silent! edit'
endfunction

command! -bang VintellijToggle call <SID>VintellijToggle(<bang>0)

if get(g:, 'vintellij_use_default_keymap', 1) == 1
  nnoremap <Leader>co :VintellijOpenFile<CR>
  nnoremap <Leader>ci :VintellijSuggestImports<CR>
endif

if get(g:, 'enable_buffer_sync_by_default', 1) == 1
  let g:enable_buffer_sync_by_default = 1
endif
