import json, os, subprocess, time, urllib.request
from pathlib import Path

API='https://api.openai.com/v1/responses'
MODEL=os.getenv('OPENAI_MODEL','gpt-5.6')
MAX_FILES=int(os.getenv('ASF_MAX_FILES','80'))
MAX_DIFF=int(os.getenv('ASF_MAX_DIFF_CHARS','50000'))
MAX_ATTEMPTS=int(os.getenv('ASF_MAX_ATTEMPTS','3'))

def run(cmd, check=False, timeout=300):
    return subprocess.run(cmd,text=True,capture_output=True,check=check,timeout=timeout)

def gh(path):
    req=urllib.request.Request('https://api.github.com'+path,headers={'Authorization':'Bearer '+os.environ['GITHUB_TOKEN'],'Accept':'application/vnd.github+json','X-GitHub-Api-Version':'2022-11-28'})
    with urllib.request.urlopen(req,timeout=30) as r:return json.load(r)

def ai(prompt):
    key=os.getenv('OPENAI_API_KEY','').strip()
    if not key: raise RuntimeError('OPENAI_API_KEY is not configured; fail-closed')
    payload={'model':MODEL,'input':prompt,'temperature':0}
    req=urllib.request.Request(API,data=json.dumps(payload).encode(),headers={'Authorization':'Bearer '+key,'Content-Type':'application/json'},method='POST')
    with urllib.request.urlopen(req,timeout=180) as r:data=json.load(r)
    text=data.get('output_text','')
    if not text:
        text='\n'.join(c.get('text','') for i in data.get('output',[]) for c in i.get('content',[]) if c.get('type')=='output_text')
    return text.strip()

def inventory():
    r=run(['git','ls-files'],timeout=60)
    files=r.stdout.splitlines()[:MAX_FILES]
    chunks=[]
    for f in files:
        p=Path(f)
        if p.is_file() and p.stat().st_size<30000:
            try: chunks.append('\n===== '+f+' =====\n'+p.read_text(encoding='utf-8',errors='replace'))
            except Exception: pass
    return ''.join(chunks)[-180000:]

def test_project():
    if Path('gradlew').exists():
        cmds=[['./gradlew','test'],['./gradlew','assembleDebug']]
    elif Path('pubspec.yaml').exists():
        cmds=[['flutter','pub','get'],['flutter','analyze'],['flutter','test']]
    else: return True,'No supported project manifest'
    out=[]
    for c in cmds:
        r=run(c,timeout=600);out.append('$ '+' '.join(c)+'\n'+r.stdout[-10000:]+'\n'+r.stderr[-10000:])
        if r.returncode:return False,'\n'.join(out)
    return True,'\n'.join(out)

def patch_from(text):
    s=(text or '').replace('\r','').strip();i=s.find('diff --git ')
    if i<0:return ''
    return s[i:].split('```')[0].strip()

def apply(patch):
    if not patch or len(patch)>MAX_DIFF:return False,'empty/oversize diff'
    if not patch.startswith('diff --git '):return False,'invalid diff header'
    Path('/tmp/asf.patch').write_text(patch+'\n',encoding='utf-8')
    c=run(['git','apply','--check','--recount','/tmp/asf.patch'],timeout=60)
    if c.returncode:return False,c.stderr[-12000:]
    c=run(['git','apply','--recount','--whitespace=fix','/tmp/asf.patch'],timeout=60)
    return (c.returncode==0,c.stderr[-12000:] if c.returncode else 'applied')

def main():
    n=os.environ['ASF_ISSUE_NUMBER']; repo=os.environ['GITHUB_REPOSITORY']
    issue=gh(f'/repos/{repo}/issues/{n}')
    title=issue.get('title','');body=issue.get('body','') or ''
    context=inventory();failure=''
    for attempt in range(1,MAX_ATTEMPTS+1):
        prompt=f'''You are the bounded NetworkCenterMonitor Code Worker. Implement ONLY GitHub issue #{n}.\nTitle: {title}\nBody: {body}\nRepository context:{context}\nPrevious failure:{failure}\nReturn ONLY a complete unified git diff beginning with diff --git. Do not use markdown fences. Keep scope minimal. Do not change CI permissions, secrets, authentication, release policy, or production gates. Add focused tests when appropriate. If unsafe/underspecified, return empty.'''
        try: patch=patch_from(ai(prompt))
        except Exception as e:
            failure=str(e);continue
        ok,msg=apply(patch)
        if not ok:
            run(['git','reset','--hard','HEAD']);failure='Patch rejected: '+msg;continue
        passed,evidence=test_project()
        if passed:return 0
        failure='Validation failed:\n'+evidence[-16000:]+'\nPatch:\n'+run(['git','diff']).stdout[-16000:]
        run(['git','reset','--hard','HEAD'])
    print(failure)
    return 1

if __name__=='__main__': raise SystemExit(main())
