# SpendLess

Offline-first Android app. Architecture, conventions, and agent guidance live in [`AGENTS.md`](./AGENTS.md).

## Setup

### Shared AI skills and agents (one-time)

The Claude Code skills and agents used in this project come from a separate repo, which is **not** versioned here. Clone it into `.claude/.agents/Skills` from the project root:

```sh
git clone https://github.com/mcesarcalegari/Skills.git .claude/.agents/Skills
```

That's it. Every Claude Code session start then:

- fast-forwards the Skills repo to the latest `main` (skipped when offline or when you have local edits in it), and
- links its skills into `.claude/skills/` and its agents into `.claude/agents/` (`.claude/scripts/sync-shared-skills.sh`).

Accept the "trust this folder" prompt the first time you open the project in Claude Code, otherwise the session-start hook won't run.

Optional, if you pull the Skills repo by hand mid-session and want the links refreshed immediately:

```sh
cd .claude/.agents/Skills/.git/hooks
for h in post-merge post-checkout post-rewrite; do
  printf '#!/bin/sh\nscript="$(git rev-parse --show-toplevel)/../../scripts/sync-shared-skills.sh"\n[ -x "$script" ] && SYNC_SHARED_SKILLS_NO_PULL=1 "$script"\nexit 0\n' > "$h"
  chmod +x "$h"
done
```
