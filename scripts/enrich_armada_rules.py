#!/usr/bin/env python3
"""Joint rulesText Armada depuis armada_bsdata_catalog.json vers catalog.json.

Le catalogue principal n'a pas le texte des cartes Armada (rulesText=null pour
204 upgrades, 46 ships, 76 squadrons, 24 commandants). La source BSData contient
ce texte riche. Match par nom normalisé (minuscules sans accents/parenthèses) et
faction (BSData use 'galactic-empire' vs catalogue 'empire' → normaliser).
"""
from __future__ import annotations
import json, re
from pathlib import Path

CATALOG = Path("/opt/projects/swlegion-army-builder/app/src/main/assets/catalog.json")
BSDATA = Path("/opt/projects/swlegion-army-builder/app/src/main/assets/armada_bsdata_catalog.json")

# faction bsd -> catalogue (IDs catalogue virés des 'galactic-')
def fac_app(f): return (f or "").replace("galactic-", "").replace("-alliance", "")

def norm(s): return re.sub(r"[^a-z0-9]", "", (s or "").lower())

def levenshtein(a, b):
    if a == b: return 0
    if not a: return len(b)
    if not b: return len(a)
    prev = list(range(len(b) + 1))
    for i, ca in enumerate(a):
        cur = [i + 1]
        for j, cb in enumerate(b):
            cur.append(min(prev[j + 1] + 1, cur[j] + 1, prev[j] + (ca != cb)))
        prev = cur
    return prev[-1]

def render_bsdata_text(raw: str) -> str:
    """Convertit le JSON BSData brut (profiles/characteristics) en texte lisible."""
    try:
        o = json.loads(raw)
    except Exception:
        return raw
    if not isinstance(o, dict):
        return raw
    lines = []
    profs = o.get("profiles") or [o]
    if isinstance(profs, dict): profs = [profs]
    for p in profs:
        if not isinstance(p, dict): continue
        name = p.get("name")
        if name: lines.append(f"[{name}]")
        chars = p.get("characteristics") or {}
        if isinstance(chars, dict):
            for k, v in chars.items():
                if v in (None, "", "None"): continue
                lines.append(f"{k}: {v}")
        crew = p.get("crew") or p.get("command-value")
        if crew: lines.append(f"Crew: {crew}")
    # infoLinks de règles
    links = o.get("infoLinks") or []
    for lk in links if isinstance(links, list) else []:
        if isinstance(lk, dict) and lk.get("name"):
            lines.append(f"→ {lk['name']}")
    text = "\n".join(lines)
    # fallback: si rien d'extractible, garder le JSON
    return text.strip() or raw

def main():
    doc = json.loads(CATALOG.read_text())
    bdoc = json.loads(BSDATA.read_text())
    bc = bdoc.get("cards", bdoc)

    # index BSData: par kind -> (nom_norm, faction_norm) -> card
    index = {}
    for c in bc:
        kind = c["kind"]
        k = (norm(c["name"]), norm(fac_app(c.get("factionId"))))
        index.setdefault(kind, {})[k] = c

    filled = {}; missing = []
    for c in doc["cards"]:
        if c["gameSystem"] != "ARMADA_V15":
            continue
        kind = c["kind"]
        # Repasse sur les rulesText déjà présents qui sont encore du JSON BSData brut
        # (commandants/squadrons Republic-Sep), hors ships qui ont shipStats séparé.
        existing = c.get("rulesText")
        if existing and existing.strip().startswith("{") and kind != "ARMADA_SHIP":
            c["rulesText"] = render_bsdata_text(existing)
            continue
        if existing:
            continue  # déjà rempli lisible
        # 1) nom + faction
        k1 = (norm(c["name"]), norm(fac_app(c.get("factionId"))))
        # 2) nom seul (factions diffèrent parfois)
        k2 = (norm(c["name"]), "")
        found = index.get(kind, {}).get(k1) or index.get(kind, {}).get(k2) or \
                next((v for (n, f), v in index[kind].items() if n == norm(c["name"])), None)
        # Fuzzy exact (sous-ensemble) pour "Boba Fett" ⊆ "Boba Fett (Slave I)".
        if not (found and found.get("rulesText")):
            cname = norm(c["name"])
            pool = index.get(kind, {})
            for (n, f), v in pool.items():
                if v.get("rulesText") and cname and (cname in n or n in cname):
                    found = v; break
        # Typo de petite distance (≤2) : Shriv Suurgav→Surgaav, Defense Liaison→Liason,
        # Magnite Crysal→Crystal, Corrupter→Corruptor. Le seuil 2 exclut "Bail Organa"→"Leia Organa" (dist 3).
        if not (found and found.get("rulesText")):
            cname = norm(c["name"])
            for (n, f), v in index.get(kind, {}).items():
                if v.get("rulesText") and cname and abs(len(n) - len(cname)) <= 2 \
                   and levenshtein(cname, n) <= 2:
                    found = v; break
        if found and found.get("rulesText"):
            c["rulesText"] = render_bsdata_text(found["rulesText"])
            filled[kind] = filled.get(kind, 0) + 1
        else:
            missing.append((c["name"], c["factionId"], kind))

    CATALOG.write_text(json.dumps(doc, ensure_ascii=False, indent=2) + "\n")
    from collections import Counter
    print(json.dumps({"remplis": dict(Counter(filled)), "total_filled": sum(filled.values())}, indent=2))
    print("non trouvés:", len(missing))
    for m in missing[:30]:
        print("   ", m)

if __name__ == "__main__":
    main()
