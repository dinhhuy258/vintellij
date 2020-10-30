let s:cpo_save = &cpo
set cpo&vim

let s:channel_id = 0

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
      call append(l:lineNumber - 1,  a:import)
      return
    endif
    let l:lineNumber += 1
  endwhile

  call append(1, @a)
  call append(2, a:import)
endfunction

function! s:GoToFile(file, offset)
  silent! normal! m'
  if expand("%:p") != a:file
    execute 'silent! edit ' . a:file
  endif

  execute 'keepjumps goto ' . a:offset
  call s:DetectKotlinFile(a:file)
endfunction

function! s:HandleGoToFile(preview)
  call s:GoToFile(b:map[a:preview].file, b:map[a:preview].offset + 1)
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
  let b:map = {}
  for hierarchy in l:hierarchies
    let b:map = extend(b:map, { hierarchy.preview: { 'file': hierarchy.file, 'offset': hierarchy.offset } })
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
  let b:map = {}
  for usage in l:usages
    let b:map = extend(b:map, { usage.preview: { 'file': usage.file, 'offset': usage.offset } })
    let l:usagePreviews = add(l:usagePreviews, usage.preview)
  endfor
  call fzf#run(fzf#wrap({
        \ 'source': l:usagePreviews,
        \ 'sink': function('s:HandleGoToFile')
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
    if l:handler ==# 'goto'
      call s:HandleGoToEvent(l:json_data.data)
    elseif l:handler ==# 'import'
      call s:HandleImportEvent(l:json_data.data)
    elseif l:handler ==# 'find-hierarchy'
      call s:HandleFindHierarchyEvent(l:json_data.data)
    elseif l:handler ==# 'find-usage'
      call s:HandleFindUsageEvent(l:json_data.data)
    elseif l:handler ==# 'open'
      call s:HandleOpenEvent(l:json_data.data)
    else
      throw '[vintellij] Invalid handler: ' . l:handler
    endif
  catch /.*/
    echo "Error during vintellij handling the response callback: " . v:exception
  endtry
endfunction

function! vintellij#GoToDefinition() abort
  call s:SendRequest('goto', {
        \ 'file': expand('%:p'),
        \ 'offset': s:GetCurrentOffset(),
        \ }, v:false)
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

function! vintellij#FindHierarchy() abort
  call s:SendRequest('find-hierarchy', {
        \ 'file': expand('%:p'),
        \ 'offset': s:GetCurrentOffset(),
        \ }, v:false)
endfunction

function! vintellij#FindUsage() abort
  call s:SendRequest('find-usage', {
        \ 'file': expand('%:p'),
        \ 'offset': s:GetCurrentOffset(),
        \ }, v:false)
endfunction

let &cpo = s:cpo_save
unlet s:cpo_save
