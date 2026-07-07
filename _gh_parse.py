import sys, json
data = json.load(sys.stdin)
for r in data.get("items", []):
    created = (r.get("created_at") or "")[:10] or "N/A"
    desc = (r.get("description") or "")[:120]
    print(f"{r['full_name']} | stars:{r['stargazers_count']} | lang:{r.get('language','N/A')} | created:{created} | {desc}")
