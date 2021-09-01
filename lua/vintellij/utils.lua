local M = {}

local function echo_multiline(msg)
	for _, s in ipairs(vim.fn.split(msg, "\n")) do
		vim.cmd("echom '" .. s:gsub("'", "''") .. "'")
	end
end

function M.repeats(s, n)
	return n > 0 and s .. M.repeats(s, n - 1) or ""
end

function M.resolve_bufnr(bufnr)
	vim.validate({ bufnr = { bufnr, "n", true } })
	if bufnr == nil or bufnr == 0 then
		return vim.api.nvim_get_current_buf()
	end

	return bufnr
end

function M.info(msg)
	vim.cmd("echohl Directory")
	echo_multiline("[vintellij] " .. msg)
	vim.cmd("echohl None")
end

function M.warn(msg)
	vim.cmd("echohl WarningMsg")
	echo_multiline("[vintellij]" .. msg)
	vim.cmd("echohl None")
end

function M.err(msg)
	vim.cmd("echohl ErrorMsg")
	echo_multiline("[vintellij]" .. msg)
	vim.cmd("echohl None")
end

return M
