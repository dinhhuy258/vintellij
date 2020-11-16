let s:cpo_save = &cpo
set cpo&vim

function! s:AddImport(import)
  let l:lineNumber = 1
  let l:maxLine = line('$')
  while l:lineNumber <= l:maxLine
    let l:line = getline(l:lineNumber)
    if l:line =~# '^import '
      call append(l:lineNumber - 1,  a:import)
      return
    endif
    let l:lineNumber += 1
  endwhile

  call append(1, @a)
  call append(2, a:import)
endfunction

function! s:HandleImportEvent(data) abort
  let l:imports = a:data.imports
  if empty(l:imports)
    echo '[vintellij] No import candidates found'
    return
  endif
  call fzf#run(fzf#wrap({
        \ 'source': l:imports,
        \ 'sink': function('s:AddImport')
        \ }))
endfunction

function! s:HandleOpenEvent(data) abort
  echo '[vintellij] File successfully opened: ' . a:data.file
endfunction

function! s:GetCurrentOffset()
  return line2byte(line('.')) + col('.') - 1
endfunction

function! s:SendRequest(handler, data, ignore_message) abort
  let l:buf = bufnr('')
  if vintellij#bvar#has(l:buf, 'channel')
    try
      let result = call('rpcrequest', [vintellij#bvar#get(l:buf, 'channel'), 'vintellij_handler',
                        \ {'handler' : a:handler, 'data' : a:data}])
    catch /./ " The channel has been probably closed
      call vintellij#util#TruncatedEcho('Failed to send request to JetBrains instance. \n' . v:exception)
    endtry
  endif
endfunction

function! vintellij#VintellijResponseCallback(data) abort
  try
    let l:json_data = json_decode(a:data)
    if !l:json_data.success
      echo '[vintellij] ' . l:json_data.message
      return
    endif

    let l:handler = l:json_data.handler
    if l:handler ==# 'import'
      call s:HandleImportEvent(l:json_data.data)
    elseif l:handler ==# 'open'
      call s:HandleOpenEvent(l:json_data.data)
    elseif l:handler ==# 'syncBufferToggle'
      if l:json_data.data.enable
        call vintellij#buffer#enableSyncBufWriteCmd(bufnr('%'))
      else
        call vintellij#buffer#disableSyncBufWriteCmd(bufnr('%'))
      endif
      echo '[vintellij] Toggle sync buffer: ' . l:json_data.data.enable
    else
      throw '[vintellij] Invalid handler: ' . l:handler
    endif
  catch /.*/
    echo "Error during vintellij handling the response callback: " . v:exception
  endtry
endfunction

function! vintellij#OpenFile() abort
  call s:SendRequest('open', {
        \ 'file': expand('%:p'),
        \ 'offset': s:GetCurrentOffset(),
        \ }, v:false)
endfunction

function! vintellij#SuggestImports() abort
  call s:SendRequest('import', {
        \ 'file': expand('%:p'),
        \ 'offset': s:GetCurrentOffset(),
        \ }, v:false)
endfunction

fu! vintellij#SyncBufferToggle(bang) abort
  if a:bang
    let l:enable = v:false
  else
    let l:enable = v:true
  endif

  call s:SendRequest('syncBufferToggle', {
        \ 'enable': l:enable,
        \ }, v:false)
endfu

function! vintellij#AddImport(import)
  let l:lineNumber = 1
  let l:maxLine = line('$')
  while l:lineNumber <= l:maxLine
    let l:line = getline(l:lineNumber)
    if l:line =~# '^import '
      call append(l:lineNumber - 1,  a:import)
      return
    endif
    let l:lineNumber += 1
  endwhile

  call append(1, @a)
  call append(2, a:import)
endfunction

let &cpo = s:cpo_save
unlet s:cpo_save

