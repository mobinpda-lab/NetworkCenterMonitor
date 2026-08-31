from pathlib import Path

workflow = Path('.github/workflows/production-orchestrator.yml').read_text(encoding='utf-8')

required_fragments = [
    "cron: '*/5 * * * *'",
    "workflows: ['Android CI', 'Android Device Smoke', 'Android Security']",
    "AUTO_LABEL = 'ncm-auto'",
    "AUTO_MARKER = 'NCM-AUTO: TRUE'",
    "run.status !== 'completed' || run.conclusion !== 'success'",
    "run.head_sha !== headSha",
    "linked?.base?.sha === mainSha",
    "beforeMergeMain.data.commit.sha !== mainSha",
    "locked.head.sha !== headSha",
    "locked.mergeable !== true",
    "merge_method: 'squash'",
    "sha: headSha",
    "return; // serial production promotion: one merge per invocation",
]

missing = [fragment for fragment in required_fragments if fragment not in workflow]
if missing:
    raise SystemExit('Missing orchestrator contract fragments:\n- ' + '\n- '.join(missing))

for forbidden in ['force: true', 'git push --force', 'conclusion === \'skipped\'']:
    if forbidden in workflow:
        raise SystemExit(f'Forbidden orchestrator behavior found: {forbidden}')

print('Production Orchestrator contract OK')
