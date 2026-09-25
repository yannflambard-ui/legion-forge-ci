#!/usr/bin/env python3
"""Enrich catalog.json Armada ships/squadrons with fleet-builder stats.

Source of truth: armada_bsdata_catalog.json (BSData = the "Armada Fleet Builder"
dataset). catalog.json ships/squadrons have no stats by default. This merges
hull / shield / speed from BSData into a new `shipStats` JSON field on each
ship & squadron card, matched by normalized name (and faction).

Standalone run:  python3 scripts/enrich_armada_stats.py
Also importable: `from enrich_armada_stats import enrich; enrich(assets_dir, catalog_path)`
"""
import json, re, sys, os

ROOT = os.path.join(os.path.dirname(__file__), "..", "app", "src", "main", "assets")
CATALOG = os.path.join(ROOT, "catalog.json")
BSDATA = os.path.join(ROOT, "armada_bsdata_catalog.json")

def norm(s):
    n = re.sub(r"\s+", " ", s.lower().replace("-", " ")).strip()
    # BSData dataset consistently typos "Dreadnought" as "Dreadought"; canonicalize.
    n = n.replace("dreadought", "dreadnought")
    return n

def ship_stats(rt):
    """Extract hull/shield/maxSpeed/defense-tokens from a BSData ship rulesText (JSON str)."""
    try:
        d = json.loads(rt)
    except (TypeError, ValueError):
        return None
    hull = d.get("hull")
    shield = d.get("shield") or {}
    tokens = d.get("defense-tokens") or []
    # max speed = greatest numeric key across all speed-chart rows
    max_speed = None
    for row in d.get("speed-chart-rows", []) or []:
        for k in (row.get("values") or {}).keys():
            if k.isdigit() and (max_speed is None or int(k) > max_speed):
                max_speed = int(k)
    if hull is None:
        return None
    return {
        "hull": hull,
        "shield": {
            "front": shield.get("front", 0),
            "right": shield.get("right", 0),
            "left": shield.get("left", 0),
            "rear": shield.get("rear", 0),
        },
        "maxSpeed": max_speed or 1,
        "defenseTokens": tokens,
    }

def squadron_stats(rt):
    """Squadron BSData JSON has characteristics: Hull Value, Speed."""
    try:
        d = json.loads(rt)
    except (TypeError, ValueError):
        return None
    ch = (d.get("characteristics") or {}) if isinstance(d, dict) else {}
    hv = ch.get("Hull Value")
    sp = ch.get("Speed")
    if hv is None:
        return None
    return {"hull": int(hv), "speed": int(sp) if sp and str(sp).isdigit() else 3}

def faction_map():
    # main faction -> allowed bs data faction
    return {
        "empire": "galactic-empire",
        "rebel": "rebel-alliance",
        "republic": "galactic-republic",
        "separatist": "separatist-alliance",
        "neutral": None,
    }

def build_bs_lookup(bs):
    ships = {}
    squads = {}
    for c in bs:
        n = norm(c["name"])
        if c.get("kind") == "ARMADA_SHIP":
            ships.setdefault(n, []).append(c)
        elif c.get("kind") == "ARMADA_SQUADRON":
            squads.setdefault(n, []).append(c)
    return ships, squads


def fuzzy_pick(stats_lookup, cat_name, wanted_faction):
    """Exact name first, then fuzzy: BSData often appends the ship in parens
    (e.g. 'Boba Fett' -> 'Boba Fett (Slave I)') or has typos ('Dreadought')."""
    n = norm(cat_name)
    cands = stats_lookup.get(n)
    if cands:
        pick = next((c for c in cands if c.get("factionId") == wanted_faction), cands[0])
        return pick
    base = n.split("(")[0].strip()
    for key, items in stats_lookup.items():
        if key == base or key.startswith(base + " ") or base.startswith(key):
            pick = next((c for c in items if c.get("factionId") == wanted_faction), items[0])
            return pick
    return None


def enrich(assets_dir=None, catalog_path=None, bsdata_path=None):
    """Inject shipStats into catalog.json cards in place. Returns (ships, squadrons, unmatched)."""
    if assets_dir is None:
        assets_dir = ROOT
    catalog_path = os.path.abspath(catalog_path or os.path.join(os.path.abspath(assets_dir), "catalog.json"))
    bsdata_path = os.path.abspath(bsdata_path or os.path.join(os.path.abspath(assets_dir), "armada_bsdata_catalog.json"))

    catalog = json.load(open(catalog_path, encoding="utf-8"))
    bs = json.load(open(bsdata_path, encoding="utf-8"))
    if isinstance(bs, dict):
        bs = bs.get("cards", [])
    bs_ships, bs_squads = build_bs_lookup(bs)
    fm = faction_map()

    added = {"ship": 0, "squadron": 0}
    unmatched = []
    for card in catalog["cards"]:
        kind = card.get("kind")
        if kind not in ("ARMADA_SHIP", "ARMADA_SQUADRON"):
            continue
        fac = card.get("factionId")
        wanted = fm.get(fac)
        if kind == "ARMADA_SHIP":
            pick = fuzzy_pick(bs_ships, card["name"], wanted)
            if pick is None:
                unmatched.append(f"{kind}:{card['name']}")
                continue
            st = ship_stats(pick.get("rulesText"))
            if st is None:
                unmatched.append(f"{kind}(no stats):{card['name']}")
                continue
            st["kind"] = "SHIP"
            card["shipStats"] = json.dumps(st, separators=(",", ":"))
            added["ship"] += 1
        else:
            pick = fuzzy_pick(bs_squads, card["name"], wanted)
            if pick is None:
                unmatched.append(f"{kind}:{card['name']}")
                continue
            st = squadron_stats(pick.get("rulesText"))
            if st is None:
                unmatched.append(f"{kind}(no stats):{card['name']}")
                continue
            st["kind"] = "SQUADRON"
            card["shipStats"] = json.dumps(st, separators=(",", ":"))
            added["squadron"] += 1

    json.dump(catalog, open(catalog_path, "w", encoding="utf-8"), ensure_ascii=False, indent=1)
    return added["ship"], added["squadron"], unmatched


def main():
    ships, squads, unmatched = enrich()
    print("ships enriched:", ships)
    print("squadrons enriched:", squads)
    print("unmatched:", len(unmatched))
    for u in unmatched:
        print("   ", u)

if __name__ == "__main__":
    main()
