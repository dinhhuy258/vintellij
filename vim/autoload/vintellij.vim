let s:cpo_save = &cpo
set cpo&vim

let s:channel_id = 0

function! s:HandleGoToEvent(data) abort
  if has_key(a:data, 'file')
    execute 'edit ' . a:data.file
    execute 'goto ' . (a:data.offset + 1)
  else
    echo '[Vintellij] Definition not found.'
  endif
endfunction

function! s:HandleImportEvent(data) abort
  echo 'HandleImportEvent'
endfunction

function! s:HandleOpenEvent(data) abort
  echo '[Vintellij] File successfully opened: ' . a:data.file
endfunction

function! s:GetCurrentOffset()
  return line2byte(line('.')) + col('.') - 1
endfunction

function! s:OnReceiveData(channel_id, data, event) abort
  let l:json_data = json_decode(a:data)
  if !l:json_data.success
    throw '[Vintellij] ' . l:json_data.message
  endif

  let l:handler = l:json_data.handler
  if l:handler ==# 'goto'
    call s:HandleGoToEvent(l:json_data.data)
  elseif l:handler ==# 'import'
    call s:HandleImportEvent(l:json_data.data)
  elseif l:handler ==# 'open'
    call s:HandleOpenEvent(l:json_data.data)
  else
    throw '[Vintellij] Invalid handler: ' . l:handler
  endif
endfunction

function! s:IsConnected() abort
  return index(map(nvim_list_chans(), 'v:val.id'), s:channel_id) >= 0
endfunction

function! s:OpenConnection() abort
  try
    if exists('g:vintellij_port')
      let l:port = g:vintellij_port
    else
      let l:port = 6969
    endif
    let s:channel_id = sockconnect('tcp', 'localhost:' . l:port, {
          \                        'on_data': function('s:OnReceiveData'),
          \ })
  catch /connection failed/
    let s:channel_id = 0
  endtry

  return s:channel_id
endfunction

function! s:SendRequest(handler, data) abort
  if !s:IsConnected()
    if s:OpenConnection() == 0
      throw '[Vintellij] Can not connect to the plugin server. Please open intelliJ which is installed vintellij plugin.'
    endif
  endif
  call chansend(s:channel_id, json_encode({
        \ 'handler': a:handler,
        \ 'data': a:data
        \ }))
endfunction

function! vintellij#GoToDefinition() abort
  call s:SendRequest('goto', {
        \ 'file': expand('%:p'),
        \ 'offset': s:GetCurrentOffset(),
        \ })
endfunction

function! vintellij#OpenFile() abort
  call s:SendRequest('open', {
        \ 'file': expand('%:p'),
        \ 'offset': s:GetCurrentOffset(),
        \ })
endfunction

let &cpo = s:cpo_save
unlet s:cpo_save

