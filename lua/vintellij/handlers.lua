local utils = require("vintellij.utils")
local cfg = require("vintellij.config").config

local M = {}

local function notification_handler()
	vim.lsp.handlers["vintellij/notification"] = function(_, _, params, client_id, _)
		if params["eventType"] == 1 then
			-- Close connection
			vim.lsp.stop_client(client_id, true)
		elseif params["eventType"] == 2 then
			-- Request completion
			if vim.fn.mode() ~= "i" then
				return
			end

			local status_ok, cmp = pcall(require, "cmp")
			if not status_ok then
				return
			end

			cmp.complete()
		end
	end
end

local function sync_buffer_handler()
	vim.lsp.handlers["vintellij/syncBuffer"] = function(_, _, params, _, _)
		local buffers = vim.api.nvim_list_bufs()

		for _, buffer in ipairs(buffers) do
			local fullname = vim.api.nvim_buf_get_name(buffer)
			if fullname == params["path"] then
				local ft = vim.api.nvim_buf_get_option(buffer, "filetype")
				if ft == "kotlin" or ft == "java" then
					if params["replaceText"] then
						if cfg.debug then
							utils.info("Syncing buffer...")
						end

						vim.api.nvim_buf_set_lines(
							buffer,
							params["startLine"],
							params["endLine"],
							false,
							params["lines"]
						)
					else
						local indent = utils.repeats(" ", vim.fn.indent(params["startLine"] + 1))
						local lines = {}
						for _, line in ipairs(params["lines"]) do
							table.insert(lines, indent .. line)
						end
						vim.fn.appendbufline(buffer, params["startLine"], lines)
					end
				end
				break
			end
		end
	end
end

function M.setup()
	notification_handler()
	sync_buffer_handler()
end

return M
