local M = {}

local function setup_events()
	vim.api.nvim_command("autocmd BufEnter * lua require('vintellij').on_buf_enter()")
end

local function setup_handlers()
	vim.lsp.handlers["vintellij/eventNotification"] = function(_, _, params, client_id, _)
		if params["eventType"] == 1 then
			-- Close connection
			vim.lsp.stop_client(client_id, true)
		end
	end

	vim.lsp.handlers["vintellij/syncBuffer"] = function(_, _, params, _, _)
		local buffers = vim.api.nvim_list_bufs()

		for _, buffer in ipairs(buffers) do
			local fullname = vim.api.nvim_buf_get_name(buffer)
			if fullname == params["path"] then
				local ft = vim.api.nvim_buf_get_option(buffer, "filetype")
				if ft == "kotlin" or ft == "java" then
					vim.api.nvim_buf_set_lines(buffer, params["startLine"], params["endLine"], true, params["lines"])
				end
				break
			end
		end
	end
end

local function resolve_bufnr(bufnr)
	vim.validate({ bufnr = { bufnr, "n", true } })
	if bufnr == nil or bufnr == 0 then
		return vim.api.nvim_get_current_buf()
	end

	return bufnr
end

local function on_attach(bufnr)
	-- Currently, nvim lsp does not support `will save` that why we need to add the autocmd here
	-- TODO: Remove this command when nvim support `will save`
	vim.api.nvim_command(
		string.format(
			"autocmd BufWriteCmd <buffer=%d> lua require('vintellij').text_document_will_save_handler(0)",
			bufnr
		)
	)
end

function M.on_buf_enter()
	local bufnr = resolve_bufnr(0)
	local uri = vim.uri_from_bufnr(bufnr)

	vim.lsp.for_each_buffer_client(bufnr, function(client, _)
		if client.name == "vintellij" then
			client.notify("textDocument/didOpen", {
				textDocument = {
					uri = uri,
				},
			})
		end
	end)
end

function M.text_document_will_save_handler(bufnr)
	bufnr = resolve_bufnr(bufnr)
	local uri = vim.uri_from_bufnr(bufnr)

	vim.lsp.for_each_buffer_client(bufnr, function(client, _)
		if client.name == "vintellij" then
			client.notify("textDocument/willSave", {
				textDocument = {
					uri = uri,
				},
				reason = 1,
			})

			vim.api.nvim_buf_set_option(bufnr, "modified", false)
		end
	end)
end

function M.setup(common_on_attach, common_capabilities, common_on_init)
	local lspconfig_ok, lspconfig = pcall(require, "lspconfig")
	local configs_ok, configs = pcall(require, "lspconfig/configs")

	if not lspconfig_ok or not configs_ok then
		-- TODO: Add error message here
		return
	end

	common_capabilities.textDocument.synchronization.willSave = true

	if not lspconfig.vintellij then
		configs.vintellij = {
			default_config = {
				cmd = { "nc", "localhost", "6969" },
				root_dir = function(fname)
					local util = require("lspconfig/util")

					local root_files = {
						"settings.gradle", -- Gradle (multi-project)
						"settings.gradle.kts", -- Gradle (multi-project)
						"build.xml", -- Ant
						"pom.xml", -- Maven
					}

					local fallback_root_files = {
						"build.gradle", -- Gradle
						"build.gradle.kts", -- Gradle
					}
					return util.root_pattern(unpack(root_files))(fname)
						or util.root_pattern(unpack(fallback_root_files))(fname)
				end,
				filetypes = { "kotlin" },
				autostart = false,
				on_attach = function(client_id, bufnr)
					common_on_attach(client_id, bufnr)
					on_attach(bufnr)
				end,
				on_init = common_on_init,
				capabilities = common_capabilities,
			},
		}
	end

	lspconfig.vintellij.setup({})
	setup_events()
	setup_handlers()
end

return M
