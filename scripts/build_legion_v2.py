#!/usr/bin/env python3
"""Build the Legion V2 portion of catalog.json from the LegionHQ V2 bundle
(main.7b20b86a.js, extracted to /tmp/legionhq_v2_full.json). Armada cards are
carried over unchanged from the existing catalog.json.

Produces structured `legionStats` (like Armada shipStats) so the play mode and
builder filters can use real card stats instead of free-text rulesText.
"""
from __future__ import annotations
import json, re, hashlib
from collections import Counter
from pathlib import Path

BUNDLE = Path("/tmp/legionhq_v2_full.json")
CATALOG = Path("/opt/projects/swlegion-army-builder/app/src/main/assets/catalog.json")
OUT = Path("/opt/projects/swlegion-army-builder/app/src/main/assets/catalog.json")

# faction maps (bundle -> app catalog factionId)
FACTION = {
    "rebels": "rebel", "rebel": "rebel", "rebel alliance": "rebel",
    "empire": "empire", "galactic empire": "empire",
    "republic": "republic", "galactic republic": "republic",
    "separatists": "separatists", "separatist": "separatists", "separatist alliance": "separatists",
    "mercenary": "mercenary", "mandalorians": "mandalorians",
    "shadow collective": "shadow_collective", "neutral": "neutral",
}
# rank map
RANK = {"commander": "COMMANDER", "operative": "OPERATIVE", "corps": "CORPS",
        "special": "SPECIAL_FORCES", "support": "SUPPORT", "heavy": "HEAVY"}
# slot map (bundle upgradeBar/subtype -> Kotlin ArmadaSlot enum)
SLOT = {
    "force": "FORCE", "command": "COMMAND", "training": "TRAINING",
    "gear": "GEAR", "armament": "ARMAMENT", "personnel": "PERSONNEL",
    "comms": "COMMS", "heavy weapon": "HEAVY_WEAPON", "heavweapon": "HEAVY_WEAPON",
    "heavyp weapon": "HEAVY_WEAPON", "hardpoint": "HARDPOINT",
    "grenades": "GRENADES", "generator": "GENERATOR", "doctrine": "DOCTRINE",
    "counterpart": "COUNTERPART", "programming": "PROGRAMMING", "ordnance": "ORDNANCE",
    "pilot": "OTHER", "crew": "OTHER", "clan": "OTHER", "squad leader": "OTHER",
}

def slug(s): return re.sub(r"-+", "-", re.sub(r"[^a-z0-9]+", "-", (s or "").lower())).strip("-") or "x"

def faction_id(f):
    f = (f or "").strip().lower()
    return FACTION.get(f, "neutral")

def make_legion_stats(c):
    """Structured JSON (mirror ArmadaStatsParser pattern)."""
    stats = c.get("stats") or {}
    weapons = []
    for w in c.get("weapons", []):
        rng = w.get("range") or []
        dice = w.get("dice") or {}
        weapons.append({
            "name": w.get("name"),
            "range": {"min": rng[0] if rng else 0, "max": rng[-1] if rng else 0},
            "dice": {"red": dice.get("r", 0), "black": dice.get("b", 0), "white": dice.get("w", 0)},
        })
    return {
        "health": stats.get("hp"),
        "courage": stats.get("courage"),
        "speed": stats.get("speed"),
        "defenseDie": stats.get("defense"),  # 'r' | 'w'
        "surgeAttack": stats.get("hitsurge"),
        "surgeDefense": stats.get("defsurge"),
        "miniCount": stats.get("minicount"),
        "keywords": [k.get("name") if isinstance(k, dict) else k for k in c.get("keywords", [])],
        "weapons": weapons,
        "upgradeBar": c.get("upgradeBar", []),
    }

def unique_id(c, seen):
    base = f"legion_v2:lhq2/{slug(c.get('cardName'))}"
    if base not in seen:
        seen.add(base); return base
    digest = hashlib.sha256((c.get("cardName","") + (c.get("title") or "")).encode()).hexdigest()[:8]
    cand = f"{base}:{digest}"; k=2
    while cand in seen: cand=f"{base}:{digest}:{k}"; k+=1
    seen.add(cand); return cand

def build():
    cards = json.loads(BUNDLE.read_text())
    units = [c for c in cards if c.get("cardType") == "unit"]
    upgrades = [c for c in cards if c.get("cardType") == "upgrade"]
    counterparts = [c for c in cards if c.get("cardType") == "counterpart"]
    seen = set()
    new_legion = []
    counts = Counter()

    for c in units:
        new_legion.append({
            "id": unique_id(c, seen), "gameSystem": "LEGION_V2", "kind": "LEGION_UNIT",
            "name": c.get("cardName", ""), "points": int(c.get("cost") or 0),
            "factionId": faction_id(c.get("faction")), "legionRank": RANK.get(c.get("rank")),
            "upgradeSlots": [], "allowedUpgradeSlots": sorted({SLOT.get(s, "OTHER") for s in c.get("upgradeBar", [])}),
            "commander": c.get("rank") == "commander", "unique": bool(c.get("isUnique")),
            "imageUrl": None, "imageAssetPath": f"cards/swl/{slug(faction_id(c.get('faction')))}/{slug(c.get('cardName'))}.webp",
            "rulesText": None, "legionStats": json.dumps(make_legion_stats(c), ensure_ascii=False, separators=(",", ":")),
        })
        counts["units"] += 1
    for c in upgrades:
        slot = SLOT.get((c.get("cardSubtype") or "").strip().lower(), "OTHER")
        new_legion.append({
            "id": unique_id(c, seen), "gameSystem": "LEGION_V2", "kind": "LEGION_UPGRADE",
            "name": c.get("cardName", ""), "points": int(c.get("cost") or 0),
            "factionId": faction_id(c.get("faction") or "neutral"), "legionRank": None,
            "upgradeSlots": [slot], "allowedUpgradeSlots": [],
            "commander": False, "unique": False,
            "imageUrl": None, "imageAssetPath": f"cards/swl/{slug(faction_id(c.get('faction') or 'neutral'))}/{slug(c.get('cardName'))}.webp",
            "rulesText": json.dumps({"requirements": c.get("requirements"), "tts": c.get("ttsName")}, ensure_ascii=False) if c.get("requirements") or c.get("ttsName") else None,
            "legionStats": None,
        })
        counts["upgrades"] += 1
    # counterparts: include as units with rank OPERATIVE? keep simple: skip ambiguity
    counts["counterparts_skipped"] = len(counterparts)

    # carry Armada unchanged
    old = json.loads(CATALOG.read_text())
    old_cards = old["cards"] if isinstance(old, dict) else old
    armada = [c for c in old_cards if c.get("gameSystem") == "ARMADA_V15"]
    final = new_legion + armada
    doc = {"schemaVersion": 1, "cards": final}
    OUT.write_text(json.dumps(doc, ensure_ascii=False, indent=2) + "\n")
    print(json.dumps({"legion_units": counts["units"], "legion_upgrades": counts["upgrades"],
                      "counterparts_skipped": counts["counterparts_skipped"],
                      "armada_kept": len(armada), "total": len(final)}, indent=2))

if __name__ == "__main__":
    build()
