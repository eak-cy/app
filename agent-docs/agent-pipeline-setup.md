# Agent pipeline setup

Both Codex and Claude Code use the same [feature workflow](../.agents/commands/feature.md): quick PO brief → EM technical handoff → one Lead implements → EM reviews and marks done. All roles follow the same [question and efficiency rules](../.agents/contracts/workflow.md).

## Shared sources and host adapters

- `AGENTS.md` is the repository instruction source; `CLAUDE.md` is a relative symlink to it.
- `.agents/{agents,contracts,commands,skills}/` owns shared guidance. Matching `.claude/` files are relative symlinks; edit the source, not copies.
- `.codex/agents/*.toml` contains native metadata/model settings and points to the same role Markdown, ignoring its Claude YAML frontmatter. Keep behavioral instructions in `.agents/`.
- Both hosts use the same `agent-docs/`, `pages/epics/`, and `docs/`. No per-host product specifications.
- Local credentials/settings stay gitignored. Codex does not need OmniRoute. The optional Claude routing setup below is per-engineer infrastructure.

Codex project roles use the [official TOML agent format](https://learn.chatgpt.com/docs/agent-configuration/subagents). Claude uses [Markdown subagents](https://code.claude.com/docs/en/sub-agents). These are host adapters, not interchangeable configuration formats.

## Run

Open the repository in a fresh session after changing agent profiles. In Claude Code use `/feature "<description>"`. In Codex say `Use the feature workflow for <description>`; AGENTS.md routes this to the shared command. A literal `/feature` request in Codex has the same repository meaning, but a native slash-menu entry is not required or promised.

The main conversation spawns/resumes each role and relays questions and Lead↔EM collaboration. Claude subagents do not have `AskUserQuestion`; a role returns `USER_QUESTION` and the main conversation asks you, then returns your answer. Use the same relay in Codex whenever direct user questions are unavailable. Do not depend on a host-specific task API or nested agents. If subagents are unavailable, the main conversation performs the roles sequentially and says so. Ordinary ChatGPT chat without repository/tool access cannot automatically load these files; this setup targets Codex and repository-enabled agent sessions.

## Roles and routing

| Role | Claude | Codex | Ownership |
|---|---|---|---|
| Product Owner | `sonnet` | parent default | product requirements, scope, acceptance, all `pages/` documentation |
| Engineering Manager | `sonnet` | parent default | requirements/code analysis, agreed approach, engineering docs outside `pages/`, code review |
| Lead LOW | `haiku` | `gpt-5.6-terra`/low | bounded implementation |
| Lead MEDIUM | `sonnet` | `gpt-5.6-sol`/high | contained feature/integration work |
| Lead HIGH | `opus` | `gpt-5.6-sol`/xhigh | high-risk implementation |

EM uses the [complexity contract](../.agents/contracts/complexity.md). Keep model selection separate from shared behavior; native model availability depends on the host/account. PO no longer writes a finished epic before handoff. EM prepares engineering documentation outside `pages/`; PO owns and updates all epics and other documentation under `pages/`. Before Lead implementation, both update affected docs from the agreed requirements and approach, marking pending work explicitly. Lead implements code/tests and reports results; it never updates docs. EM reviews the actual code, updates engineering docs, and sends the Lead's output and verified findings to PO for page updates. EM confirms both sets match the implementation before marking done, per delivered slice/PR, accepting minor disclosed polish follow-ups. Required checks and agreed behavior still matter.

Each role asks about unresolved preferences, including implementation choices with meaningful tradeoffs. Questions go straight to the user through the main conversation when needed, not through a PO↔EM loop. Prior answers carry forward. EM must obtain explicit user agreement to the brief plan before handing implementation to the complexity-selected Lead. Material plan changes require renewed agreement; any role, including Lead, can raise new questions at any time. No extra formal planning stage or repeated full-package task dispatch.

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
