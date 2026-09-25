#!/usr/bin/env python3
"""Télécharge les images de cartes LEGION V2 (unit + upgrade) depuis le CDN
CloudFront `d2maxvwz12z6fm.cloudfront.net/<type>Cards/<imageName>` et les place
dans assets/cards/swl/<faction>/<slug>.webp, en mettant à jour imageAssetPath.

Les images V1 (LegionHQ v1) et V2 sont différentes : on REMPLACE par la V2.
Correspondance imageName depuis /tmp/legionhq_v2_full.json (bundle live 2.6).
"""
from __future__ import annotations
import json, os, re, urllib.parse
from concurrent.futures import ThreadPoolExecutor
from pathlib import Path

CDN = "https://d2maxvwz12z6fm.cloudfront.net"
BUNDLE = Path("/tmp/legionhq_v2_full.json")
CATALOG = Path("/opt/projects/swlegion-army-builder/app/src/main/assets/catalog.json")
ASSETS = Path("/opt/projects/swlegion-army-builder/app/src/main/assets/cards/swl")

def slug(s): return re.sub(r"-+", "-", re.sub(r"[^a-z0-9]+", "-", (s or "").lower())).strip("-") or "x"

def main():
    bundle = json.loads(BUNDLE.read_text())
    # imageName par (type, cardName) -> nom exact sur le CDN
    byname: dict[tuple, str] = {}
    for c in bundle:
        nm = (c.get("imageName") or "").strip()
        if nm:
            byname[(c.get("cardType"), c.get("cardName").lower())] = nm

    doc = json.loads(CATALOG.read_text())
    cards = doc["cards"]
    leg = [c for c in cards if c["gameSystem"] == "LEGION_V2"]
    todo = []
    import requests
    def worker(c):
        typ = "unit" if c["kind"] == "LEGION_UNIT" else "upgrade"
        nm = byname.get((typ, c["name"].lower()))
        if not nm:
            return c["name"], "no-imageName", None
        fac = slug(c["factionId"])
        dest = ASSETS / fac / f"{slug(c['name'])}.webp"
        dest.parent.mkdir(parents=True, exist_ok=True)
        try:
            r = requests.get(f"{CDN}/{'unit' if typ=='unit' else 'upgrade'}Cards/{urllib.parse.quote(nm)}", timeout=20)
            if r.status_code != 200 or "image" not in r.headers.get("content-type", ""):
                return c["name"], f"HTTP{r.status_code}", None
            dest.write_bytes(r.content)
            return c["name"], "OK", f"cards/swl/{fac}/{slug(c['name'])}.webp"
        except Exception as e:
            return c["name"], f"ERR {e}", None

    results = list(ThreadPoolExecutor(max_workers=16).map(worker, leg))
    ok = [r for r in results if r[1] == "OK"]
    bad = [r for r in results if r[1] != "OK"]
    # mettre à jour imageAssetPath des cartes OK
    okmap = {r[0]: r[2] for r in ok}
    for c in leg:
        if c["name"] in okmap:
            c["imageAssetPath"] = okmap[c["name"]]
    CATALOG.write_text(json.dumps(doc, ensure_ascii=False, indent=2) + "\n")
    print(json.dumps({"téléchargées": len(ok), "échec": len(bad)}, indent=2))
    if bad:
        print("échecs:", bad[:20])

if __name__ == "__main__":
    main()
