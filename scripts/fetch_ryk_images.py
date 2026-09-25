#!/usr/bin/env python3
"""Download all card images from Ryan Kingston's Armada Fleet Builder."""
import json, os, urllib.request, sys, concurrent.futures

BASE="https://armada.ryankingston.com/img/cards/"
WORK="/root/.hermes/cache/browser-use/workspace"
OUT=os.path.join(WORK,"ryk_imgs")
os.makedirs(OUT, exist_ok=True)

data=json.load(open(os.path.join(WORK,"ryk_full.json")))
imgs=set()
for key, lst in data.items():
    if isinstance(lst, list):
        for c in lst:
            if isinstance(c,dict) and c.get("image"):
                imgs.add(c["image"])
print("total unique:", len(imgs))


def dl(name):
    p=os.path.join(OUT,name)
    if os.path.exists(p) and os.path.getsize(p)>500: return (name,"cached")
    req=urllib.request.Request(BASE+name, headers={"User-Agent":"Mozilla/5.0"})
    try:
        r=urllib.request.urlopen(req,timeout=30).read()
        if len(r)>500:
            open(p,"wb").write(r); return (name,"ok")
        return (name,"tiny")
    except Exception as e:
        return (name,f"err:{e.__class__.__name__}")

with concurrent.futures.ThreadPoolExecutor(max_workers=16) as ex:
    results=list(ex.map(dl, sorted(imgs)))

stat={}
for name,st in results: stat[st]=stat.get(st,0)+1
print(stat)
