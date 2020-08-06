let s:cpo_save = &cpo
set cpo&vim

let s:channel_id = 0
let s:map = {}

function! s:SaveCurrentBuffer() abort
  unlet! b:vintellij_refresh_done
  silent! w
  call vintellij#RefreshFile()
endfunction

function! s:IsRefreshDone() abort
  return exists('b:vintellij_refresh_done')
endfunction

function! s:SaveBufferAndWaitForRefresh()
  call s:SaveCurrentBuffer()

  let l:num_tries = 0
  echo "[vintellij] Waiting to refresh file in intellij..."
  " Waiting for refreshing the file
  while !s:IsRefreshDone() && l:num_tries < 100 " Only wait for 100 * 2 = 200ms
    sleep 2ms
    let l:num_tries += 1
  endwhile
endfunction

function! s:GetCompleteResult() abort
  if exists('b:vintellij_completion_result')
    return b:vintellij_completion_result
  endif

  return v:null
endfunction

function! s:DetectKotlinFile(file) abort
  if a:file =~ 'kotlin-sdtlib' || a:file =~ '::kotlin'
    setfiletype kotlin
  endif
endfunction

function! s:AddImport(import)
  let l:lineNumber = 1
  let l:maxLine = line('$')
  while l:lineNumber <= l:maxLine
    let l:line = getline(l:lineNumber)
    if l:line =~# '^import '
      call append(l:lineNumber - 1, 'import ' . a:import)
      return
    endif
    let l:lineNumber += 1
  endwhile
  call append(1, 'import ' . a:import)
endfunction

function! s:GoToFile(file, offset)
  execute 'edit ' . a:file
  execute 'goto ' . a:offset
  call s:DetectKotlinFile(a:file)
endfunction

function! s:HandleGoToFile(preview)
  call s:GoToFile(s:map[a:preview].file, s:map[a:preview].offset + 1)
endfunction

function! s:HandleGoToEvent(data) abort
  if has_key(a:data, 'file')
    call s:GoToFile(a:data.file, a:data.offset + 1)
  else
    echo '[vintellij] Definition not found'
  endif
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

function! s:HandleFindHierarchyEvent(data) abort
  let l:hierarchies = a:data.hierarchies
  if empty(l:hierarchies)
    echo '[vintellij] No hierarchy found'
    return
  elseif len(l:hierarchies) == 1
    call s:GoToFile(l:hierarchies[0].file, l:hierarchies[0].offset + 1)
    return
  endif

  let l:hierarchyPreviews = []
  let s:map = {}
  for hierarchy in l:hierarchies
    let s:map = extend(s:map, { hierarchy.preview: { 'file': hierarchy.file, 'offset': hierarchy.offset } })
    let l:hierarchyPreviews = add(l:hierarchyPreviews, hierarchy.preview)
  endfor
  call fzf#run(fzf#wrap({
        \ 'source': l:hierarchyPreviews,
        \ 'sink': function('s:HandleGoToFile')
        \ }))
endfunction

function! s:HandleFindUsageEvent(data) abort
  let l:usages = a:data.usages
  if empty(l:usages)
    echo '[vintellij] No usage found'
    return
  elseif len(l:usages) == 1
    call s:GoToFile(l:usages[0].file, l:usages[0].offset + 1)
    return
  endif

  let l:usagePreviews = []
  let s:map = {}
  for usage in l:usages
    let s:map = extend(s:map, { usage.preview: { 'file': usage.file, 'offset': usage.offset } })
    let l:usagePreviews = add(l:usagePreviews, usage.preview)
  endfor
  call fzf#run(fzf#wrap({
        \ 'source': l:usagePreviews,
        \ 'sink': function('s:HandleGoToFile')
        \ }))
endfunction

function! s:HandleAutocompleteEvent(data) abort
  let b:vintellij_completion_result = a:data.completions
endfunction

function! s:HandleOpenEvent(data) abort
  echo '[vintellij] File successfully opened: ' . a:data.file
endfunction

function! s:HandleRefreshEvent(data) abort
  let b:vintellij_refresh_done = 1
  echo '[vintellij] File successfully refreshed: ' . a:data.file
endfunction

function! s:HandleHealthCheckEvent() abort
  echo '[vintellij] Connect to plugin server successful'
endfunction

function! s:GetCurrentOffset()
  return line2byte(line('.')) + col('.') - 1
endfunction

function! s:OnReceiveData(channel_id, data, event) abort
  try
    let l:json_data = json_decode(a:data)
  catch /.*/
    call s:CloseConnection()
    echo '[vintellij] The plugin server has been disconnected'
    return
  endtry
  if !l:json_data.success
    throw '[vintellij] ' . l:json_data.message
  endif

  let l:handler = l:json_data.handler
  if l:handler ==# 'goto'
    call s:HandleGoToEvent(l:json_data.data)
  elseif l:handler ==# 'import'
    call s:HandleImportEvent(l:json_data.data)
  elseif l:handler ==# 'find-hierarchy'
    call s:HandleFindHierarchyEvent(l:json_data.data)
  elseif l:handler ==# 'find-usage'
    call s:HandleFindUsageEvent(l:json_data.data)
  elseif l:handler ==# 'autocomplete'
    call s:HandleAutocompleteEvent(l:json_data.data)
  elseif l:handler ==# 'open'
    call s:HandleOpenEvent(l:json_data.data)
  elseif l:handler ==# 'refresh'
    call s:HandleRefreshEvent(l:json_data.data)
  elseif l:handler ==# 'health-check'
    call s:HandleHealthCheckEvent()
  else
    throw '[vintellij] Invalid handler: ' . l:handler
  endif
endfunction

function! s:IsConnected() abort
  return index(map(nvim_list_chans(), 'v:val.id'), s:channel_id) >= 0
endfunction

function! s:CloseConnection() abort
  call chanclose(s:channel_id)
  let s:channel_id = 0
endfunction

function! s:OpenConnection() abort
  try
    let s:channel_id = sockconnect('tcp', 'localhost:6969', {
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
      throw '[vintellij] Can not connect to the plugin server. Please open intelliJ which is installed vintellij plugin'
    endif
  endif
  call chansend(s:channel_id, json_encode({
        \ 'handler': a:handler,
        \ 'data': a:data
        \ }))
endfunction

function! vintellij#GoToDefinition() abort
  call s:SaveBufferAndWaitForRefresh()

  call s:SendRequest('goto', {
        \ 'file': expand('%:p'),
        \ 'offset': s:GetCurrentOffset(),
        \ })
endfunction

function! vintellij#OpenFile() abort
  call s:SaveBufferAndWaitForRefresh()

  call s:SendRequest('open', {
        \ 'file': expand('%:p'),
        \ 'offset': s:GetCurrentOffset(),
        \ })
endfunction

function! vintellij#SuggestImports() abort
  call s:SaveBufferAndWaitForRefresh()

  call s:SendRequest('import', {
        \ 'file': expand('%:p'),
        \ 'offset': s:GetCurrentOffset(),
        \ })
endfunction

function! vintellij#FindHierarchy() abort
  call s:SaveBufferAndWaitForRefresh()

  call s:SendRequest('find-hierarchy', {
        \ 'file': expand('%:p'),
        \ 'offset': s:GetCurrentOffset(),
        \ })
endfunction

function! vintellij#FindUsage() abort
  call s:SaveBufferAndWaitForRefresh()

  call s:SendRequest('find-usage', {
        \ 'file': expand('%:p'),
        \ 'offset': s:GetCurrentOffset(),
        \ })
endfunction

function! vintellij#RefreshFile() abort
  call s:SendRequest('refresh', {
        \ 'file': expand('%:p'),
        \ })
endfunction

function! vintellij#HealthCheck() abort
  call s:SendRequest('health-check', {})
endfunction

function! vintellij#Autocomplete(findstart, base) abort
  if a:findstart
    " Saving the buffer before doing completion
    call s:SaveCurrentBuffer()

    " The function is called to find the start of the text to be completed
    let l:start = col('.') - 1
    let l:line = getline('.')
    while l:start > 0 && l:line[l:start - 1] =~# '\a'
      let l:start -= 1
    endwhile

    return l:start
  endif

  echo "[vintellij] Waiting to refresh file in intellij..."
  " Waiting for refreshing the file
  while !s:IsRefreshDone() && !complete_check()
    sleep 2ms
  endwhile

  " The function is called to actually find the matches
  echo '[vintellij] Getting completions...'
  unlet! b:vintellij_completion_result
  call s:SendRequest('autocomplete', {
        \ 'file': expand('%:p'),
        \ 'offset': s:GetCurrentOffset() - 1,
        \ 'base': a:base,
        \ })

  let l:result = s:GetCompleteResult()
  while l:result is v:null && !complete_check()
    sleep 2ms
    let l:result = s:GetCompleteResult()
  endwhile

  let l:completions = l:result isnot v:null ? l:result : []
  echo '[vintellij] Found ' . len(l:completions) . ' completion(s)'
  return l:completions
endfunction

function! vintellij#EnableAutoRefreshFile(isDisable)
  augroup vintellij_on_kt_java_file_save
    autocmd!
    if !a:isDisable
      autocmd BufWritePost,FileReadPost *.kt,*.java call vintellij#RefreshFile()
    endif
  augroup END
endfunction

function! vintellij#EnableHealthCheckOnLoad(isDisable)
  augroup vintellij_on_kt_java_file_load
    autocmd!
    if !a:isDisable
      autocmd BufReadPost,FileReadPost *.kt,*.java call vintellij#HealthCheck()
    endif
  augroup END
endfunction

let &cpo = s:cpo_save
unlet s:cpo_save

