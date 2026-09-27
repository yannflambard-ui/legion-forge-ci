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
    """Extract hull/shield/maxSpeed/defense-tokens/attack/speedChart from a BSData ship rulesText (JSON str)."""
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
    # Des d'attaque par arc : {front/right/left/rear: [bleu, rouge, noir]}
    attack = d.get("attack") or {}
    # Matrice de manoeuvres : liste de lignes {position, values:{vitesse: nb}} (structure triangulaire officielle).
    speed_chart = []
    for row in d.get("speed-chart-rows", []) or []:
        vals = {}
        for k, v in (row.get("values") or {}).items():
            if k.isdigit():
                vals[k] = int(v) if str(v).isdigit() else 0
        speed_chart.append({"position": row.get("position", 0), "values": vals})
    return {
        "hull": hull,
        "shield": {
            "front": shield.get("front", 0),
            "right": shield.get("right", 0),
            "left": shield.get("left", 0),
            "rear": shield.get("rear", 0),
        },
        "maxSpeed": max_speed or 1,
        "size": d.get("size", "small"),
        "command": d.get("command", 1),
        "squadron": d.get("squadron", 0),
        "engineering": d.get("engineering", 0),
        "defenseTokens": tokens,
        "attack": {
            "front": attack.get("front", [0, 0, 0]),
            "right": attack.get("right", [0, 0, 0]),
            "left": attack.get("left", [0, 0, 0]),
            "rear": attack.get("rear", [0, 0, 0]),
        },
        "speedChart": speed_chart,
    }

# BSData upgrade-bar label -> ArmadaSlot enum name (unknown -> OTHER).
UPGRADE_BAR_SLOT = {
    "Defensive Retrofit": "DEFENSIVE_RETROFIT",
    "Experimental Retrofit": "EXPERIMENTAL_RETROFIT",
    "Fleet Command": "FLEET_COMMAND",
    "Fleet Support": "FLEET_SUPPORT",
    "Ion Cannon": "ION_CANNONS",
    "Ion Cannons": "ION_CANNONS",
    "Offensive Retrofit": "OFFENSIVE_RETROFIT",
    "Officer": "OFFICER",
    "Ordnance": "ORDNANCE",
    "Superweapon": "SUPERWEAPON",
    "Support Team": "SUPPORT_TEAM",
    "Turbolasers": "TURBOLASERS",
    "Weapons Team": "WEAPONS_TEAM",
    "Engineering Team": "ENGINEERING_TEAM",
}

def ship_upgrade_slots(rt):
    """Map a BSData ship's upgrade-bar to ArmadaSlot enum names (TITLE + COMMANDER always included).

    IMPORTANT : on GARDE les doublons (ex. Executor I = 4 slots OFFICER) pour que le builder
    sache combien d'upgrades d'un même type un vaisseau accepte. Ne PAS dédupliquer avec set()."""
    try:
        d = json.loads(rt)
    except (TypeError, ValueError):
        return ["TITLE", "COMMANDER"]
    bar = d.get("upgrade-bar") or []
    slots = [UPGRADE_BAR_SLOT.get(lb, "OTHER") for lb in bar]
    if "TITLE" not in slots:
        slots.append("TITLE")
    if "COMMANDER" not in slots:
        slots.append("COMMANDER")
    return slots

def parse_def_tokens(s):
    """Parse BSData 'Defense Tokens' string -> list of token names (with dupes).
    Formats: '2 Brace', 'Brace, Scatter', '1 Brace, 1 Scatter', 'Brace, Brace'."""
    if not s:
        return []
    out = []
    for part in str(s).split(","):
        part = part.strip()
        m = re.match(r"^(\d+)\s+(.+)$", part)
        if m:
            count, name = int(m.group(1)), m.group(2)
        else:
            count, name = 1, part
        out.extend([name.upper()] * count)
    return out

ARMADA_KEYWORDS = ["AI", "Adept", "Assault", "Bomber", "Cloak", "Counter", "Dodge", "Escort", "Grit", "Heavy", "Intel", "Relay", "Rogue", "Screen", "Snipe", "Strategic", "Swarm"]

def parse_keywords(s):
    """Extract individual Armada keyword names from a BSData 'Keywords' string.
    Handles all formats: 'Bomber, Rogue', 'Counter 1, Rogue', 'AI: Anti-Squadron 1. Counter 2. Snipe 3. Swarm', 'Bomber\\nGrit\\nRogue'."""
    if not s:
        return []
    out = []
    for kw in ARMADA_KEYWORDS:
        if re.search(r"\b" + re.escape(kw) + r"\b", str(s), re.IGNORECASE):
            out.append(kw)
    return out

def parse_dice(s):
    """Parse BSData dice string -> [blue, red, black]. Formats: '4 Blue', '1 Blue, 1 Black'."""
    out = [0, 0, 0]
    if not s:
        return out
    for part in str(s).split(","):
        part = part.strip()
        m = re.match(r"^(\d+)\s+(\w+)$", part)
        if m:
            n, color = int(m.group(1)), m.group(2).lower()
            if "blue" in color:
                out[0] += n
            elif "red" in color:
                out[1] += n
            elif "black" in color:
                out[2] += n
    return out

def squadron_stats(rt):
    """Squadron BSData JSON has characteristics: Hull Value, Speed, Anti-Squadron, Battery, Defense Tokens, Keywords."""
    try:
        d = json.loads(rt)
    except (TypeError, ValueError):
        return None
    ch = (d.get("characteristics") or {}) if isinstance(d, dict) else {}
    hv = ch.get("Hull Value")
    sp = ch.get("Speed")
    if hv is None:
        return None
    return {
        "hull": int(hv),
        "speed": int(sp) if sp and str(sp).isdigit() else 3,
        "antiSquadron": parse_dice(ch.get("Anti-Squadron")),
        "battery": parse_dice(ch.get("Battery")),
        "defenseTokens": parse_def_tokens(ch.get("Defense Tokens")),
        "keywords": parse_keywords(ch.get("Keywords")),
    }

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
            # Slots d'upgrade du vaisseau (upgrade-bar BSData + TITLE) : corrige le bug
            # où les vaisseaux n'avaient que ['TITLE'] et n'affichaient aucun upgrade régulier.
            card["allowedUpgradeSlots"] = ship_upgrade_slots(pick.get("rulesText"))
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
