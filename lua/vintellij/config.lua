local M = {}

M.config = {}

local default_cfg = {
	debug = false,
	keymaps = {
		gen_doc = "<Leader>cd",
	},
	common_on_attach = function() end,
	common_capabilities = {},
	common_on_init = function() end,
	lib_dirs = {},
}

function M.setup(cfg)
	if cfg == nil then
		cfg = {}
	end

	for k, v in pairs(default_cfg) do
		if cfg[k] ~= nil then
			if type(v) == "table" then
				M.config[k] = vim.tbl_extend("force", v, cfg[k])
			else
				M.config[k] = cfg[k]
			end
		else
			M.config[k] = default_cfg[k]
		end
	end
end

return M
