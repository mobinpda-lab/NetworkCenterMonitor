import json, math, os, re, subprocess, time, urllib.request
from pathlib import Path

API = "https://api.openai.com/v1/responses"
MODEL = os.getenv("OPENAI_MODEL", "gpt-5.6")
MAX_FILES = int(os.getenv("ASF_MAX_CONTEXT_FILES", "60"))
MAX_FILE_CHARS = int(os.getenv("ASF_MAX_FILE_CHARS", "6000"))
MAX_CONTEXT_CHARS = int(os.getenv("ASF_MAX_CONTEXT_CHARS", "120000"))
MAX_DIFF = int(os.getenv("ASF_MAX_DIFF_CHARS", "50000"))
MAX_FIX_ATTEMPTS = int(os.getenv("ASF_MAX_FIX_ATTEMPTS", "3"))
PROVIDER_TIMEOUT = int(os.getenv("ASF_PROVIDER_TIMEOUT_SECONDS", "180"))
PROVIDER_BUDGET = int(os.getenv("ASF_PROVIDER_BUDGET_SECONDS", "540"))
LEASE_SECONDS = int(os.getenv("ASF_LEASE_SECONDS", "1800"))
SECRET_PATTERNS = re.compile(r"(^|/)(\.env|.*\.pem|.*\.p12|.*\.jks|.*\.keystore|.*credentials.*|.*secret.*)$", re.I)
TEXT_EXTENSIONS = {".kt", ".kts", ".java", ".xml", ".gradle", ".yaml", ".yml", ".json", ".md", ".txt", ".properties", ".toml", ".py", ".sh"}

def run(cmd, check=True, timeout=None):
    return subprocess.run(cmd, text=True, capture_output=True, check=check, timeout=timeout)

def github_json(url):
    req = urllib.request.Request(url, headers={"Authorization": f"Bearer {os.environ['GITHUB_TOKEN']}", "Accept": "application/vnd.github+json", "X-GitHub-Api-Version": "2022-11-28"})
    with urllib.request.urlopen(req, timeout=30) as response:
        return json.load(response)

def post_comment(issue_number, body):
    repo = os.environ["GITHUB_REPOSITORY"]
    req = urllib.request.Request(f"https://api.github.com/repos/{repo}/issues/{issue_number}/comments", data=json.dumps({"body": body}).encode(), headers={"Authorization": f"Bearer {os.environ['GITHUB_TOKEN']}", "Accept": "application/vnd.github+json", "X-GitHub-Api-Version": "2022-11-28", "Content-Type": "application/json"}, method="POST")
    with urllib.request.urlopen(req, timeout=30):
        pass

def lease(issue_number):
    now = int(time.time())
    marker = f"<!-- asf-lease:{issue_number} -->"
    comments = github_json(f"https://api.github.com/repos/{os.environ['GITHUB_REPOSITORY']}/issues/{issue_number}/comments?per_page=100")
    for comment in comments:
        body = comment.get("body") or ""
        if marker in body:
            match = re.search(r"expires=(\d+)", body)
            if match and int(match.group(1)) > now:
                return
    post_comment(issue_number, f"{marker}\nworker={os.getenv('GITHUB_RUN_ID','unknown')}\nexpires={now + LEASE_SECONDS}\nstatus=RUNNING")

def openai_response(prompt, timeout_seconds):
    key = os.getenv("OPENAI_API_KEY", "").strip()
    if not key:
        raise RuntimeError("WAITING_AI_PROVIDER: OPENAI_API_KEY is not configured")
    payload = {"model": MODEL, "input": prompt}
    req = urllib.request.Request(API, data=json.dumps(payload).encode(), headers={"Authorization": f"Bearer {key}", "Content-Type": "application/json"}, method="POST")
    with urllib.request.urlopen(req, timeout=timeout_seconds) as response:
        data = json.load(response)
    text = data.get("output_text") or "\n".join(c.get("text", "") for item in data.get("output", []) for c in item.get("content", []) if c.get("type") == "output_text")
    return (text or "").strip()

def build_context(title, body):
    terms = set(re.findall(r"[A-Za-z0-9_]{3,}", f"{title} {body}".lower()))
    paths = []
    for path in run(["git", "ls-files"], check=False).stdout.splitlines():
        p = Path(path)
        if SECRET_PATTERNS.search(path) or p.suffix.lower() not in TEXT_EXTENSIONS:
            continue
        low = path.lower()
        score = (20 if low in {"readme.md", "build.gradle.kts", "settings.gradle.kts"} else 0) + (8 if low.startswith("docs/") else 0) + (4 if any(t in low for t in terms) else 0) + (3 if any(x in low for x in ("app/", "src/", "test/", ".github/")) else 0)
        paths.append((score, path))
    paths.sort(key=lambda x: (-x[0], x[1]))
    out, total = [], 0
    for _, path in paths[:MAX_FILES]:
        try:
            text = Path(path).read_text(encoding="utf-8")[:MAX_FILE_CHARS]
        except (OSError, UnicodeDecodeError):
            continue
        chunk = f"\n===== {path} =====\n{text}\n"
        if total + len(chunk) > MAX_CONTEXT_CHARS:
            break
        out.append(chunk)
        total += len(chunk)
    return "".join(out)

def project_test():
    if Path("gradlew").is_file():
        commands = [["./gradlew", "test"]]
    elif Path("pubspec.yaml").is_file():
        commands = [["flutter", "pub", "get"], ["flutter", "analyze"], ["flutter", "test"]]
    elif Path("package.json").is_file():
        commands = [["npm", "ci"], ["npm", "test", "--if-present"]]
    else:
        return True, "No supported project manifest."
    evidence = []
    for command in commands:
        result = run(command, check=False, timeout=900)
        evidence.append(f"$ {' '.join(command)}\n{result.stdout}\n{result.stderr}")
        if result.returncode != 0:
            return False, "\n".join(evidence)[-30000:]
    return True, "\n".join(evidence)[-30000:]

def normalize_diff(text):
    value = (text or "").replace("\r\n", "\n").replace("\r", "\n").lstrip("\ufeff").strip()
    start = value.find("diff --git ")
    if start < 0:
        return value
    lines = []
    for line in value[start:].splitlines():
        if lines and line.strip() in {"```", "~~~"}:
            break
        lines.append(line)
    return "\n".join(lines).strip()

def validate_diff(diff):
    diff = normalize_diff(diff)
    if not diff.startswith("diff --git "):
        return False, "Patch must start with diff --git"
    if len(diff) > MAX_DIFF:
        return False, "Patch exceeds safety limit"
    lines = diff.splitlines()
    starts = [i for i, line in enumerate(lines) if line.startswith("diff --git ")]
    if not starts or len(starts) > MAX_FILES:
        return False, "Invalid file count"
    starts.append(len(lines))
    for i in range(len(starts) - 1):
        section = lines[starts[i]:starts[i + 1]]
        minus = next((j for j, line in enumerate(section) if line.startswith("--- ")), None)
        plus = next((j for j, line in enumerate(section) if line.startswith("+++ ")), None)
        hunk = next((j for j, line in enumerate(section) if line.startswith("@@ ")), None)
        if minus is None or plus is None or hunk is None or not (minus < plus < hunk):
            return False, "Each file requires --- +++ and complete @@ hunks"
        for line in section:
            if line.startswith("+++ b/") and SECRET_PATTERNS.search(line[6:]):
                return False, "Secret-like path is not allowed"
    return True, "ok"

def apply_diff(diff):
    diff = normalize_diff(diff)
    ok, msg = validate_diff(diff)
    if not ok:
        return False, msg
    Path("/tmp/ncm-worker.patch").write_text(diff + "\n", encoding="utf-8")
    check = run(["git", "apply", "--check", "--recount", "/tmp/ncm-worker.patch"], check=False)
    if check.returncode != 0:
        return False, check.stderr[-12000:]
    applied = run(["git", "apply", "--recount", "--whitespace=fix", "/tmp/ncm-worker.patch"], check=False)
    if applied.returncode != 0:
        return False, applied.stderr[-12000:]
    return True, "patch applied"

def prompt(issue_number, title, body, context, failure):
    repair = f"\nPrevious failure evidence:\n{failure}\n" if failure else ""
    return f"""You are the bounded autonomous Code Worker for {os.environ['GITHUB_REPOSITORY']}. Implement ONLY GitHub Issue #{issue_number}.\nTitle: {title}\nIssue body:\n{body}\n{repair}\nRepository context (secret-safe, bounded):\n{context}\nRules: preserve the canonical NetworkCenterMonitor architecture and MASTER_SPEC; no new product scope; no secrets/credentials or unrelated CI controls; prefer the smallest safe implementation and focused tests; return ONLY a complete unified git diff beginning with diff --git; every changed file needs --- +++ and complete @@ hunks; return empty output if unsafe or underspecified."""

def main():
    raw = os.getenv("NCM_ISSUE_NUMBER") or os.getenv("ASF_ISSUE_NUMBER")
    if not raw:
        raise RuntimeError("NCM_ISSUE_NUMBER is required")
    issue_number = int(raw)
    expected = os.getenv("NCM_EXPECTED_MAIN_SHA") or os.getenv("ASF_EXPECTED_MAIN_SHA") or ""
    repo = os.environ["GITHUB_REPOSITORY"]
    main_sha = github_json(f"https://api.github.com/repos/{repo}/branches/main")["commit"]["sha"]
    if not expected or main_sha != expected:
        post_comment(issue_number, f"<!-- ncm-worker-result -->\nstatus=FAIL_CLOSED\nreason=MAIN_MOVED_OR_MISSING_LEASE\nexpected={expected}\nactual={main_sha}")
        return 7
    issue = github_json(f"https://api.github.com/repos/{repo}/issues/{issue_number}")
    labels = {x.get("name") for x in issue.get("labels", [])}
    body = issue.get("body", "") or ""
    if not ("NCM-AUTO: TRUE" in body or "ncm-auto" in labels):
        raise RuntimeError("Issue is not opted into NCM autonomous execution")
    lease(issue_number)
    title = issue.get("title", "")
    context = build_context(title, body)
    deadline = time.monotonic() + PROVIDER_BUDGET
    failure = ""
    for attempt in range(1, MAX_FIX_ATTEMPTS + 1):
        remaining = deadline - time.monotonic()
        if remaining <= 0:
            break
        try:
            diff = openai_response(prompt(issue_number, title, body, context, failure), max(1, min(PROVIDER_TIMEOUT, math.ceil(remaining))))
        except Exception as exc:
            failure = str(exc)
            continue
        if not diff:
            failure = "AI returned no actionable diff"
            continue
        ok, msg = apply_diff(diff)
        if not ok:
            run(["git", "reset", "--hard", "HEAD"], check=False)
            failure = f"Patch rejected: {msg}"
            continue
        passed, evidence = project_test()
        if passed:
            print("NCM_CODE_WORKER_PASS")
            return 0
        failed_patch = run(["git", "diff"], check=False).stdout[-20000:]
        run(["git", "reset", "--hard", "HEAD"], check=False)
        failure = f"Tests failed:\n{evidence}\nFailed patch:\n{failed_patch}"
    post_comment(issue_number, f"<!-- ncm-worker-result -->\nstatus=WAITING_AI_PROVIDER_OR_RETRY\nreason={failure[-5000:]}")
    return 6

if __name__ == "__main__":
    raise SystemExit(main())
