local M = {}

function M.repeats(s, n)
	return n > 0 and s .. M.repeats(s, n - 1) or ""
end

return M
