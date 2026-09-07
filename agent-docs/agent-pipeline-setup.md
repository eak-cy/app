# Agent pipeline setup

Both hosts use the [shared workflow](../.agents/commands/feature.md). The main conversation is EM: clarify requirements (PO only when needed), agree a brief plan with the user, send one Lead to implement code/tests/docs, then review the diff. No separate orchestrator or routine PO documentation approval round.

## Shared sources and hosts

`AGENTS.md` is shared through `CLAUDE.md`'s relative symlink. `.agents/` owns roles, contracts, commands, and skills; matching `.claude/` files are symlinks. `.codex/agents/*.toml` holds native metadata/model settings and references the same role Markdown, excluding Claude YAML. Both hosts read the same product and engineering docs. Keep credentials and local settings gitignored.

Codex uses the [native TOML format](https://learn.chatgpt.com/docs/agent-configuration/subagents); Claude uses [Markdown subagents](https://code.claude.com/docs/en/sub-agents). Open a fresh session after profile changes. Claude: `/feature "<description>"`. Codex: `Use the feature workflow for <description>`; AGENTS.md routes the request without requiring a native slash-menu entry.

EM speaks directly to the user and handles agent dispatch. If a subagent cannot ask the user directly, it returns `USER_QUESTION` to EM. Use the same workflow sequentially if agents are unavailable and disclose self-review. Ordinary ChatGPT without repository/tools access cannot automatically load this setup. Codex uses its configured provider; OmniRoute below is optional Claude infrastructure.

## Roles and effort

| Role | Claude profile | Codex profile | Use |
|---|---|---|---|
| PO | `sonnet` | parent default | new/unclear product requirements; accountable for `pages/` |
| EM | main session; optional separate profile `sonnet` | main session; optional separate profile inherits parent | technical direction, questions, final review |
| Lead LOW | `haiku` | `gpt-5.6-terra`/low | bounded implementation |
| Lead MEDIUM | `sonnet` | `gpt-5.6-sol`/medium | contained feature/integration work |
| Lead HIGH | `opus` | `gpt-5.6-sol`/xhigh | high-risk implementation |

The [complexity contract](../.agents/contracts/complexity.md) scales process depth as well as Lead selection. Model availability depends on the account/host. Main EM uses the user's selected session model; the optional EM profile does not change it.

Documentation ownership is accountability, not a write restriction. All roles may make verified factual edits within agreed scope; the Lead normally updates code and reference docs together. PO is consulted again for product ambiguity only. One writer per file; one EM review covers code and docs. No finished epic prerequisite; new feature docs still start in the first slice.

The [shared contract](../.agents/contracts/workflow.md) owns question, agreement, context, and messaging rules. AGENTS.md owns change-specific verification: docs/config-only edits use relevant structural checks, while application changes retain applicable tests and lint. Small features can combine dependency-ordered slices in one PR. No unrelated tests or repeated passing checks.

To evaluate changes over several comparable tasks, record available time-to-first-code-edit, total time, token usage, and material rework. Use host-reported metrics when available; do not add monitoring agents or invent missing counters. Compare similar risk levels before changing more settings.

## Optional Claude OmniRoute setup

Never commit OmniRoute's admin password, API key, or provider keys. The following records the tested local setup; verify provider IDs against your installation.

## Requirements

- nvm; Node 22 `>=22.22.2` (tested `22.23.1`); bundled npm 10.x.
- `omniroute` npm package (tested `3.8.48`; ~700 MB unpacked).
- Provider connections/API keys owned by the engineer.

Use nvm: system/Homebrew Node may be too old and global npm may require `sudo`.

## Install

```sh
brew install nvm
mkdir -p ~/.nvm
```

Add to `~/.zshrc`:

```sh
export NVM_DIR="$HOME/.nvm"
[ -s "/opt/homebrew/opt/nvm/nvm.sh" ] && \. "/opt/homebrew/opt/nvm/nvm.sh"
[ -s "/opt/homebrew/opt/nvm/etc/bash_completion.d/nvm" ] && \. "/opt/homebrew/opt/nvm/etc/bash_completion.d/nvm"
```

Then:

```sh
nvm install 22
nvm alias default 22
npm install -g omniroute
omniroute serve --daemon --no-open
curl http://localhost:20128/v1/models
```

Remove any old custom npm prefix/PATH. If needed: `nvm use --delete-prefix`.

Known OmniRoute 3.8.48 packaging bug: `omniroute config set claude` and `omniroute setup --list` fail on `@/shared`/`@/lib`. Do not use them; `serve`, `status`, `doctor`, `providers`, `chat`, `setup-claude`, and `launch` work. Configure manually below.

## Provider setup

At `http://localhost:20128`:

1. Set a local admin password.
2. Add Claude subscription and optional paid/free providers.
3. Create an OmniRoute API key.
4. Verify:

```sh
OMNIROUTE_API_KEY=<key> omniroute chat "reply with exactly: pong" --model auto
```

## Claude Code environment

Add to `~/.zshrc`:

```sh
export ANTHROPIC_BASE_URL="http://localhost:20128"
export ANTHROPIC_AUTH_TOKEN="<omniroute-api-key>"
export ANTHROPIC_API_KEY=""
export CLAUDE_CODE_ENABLE_GATEWAY_MODEL_DISCOVERY=1

export ANTHROPIC_DEFAULT_OPUS_MODEL="cc/claude-opus-4-8"
export ANTHROPIC_DEFAULT_SONNET_MODEL="cc/claude-sonnet-5"
export ANTHROPIC_DEFAULT_HAIKU_MODEL="cc/claude-haiku-4-5-20251001"
```

Explicit aliases prevent `400 Ambiguous model` when multiple providers expose the same bare name. IDs may drift; inspect `OMNIROUTE_API_KEY=<key> omniroute models`, update all three, then verify a prefixed model:

```sh
OMNIROUTE_API_KEY=<key> omniroute chat "reply with exactly: pong" --model cc/claude-opus-4-8
```

Restart every Claude Code process; environment is read at startup. `ANTHROPIC_BASE_URL` must not end in `/v1`.

## User questions and permissions

Run the main conversation in a mode that allows user interaction. Claude's `dontAsk` mode denies `AskUserQuestion` even if allowlisted; use a normal interactive mode or relay the question as plain text. A tool permission denial is not a user answer. Do not change local permissions or bypass host approval controls automatically.

## Troubleshooting

- Node warning: `nvm use 22`; verify `node --version`.
- `omniroute` missing after restart: fix the nvm block/default alias.
- Models missing: remove `/v1`; fully restart process.
- `@/shared`/`@/lib` crash: known commands above; use manual environment.
- Ambiguous model: set all `ANTHROPIC_DEFAULT_*_MODEL` to available provider-prefixed IDs; restart.
