#!/usr/bin/env python3
"""Link Armada schema to the ship family each TITLE upgrade applies to.

BSData (armada_bsdata_catalog.json) encodes the target in each upgrade's
rulesText as `slotCategory` = "Title - <Ship family>". This enriches catalog.json:
  * `linkedUnit` on each ARMADA_UPGRADE TITLE card = canonical ship-family key.
  * adds a TITLE slot to every ARMADA_SHIP's allowedUpgradeSlots (each ship has
    a title slot in real Armada rules).

The family key is computed by `ship_family(name)` which both this script and
the Kotlin builder must mirror EXACTLY (same normalization + overrides), so the
runtime filter can decide which titles fit which ships.

Standalone:  python3 scripts/enrich_armada_links.py
"""
import json, os, re

ROOT = os.path.join(os.path.dirname(__file__), "..", "app", "src", "main", "assets")
CATALOG = os.path.join(ROOT, "catalog.json")
BSDATA = os.path.join(ROOT, "armada_bsdata_catalog.json")

# Tokens stripped when computing a ship's canonical family: variants / hull
# types that distinguish individual variants of the same underlying hull.
_VARIANT_WORDS = re.compile(
    r"\b(i{1,3}|iv|v|a|b|c|mk|mark|class|type)\b"
    r"|\b(assault|battle|command|star|combat|medium|light|scout|torpedo|"
    r"armored|ordnance|suppression|carrier|dreadnought|transport|refit|"
    r"retrofit|escort|support|flotilla|patrol|special|troopship|missile|"
    r"artillery|juggernaut|interdiction|city)\b"
)

# Canonical family => canonical family "base" used to match title target to ships.
# Each title 'Title - X' and each ship name reduce to a base via this map;
# a title applies to a ship if their bases are equal.
_BASE = {
    "super star destroyer": "executor",
    "super destroyer": "executor",
    "star dreadnought": "executor",
    "executor": "executor",
    "imperial star destroyer": "imperial star destroyer",
    "gladiator": "gladiator",
    "argo-type transport": "argotype",
    "gonzanti": "gozanti",
    "gozanti": "gozanti",
    "gr 75": "gr75",
    "cr90": "cr90",
    "nebula": "nebula",
    "nebula-b": "nebulon b",
    "mc30": "mc30",
    "mc75": "mc75",
    "mc80": "mc80",
    "mc80 cruiser home one type": "mc80 home one",
    "mc80 cruiser home one": "mc80 home one",
    "mc80 home one": "mc80 home one",
    "mc80 cruiser liberty type": "mc80 liberty",
    "mc80 cruiser liberty": "mc80 liberty",
    "mc80 liberty": "mc80 liberty",
    "hammerhead": "hammerhead",
    "raider": "raider",
    "arquitens": "arquitens",
    "pelta": "pelta",
    "modified pelta": "pelta",
    "hardcell": "hardcell",
    "acclamator": "acclamator",
    "venator": "venator",
    "victory": "victory",
    "quasar": "quasar",
    "onager": "onager",
    "interdictor": "interdictor",
    "muunificent": "munificent",
    "munificent": "munificent",
    "recusant": "recusant",
    "providence": "providence",
    "starhawk": "starhawk",
    "home one": "mc80 home one",
    "liberty": "mc80 liberty",
}


def canonical_family(name: str) -> str:
    """Canonical ship-family base. Designed to be mirrored in Kotlin."""
    low = re.sub(r"[^a-z0-9 ]", " ", name.lower()).replace("dreadought", "dreadnought")
    low = re.sub(_VARIANT_WORDS, " ", low)
    low = re.sub(r"\s+", " ", low).strip()
    # resolve to a stable base when known; fall back to the blanked string
    for key, base in _BASE.items():
        if key in low:
            return base
    return low or "unknown"


def normalize(s: str) -> str:
    return re.sub(r"[^a-z0-9 ]", " ", s.lower()).replace("dreadought", "dreadnought").strip()


def family_matches_ship(fam: str, ship_name: str) -> bool:
    """Does a title family apply to a ship? True if their canonical bases match."""
    return canonical_family(fam) == canonical_family(ship_name)


# Titles absent from BSData (or mismatched) -> canonical family.
MANUAL_FAMILY = {
    "Corrupter": "super star destroyer",
    "Seventh Fleet Star Destroyer": "super star destroyer",
    "Mon Calamari Exodus Fleet": "mc80",
}

# Named/individual flagship hulls that are UNIQUE in Armada: you cannot field
# two of them (even two hull variants) in the same fleet. Keyed by canonical
# family. Executor / super-star-destroyer is the flagship example.
UNIQUE_SHIP_FAMILIES = {"executor"}


def enrich(assets_dir=None):
    """Add linkedUnit to TITLE upgrades + TITLE slot to ships. Returns (linked, ships_touched, unmatched)."""
    cat_path = os.path.abspath(os.path.join(assets_dir or ROOT, "catalog.json"))
    bs_path = os.path.abspath(os.path.join(assets_dir or ROOT, "armada_bsdata_catalog.json"))
    catalog = json.load(open(cat_path, encoding="utf-8"))
    bs = json.load(open(bs_path, encoding="utf-8"))
    if isinstance(bs, dict):
        bs = bs.get("cards", [])

    ships = {c["id"]: c for c in catalog["cards"] if c.get("kind") == "ARMADA_SHIP"}
    ship_fams = {sid: canonical_family(c["name"]) for sid, c in ships.items()}

    # Index BSData TITLE upgrades: norm(name) -> list of (canonical family, faction)
    bs_titles = {}
    for c in bs:
        if c.get("kind") != "ARMADA_UPGRADE":
            continue
        try:
            sc = json.loads(c.get("rulesText") or "").get("slotCategory", "")
        except (TypeError, ValueError):
            continue
        if sc.startswith("Title - "):
            fam = canonical_family(sc[len("Title - "):])
            bs_titles.setdefault(normalize(c["name"]), []).append((fam, c.get("factionId")))

    linked = ships_touched = 0
    unmatched = []
    for card in catalog["cards"]:
        if card.get("kind") != "ARMADA_UPGRADE" or "TITLE" not in (card.get("upgradeSlots") or []):
            continue
        fam = None
        cands = bs_titles.get(normalize(card["name"]))
        if cands:
            pick = next((c for c in cands if c[1] == card.get("factionId")), cands[0])
            fam = pick[0]
        elif card["name"] in MANUAL_FAMILY:
            fam = MANUAL_FAMILY[card["name"]]
        if not fam:
            unmatched.append(card["name"])
            card.pop("linkedUnit", None)
            continue
        card["linkedUnit"] = fam
        matched = [sid for sid, sf in ship_fams.items() if family_matches_ship(fam, ships[sid]["name"])]
        # keep the user informed if a title's family matches no ship in the bundle
        if not matched:
            unmatched.append(f"{card['name']} (famille '{fam}' sans vaisseau correspondant)")
        linked += 1

    # Add a TITLE slot to every ship (each ship has a title slot in Armada rules),
    # and mark named flagship hull families as unique (can't field two in a fleet).
    for c in catalog["cards"]:
        if c.get("kind") == "ARMADA_SHIP":
            if "TITLE" not in c.get("allowedUpgradeSlots", []):
                c["allowedUpgradeSlots"] = c.get("allowedUpgradeSlots", []) + ["TITLE"]
                ships_touched += 1
            if canonical_family(c["name"]) in UNIQUE_SHIP_FAMILIES:
                c["unique"] = True

    json.dump(catalog, open(cat_path, "w", encoding="utf-8"), ensure_ascii=False, indent=1)
    return linked, ships_touched, unmatched


def main():
    linked, ships_touched, unmatched = enrich()
    print("titles linked:", linked)
    print("ships with TITLE slot added:", ships_touched)
    print("unmatched / no-ship:", len(unmatched))
    for u in unmatched:
        print("   ", u)


if __name__ == "__main__":
    main()
