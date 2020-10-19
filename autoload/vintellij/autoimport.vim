function! vintellij#autoimport#OnCompletionDone() abort
  " assume the completed item has 'abbr' = 'Repository (org.springframework.stereotype)', and 'word' = 'Repository'
  let importInfo = get(v:completed_item, 'abbr', '')
  let candidate = get(v:completed_item, 'word', '')

  let package = s:ExtractPackage(l:importInfo)
  if empty(l:package)
    return
  endif

  let importLine = printf('import %s.%s', l:package, l:candidate)
  if !search(l:importLine, 'np')
    call appendbufline(bufnr(), 2, l:importLine)
  endif
endfunction

" To get 'org.springframework.stereotype' from 'Repository (org.springframework.stereotype)'
function! s:ExtractPackage(importInfo) abort
  let indexOfStartParentheses = stridx(a:importInfo, '(')
  if indexOfStartParentheses == -1
    return ''
  endif
  let indexOfEndParentheses = stridx(a:importInfo, ')')
  if indexOfEndParentheses == -1
    return ''
  endif

  " Get index of 'o' after '(' in 'Repository (org.springframework.stereotype)'
  let indexOfPackageBegin = l:indexOfStartParentheses + 1

  " Get the length of 'org.springframework.stereotype'
  let lengthOfPackageString = l:indexOfEndParentheses - l:indexOfStartParentheses - 1

  let package = strpart(a:importInfo, l:indexOfPackageBegin, l:lengthOfPackageString)

  " match either 'org.springframework.stereotype' or 'org', check http://vimregex.com/
  if l:package =~  '^\(\w\+\.\)\+\w*\(\.\w\+\|\w\+\)$'|| l:package =~ '^\w\+$'
    return l:package
  endif

  return ''
endfunction

