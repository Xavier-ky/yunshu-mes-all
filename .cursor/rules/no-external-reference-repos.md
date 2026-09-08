# 工作区边界

- **Never** create, download, clone, or unzip external MES reference codebases inside this workspace.
- All UI and API work must use existing code under `frontend/web-admin/src/yunshu-ui/` and backend `compat` packages only.
- If any external reference folder appears at project root, **delete it immediately** — do not read from it or add it to the project.
- Do not add symlinks or copy trees from outside the repo.
