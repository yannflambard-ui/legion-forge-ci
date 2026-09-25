#!/usr/bin/env python3
"""Rename card images into a clean folder structure:
    assets/cards/<game>/<faction>/<official-english-name>.webp
where game = swa (Armada) | swl (Legion), faction = empire/rebel/republic/separatist/...
Same-name variants (different points/rank) get a disambiguating suffix.
Updates imageAssetPath in catalog.json accordingly and moves the webp files.
"""
import json, os, re, shutil
from collections import defaultdict

ROOT = os.path.join(os.path.dirname(__file__), "..", "app", "src", "main", "assets")
CATALOG = os.path.join(ROOT, "catalog.json")
CARDS = os.path.join(ROOT, "cards")

def slug(s):
    s = s.lower().replace("'", "").replace('"', "")
    return re.sub(r"[^a-z0-9]+", "-", s).strip("-")

def norm_faction(f):
    return {"separatists": "separatist", "shadow_collective": "shadow-collective"}.get(f, f)

def main():
    d = json.load(open(CATALOG, encoding="utf-8"))
    cards = d["cards"]

    # 1) compute target path per card
    targets = []  # (card, game, faction, slug, points)
    for x in cards:
        if not x.get("imageAssetPath"):
            continue
        game = "swl" if x["gameSystem"] == "LEGION_V2" else "swa"
        fac = norm_faction(x["factionId"])
        targets.append((x, game, fac, slug(x["name"]), x.get("points", 0)))

    # 2) detect collisions per (game, faction, slug)
    groups = defaultdict(list)
    for t in targets:
        groups[(t[1], t[2], t[3])].append(t)

    # 3) assign final filenames
    plan = []  # (card, old_rel, new_rel)
    for key, items in groups.items():
        game, fac, sl = key
        if len(items) == 1:
            card, _, _, _, _ = items[0]
            new_rel = os.path.join("cards", game, fac, sl + ".webp")
            plan.append((card, card["imageAssetPath"], new_rel))
        else:
            # disambiguate by points (stable, distinguishes variants)
            for card, _, _, _, pts in items:
                new_rel = os.path.join("cards", game, fac, f"{sl}-{pts}.webp")
                plan.append((card, card["imageAssetPath"], new_rel))

    # 4) copy files (shared sources) + update catalog
    moved = 0
    for card, old_rel, new_rel in plan:
        old_abs = os.path.join(ROOT, old_rel)
        new_abs = os.path.join(ROOT, new_rel)
        if not os.path.exists(old_abs):
            print("WARN missing source:", old_rel)
            continue
        os.makedirs(os.path.dirname(new_abs), exist_ok=True)
        if os.path.abspath(old_abs) != os.path.abspath(new_abs):
            shutil.copy2(old_abs, new_abs)
        card["imageAssetPath"] = new_rel
        moved += 1

    # 5) remove old flat files (top-level cards/*.webp) no longer referenced
    referenced = {os.path.join(ROOT, c["imageAssetPath"]) for c in cards if c.get("imageAssetPath")}
    for f in os.listdir(CARDS):
        p = os.path.join(CARDS, f)
        if os.path.isfile(p) and p not in referenced:
            os.remove(p)

    # 6) remove now-empty old dirs
    for root, dirs, files in os.walk(CARDS, topdown=False):
        if root != CARDS and not os.listdir(root):
            os.rmdir(root)

    json.dump(d, open(CATALOG, "w", encoding="utf-8"), ensure_ascii=False, indent=1)
    print("moved:", moved)
    print("total cards:", len(cards))

if __name__ == "__main__":
    main()
