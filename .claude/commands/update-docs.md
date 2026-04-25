Update BOCRM project documentation to reflect recent changes.

Steps:
1. Run `git log --oneline -15` to see recent commits
2. Read the current `memory/MEMORY.md`
3. For each recent commit that introduced a new feature, fixed a significant bug, or changed an architectural pattern — check if it's already documented
4. Update `MEMORY.md` with anything missing: new features, new files, changed patterns, new API endpoints
5. Check if `CLAUDE.md` needs any rule updates (new "what not to do" patterns, new entity types, new conventions)

Focus on:
- New entities or services added
- New API endpoints
- Changed patterns or conventions
- New environment variables or config
- Bug fixes that imply a rule (e.g. "don't do X because it caused Y")

Do NOT add ephemeral task details — only durable patterns and facts that future sessions need.
