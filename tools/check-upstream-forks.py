#!/usr/bin/env python3
"""Compare les branches notées dans docs/upstream-forks.md à GitHub.

Liste les branches qui ont bougé et les forks apparus depuis le relevé.
Sortie 0 si rien n'a changé, 1 sinon. Demande `git` et `gh`.
"""
import json
import pathlib
import re
import subprocess
import sys

ROOT_REPOSITORY = "Matroska-Org/jebml"
JIMBE_REPOSITORY = "kangaroo-and-rabbit/jimbe"
AUDIT = pathlib.Path(__file__).resolve().parent.parent / "docs" / "upstream-forks.md"
ROW = re.compile(r"^\| (\S+/\S+) \| (\S+) \| `([0-9a-f]{40})` \|", re.MULTILINE)


def forks_of(repository):
    """Tous les forks, forks de forks compris."""
    output = subprocess.check_output(
        ["gh", "api", "--paginate", f"repos/{repository}/forks?per_page=100", "--jq", ".[].full_name"], text=True
    )
    found = set()
    for fork in output.split():
        found.add(fork)
        found |= forks_of(fork)
    return found


def branches_of(repository):
    """Les branches du dépôt et leur dernier commit, None si le dépôt a disparu."""
    url = f"https://github.com/{repository}.git"
    result = subprocess.run(["git", "ls-remote", "--heads", url], text=True, capture_output=True)
    if result.returncode != 0:
        return None
    lines = (line.split("\t") for line in result.stdout.splitlines())
    return {ref.removeprefix("refs/heads/"): commit for commit, ref in lines}


def main():
    noted = {}
    for repository, branch, commit in ROW.findall(AUDIT.read_text(encoding="utf-8")):
        noted.setdefault(repository, {})[branch] = commit

    current = forks_of(ROOT_REPOSITORY) | {ROOT_REPOSITORY}
    current.discard(JIMBE_REPOSITORY)
    changes = [f"NOUVEAU FORK  {repository}" for repository in sorted(current - noted.keys())]

    for repository in sorted(noted):
        if repository not in current:
            changes.append(f"DISPARU       {repository}")
            continue
        branches = branches_of(repository)
        if branches is None:
            changes.append(f"ILLISIBLE     {repository}")
            continue
        for branch in sorted(noted[repository].keys() | branches.keys()):
            before = noted[repository].get(branch)
            after = branches.get(branch)
            if before != after:
                changes.append(f"BRANCHE       {repository} {branch}: {before} -> {after}")

    print("\n".join(changes) if changes else f"Rien de nouveau sur {len(noted)} dépôts.")
    return 1 if changes else 0


if __name__ == "__main__":
    sys.exit(main())
