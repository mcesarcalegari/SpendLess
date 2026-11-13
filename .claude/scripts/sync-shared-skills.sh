#!/usr/bin/env bash
# Links skills and agents from the shared Skills repo (.claude/.agents/Skills)
# into .claude/skills/ and .claude/agents/ so Claude Code discovers them.
#
# - Adds a relative symlink for every skills/<name>/SKILL.md and agents/*.md.
# - Removes symlinks that point into the Skills repo but whose target is gone.
# - Never touches real (non-symlink) files or directories; reports name clashes.
# - Keeps the managed links out of git via a block in .git/info/exclude.
#
# - Fast-forwards the Skills repo first (see below), so sessions use the latest.
#
# Runs automatically from the Claude Code SessionStart hook and from the Skills
# repo's git post-merge/post-checkout/post-rewrite hooks. Safe to run by hand.
set -u

project_dir="$(cd "$(dirname "$0")/../.." && pwd)"
claude_dir="$project_dir/.claude"
repo_rel=".agents/Skills"           # relative to .claude/
repo_dir="$claude_dir/$repo_rel"

repo_url="https://github.com/mcesarcalegari/Skills.git"

if [ ! -d "$repo_dir/.git" ]; then
  # stdout of a SessionStart hook reaches Claude, which relays it to the dev.
  echo "Shared skills are not installed. See README.md > Setup, or run:"
  echo "  git clone $repo_url .claude/.agents/Skills"
  exit 0
fi

# Fast-forward the Skills repo to its upstream. Skipped when invoked from the
# repo's own git hooks (they run right after a pull), when the tree has local
# changes, or when offline; never prompts for credentials, never blocks long.
if [ -z "${SYNC_SHARED_SKILLS_NO_PULL:-}" ] \
  && git -C "$repo_dir" rev-parse --abbrev-ref --symbolic-full-name '@{u}' >/dev/null 2>&1 \
  && [ -z "$(git -C "$repo_dir" status --porcelain --untracked-files=no)" ]; then
  # perl = portable 8s timeout (macOS has no `timeout`); kills git, exits 124.
  if ! SYNC_SHARED_SKILLS_NO_PULL=1 GIT_TERMINAL_PROMPT=0 \
       GIT_SSH_COMMAND="ssh -o BatchMode=yes -o ConnectTimeout=5" \
       perl -e '$p = fork // exit 1; exec @ARGV[1..$#ARGV] or exit 127 unless $p;
                $SIG{ALRM} = sub { kill "TERM", $p; exit 124 }; alarm $ARGV[0];
                waitpid $p, 0; exit($? >> 8)' 8 \
       git -C "$repo_dir" pull --ff-only --quiet >/dev/null 2>&1; then
    echo "sync-shared-skills: could not update .claude/.agents/Skills (offline or diverged); using local copy" >&2
  fi
fi

mkdir -p "$claude_dir/skills" "$claude_dir/agents"
managed=()
added=0 removed=0

# Remove dangling links that we own (target inside the Skills repo).
prune() {
  local dir="$1" link target
  for link in "$dir"/*; do
    [ -L "$link" ] || continue
    target="$(readlink "$link")"
    case "$target" in *"$repo_rel/"*) ;; *) continue ;; esac
    if [ ! -e "$link" ]; then rm -f "$link"; removed=$((removed + 1)); fi
  done
}

# link <dest> <relative-target>
link() {
  local dest="$1" target="$2"
  if [ -L "$dest" ]; then
    [ "$(readlink "$dest")" = "$target" ] || { rm -f "$dest"; ln -s "$target" "$dest"; }
  elif [ -e "$dest" ]; then
    echo "sync-shared-skills: skipped '${dest#"$project_dir"/}' (a local copy exists; remove it to use the shared one)" >&2
    return
  else
    ln -s "$target" "$dest"; added=$((added + 1))
  fi
  managed+=("/${dest#"$project_dir"/}")
}

prune "$claude_dir/skills"
prune "$claude_dir/agents"

for skill in "$repo_dir"/skills/*/; do
  [ -f "$skill/SKILL.md" ] || continue
  name="$(basename "$skill")"
  link "$claude_dir/skills/$name" "../$repo_rel/skills/$name"
done

for agent in "$repo_dir"/agents/*.md; do
  [ -f "$agent" ] || continue
  name="$(basename "$agent")"
  link "$claude_dir/agents/$name" "../$repo_rel/agents/$name"
done

# Rewrite the managed block in .git/info/exclude (local-only ignore).
exclude="$project_dir/.git/info/exclude"
if [ -d "$project_dir/.git/info" ]; then
  begin="# >>> sync-shared-skills (managed) >>>"
  end="# <<< sync-shared-skills (managed) <<<"
  tmp="$(mktemp)"
  [ -f "$exclude" ] && awk -v b="$begin" -v e="$end" '$0==b{skip=1} !skip{print} $0==e{skip=0}' "$exclude" > "$tmp"
  {
    echo "$begin"
    for p in ${managed[@]+"${managed[@]}"}; do echo "$p"; done
    echo "$end"
  } >> "$tmp"
  mv "$tmp" "$exclude"
fi

if [ $((added + removed)) -gt 0 ]; then
  echo "sync-shared-skills: +$added linked, -$removed removed"
fi
exit 0
