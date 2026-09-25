#!/usr/bin/env python3
"""Build the offline Legion v2 + Armada v1.5 catalog from local donor checkouts.

No image URLs or asset paths are carried over. Unsupported slot labels are
represented as OTHER (the current Kotlin enum has no Legion slot variants).
"""
from __future__ import annotations

import argparse
import hashlib
import json
import re
import subprocess
from collections import Counter
from pathlib import Path
from typing import Any

SCHEMA_VERSION = 1
CARD_KEYS = {
    "id", "gameSystem", "kind", "name", "points", "factionId", "legionRank",
    "upgradeSlots", "allowedUpgradeSlots", "commander", "unique", "imageUrl",
    "imageAssetPath", "rulesText",
}
ARMADA_SLOTS = {
    "commander": "COMMANDER",
    "offensive retrofit": "OFFENSIVE_RETROFIT",
    "turbolasers": "TURBOLASERS",
    "weapons team": "WEAPONS_TEAM",
    "support team": "SUPPORT_TEAM",
    "defensive retrofit": "DEFENSIVE_RETROFIT",
    "engineering team": "ENGINEERING_TEAM",
    "title": "TITLE",
    "officer": "OFFICER",
    "fleet command": "FLEET_COMMAND",
    "fleet support": "FLEET_SUPPORT",
    "ion cannons": "ION_CANNONS",
    "ordnance": "ORDNANCE",
    "experimental retrofit": "EXPERIMENTAL_RETROFIT",
    "superweapon": "SUPERWEAPON",
}
LEGION_RANKS = {
    "commander": "COMMANDER",
    "operative": "OPERATIVE",
    "corps": "CORPS",
    "special forces": "SPECIAL_FORCES",
    "support": "SUPPORT",
    "heavy": "HEAVY",
}
LEGION_SLOT_ENUMS = {
    "FORCE", "COMMAND", "TRAINING", "GEAR", "ARMAMENT", "PERSONNEL", "COMMS",
    "HEAVY_WEAPON", "HARDPOINT", "GRENADES", "GENERATOR", "DOCTRINE",
    "COUNTERPART", "PROGRAMMING", "ORDNANCE",
}
FACTION_ALIASES = {
    "rebel": "rebel-alliance",
    "rebels": "rebel-alliance",
    "rebel alliance": "rebel-alliance",
    "empire": "galactic-empire",
    "galactic empire": "galactic-empire",
    "republic": "galactic-republic",
    "galactic republic": "galactic-republic",
    "separatists": "separatist-alliance",
    "separatist": "separatist-alliance",
    "separatist alliance": "separatist-alliance",
    "mercenary": "mercenary",
    "shadow collective": "shadow-collective",
}
IMAGE_KEYS = {"image", "ship-image", "squadron-image", "image-url", "image-asset-path"}


def slug(value: Any) -> str:
    text = str(value or "").strip().lower()
    return re.sub(r"-+", "-", re.sub(r"[^a-z0-9]+", "-", text)).strip("-") or "unknown"


def clean_points(value: Any) -> int | None:
    if isinstance(value, bool) or value is None:
        return None
    if isinstance(value, int):
        result = value
    elif isinstance(value, float) and value.is_integer():
        result = int(value)
    elif isinstance(value, str) and re.fullmatch(r"\s*\+?\d+\s*", value):
        result = int(value.strip())
    else:
        return None
    return result if result >= 0 else None


def enum_slot(value: Any, legion: bool = False) -> str:
    label = str(value or "").strip()
    if legion:
        # Convert only names known to exist in the Kotlin enum; retain original
        # donor labels in rulesText when no safe enum equivalent exists.
        enum_name = re.sub(r"[^A-Za-z0-9]+", "_", label).strip("_").upper()
        return enum_name if enum_name in LEGION_SLOT_ENUMS else "OTHER"
    return ARMADA_SLOTS.get(label.casefold(), "OTHER")


def slot_values(value: Any, legion: bool = False) -> list[str]:
    if value is None:
        return []
    if isinstance(value, dict):
        labels = list(value.keys())
    elif isinstance(value, (list, tuple)):
        labels = list(value)
    else:
        labels = [value]
    # The Kotlin model stores slot types, not their multiplicities.
    return sorted({enum_slot(label, legion) for label in labels if str(label).strip()})


def faction_id(faction: Any, fallback: str | None = None) -> str:
    raw = faction if isinstance(faction, str) and faction.strip() else fallback
    if not raw:
        return "neutral"
    normalized = raw.strip().casefold()
    return FACTION_ALIASES.get(normalized, slug(normalized))


def clean_json_value(value: Any) -> Any:
    """Remove donor artwork references recursively before rule-text rendering."""
    if isinstance(value, dict):
        return {
            key: clean_json_value(item)
            for key, item in value.items()
            if key.casefold() not in IMAGE_KEYS and not key.casefold().endswith("-image")
            and key != "__comment"
        }
    if isinstance(value, list):
        return [clean_json_value(item) for item in value]
    return value


def render_rules(data: dict[str, Any], game: str, category: str) -> str | None:
    """Keep useful source-row detail in the only free-text field in the schema."""
    if game == "LEGION_V2":
        lines: list[str] = []
        subtitle = data.get("title") or data.get("subtitle")
        if subtitle:
            lines.append(f"Title: {subtitle}")
        unit_type = data.get("unit_type") or data.get("type")
        if unit_type:
            lines.append(f"Type: {unit_type}")
        stats = data.get("stats") if isinstance(data.get("stats"), dict) else data
        stat_labels = (("health", "Health"), ("courage", "Courage"), ("speed", "Speed"))
        for key, label in stat_labels:
            value = stats.get(key)
            if value is not None:
                lines.append(f"{label}: {value}")
        slots = data.get("upgrade_slots")
        if slots:
            if isinstance(slots, dict):
                slot_text = ", ".join(f"{key} ×{value}" for key, value in slots.items())
            elif isinstance(slots, list):
                slot_text = ", ".join(str(value) for value in slots)
            else:
                slot_text = str(slots)
            lines.append(f"Upgrade slots: {slot_text}")
        keywords = data.get("keywords")
        if isinstance(keywords, list) and keywords:
            rendered = []
            for item in keywords:
                if isinstance(item, dict):
                    label = str(item.get("ref", "")).split("/")[-1].replace("_", " ").title()
                    if item.get("value") is not None:
                        label += f" {item['value']}"
                    if label.strip():
                        rendered.append(label)
                elif item:
                    rendered.append(str(item))
            if rendered:
                lines.append("Keywords: " + "; ".join(rendered))
        weapons = data.get("weapons")
        if isinstance(weapons, list) and weapons:
            rendered_weapons = []
            for weapon in weapons:
                if isinstance(weapon, dict):
                    bits = [str(weapon.get("name") or "Weapon")]
                    if weapon.get("range") is not None:
                        bits.append(f"range {weapon['range']}")
                    dice = weapon.get("dice")
                    if isinstance(dice, dict) and dice:
                        bits.append("dice " + "/".join(f"{k} {v}" for k, v in dice.items()))
                    weapon_keywords = weapon.get("keywords")
                    if weapon_keywords:
                        bits.append("; ".join(map(str, weapon_keywords)))
                    rendered_weapons.append(bits[0] + (" (" + ", ".join(bits[1:]) + ")" if len(bits) > 1 else ""))
                else:
                    rendered_weapons.append(str(weapon))
            lines.append("Weapons: " + "; ".join(rendered_weapons))
        text = data.get("text")
        if isinstance(text, str) and text.strip():
            lines.append(text.strip())
        upgrade_slot = data.get("upgrade_slot")
        if upgrade_slot:
            lines.append(f"Upgrade slot: {upgrade_slot}")
        restrictions = data.get("restrictions")
        if restrictions and str(restrictions).casefold() != "none":
            lines.append(f"Restrictions: {restrictions}")
        gained = data.get("keywords_gained")
        if gained:
            lines.append("Keywords gained: " + ", ".join(map(str, gained)))
        return "\n".join(lines) or None

    # Armada ship/squadron records have structured characteristics rather than
    # prose. Compact JSON preserves these useful stats without carrying artwork.
    useful = clean_json_value(data)
    for key in ("name", "faction", "points", "slots", "unique"):
        useful.pop(key, None)
    if category == "upgrade-card":
        text = str(data.get("text", "")).strip()
        extras = []
        for key in ("ship", "trait", "restriction", "errata"):
            if data.get(key) is not None:
                extras.append(f"{key}: {json.dumps(data[key], ensure_ascii=False, separators=(',', ':'))}")
        if extras:
            text = "\n".join(filter(None, [text, *extras]))
        return text or None
    if category == "squadron-card" and isinstance(data.get("text"), str):
        useful.pop("text", None)
        text = data["text"].strip()
        details = json.dumps(useful, ensure_ascii=False, separators=(",", ":"))
        return "\n".join(filter(None, [text, details])) or None
    return json.dumps(useful, ensure_ascii=False, separators=(",", ":")) or None


def unique_id(namespace: str, source_path: str, name: str, row: dict[str, Any], seen: set[str]) -> str:
    base = f"{namespace}:{slug(source_path)}:{slug(name)}"
    if base not in seen:
        seen.add(base)
        return base
    digest = hashlib.sha256(json.dumps(row, sort_keys=True, ensure_ascii=False).encode("utf-8")).hexdigest()[:10]
    candidate = f"{base}:{digest}"
    serial = 2
    while candidate in seen:
        candidate = f"{base}:{digest}:{serial}"
        serial += 1
    seen.add(candidate)
    return candidate


def git_revision(repo: Path) -> str | None:
    try:
        return subprocess.run(
            ["git", "-C", str(repo), "rev-parse", "HEAD"], check=True,
            capture_output=True, text=True, timeout=5,
        ).stdout.strip()
    except (OSError, subprocess.SubprocessError):
        return None


def valid_card(card: dict[str, Any]) -> None:
    if set(card) != CARD_KEYS:
        raise ValueError(f"Catalog card keys differ from model: {set(card) ^ CARD_KEYS}")
    if not isinstance(card["points"], int) or card["points"] < 0:
        raise ValueError(f"Invalid points for {card['id']}")
    if card["imageUrl"] is not None or card["imageAssetPath"] is not None:
        raise ValueError(f"Image field unexpectedly populated: {card['id']}")
    if not isinstance(card["upgradeSlots"], list) or not isinstance(card["allowedUpgradeSlots"], list):
        raise ValueError(f"Slot fields must be lists: {card['id']}")


def build(legion_root: Path, armada_root: Path, output: Path, sources_output: Path) -> dict[str, Any]:
    cards: list[dict[str, Any]] = []
    seen_ids: set[str] = set()
    counts: Counter[str] = Counter()
    skipped: Counter[str] = Counter()

    units_path = legion_root / "compiled" / "all_units.json"
    unit_rows = json.loads(units_path.read_text(encoding="utf-8"))
    if isinstance(unit_rows, dict):
        unit_rows = list(unit_rows.values())
    for row in unit_rows:
        counts["legion_units_seen"] += 1
        points = clean_points(row.get("points"))
        if points is None:
            skipped["legion_units_invalid_points"] += 1
            continue
        row_id = str(row.get("id") or row.get("source") or row.get("name") or "unit")
        path_parts = row_id.replace("\\", "/").split("/")
        fallback_faction = path_parts[0] if path_parts else None
        rank = LEGION_RANKS.get(str(row.get("rank", "")).strip().casefold())
        source_path = str(row.get("source") or row_id)
        card = {
            "id": unique_id("legion-v2-unit", source_path, row.get("name", ""), row, seen_ids),
            "gameSystem": "LEGION_V2",
            "kind": "LEGION_UNIT",
            "name": str(row.get("name") or "Unknown unit").strip(),
            "points": points,
            "factionId": faction_id(row.get("faction"), fallback_faction),
            "legionRank": rank,
            "upgradeSlots": [],
            "allowedUpgradeSlots": slot_values(row.get("upgrade_slots"), legion=True),
            "commander": rank == "COMMANDER",
            "unique": bool(row.get("unique", False)),
            "imageUrl": None,
            "imageAssetPath": None,
            "rulesText": render_rules(row, "LEGION_V2", "unit"),
        }
        valid_card(card)
        cards.append(card)
        counts["legion_units_included"] += 1

    upgrade_dir = legion_root / "data" / "upgrades"
    for path in sorted(upgrade_dir.rglob("*.json")):
        row = json.loads(path.read_text(encoding="utf-8"))
        rows = row if isinstance(row, list) else [row]
        for row_index, item in enumerate(rows):
            counts["legion_upgrades_seen"] += 1
            points = clean_points(item.get("points"))
            if points is None:
                skipped["legion_upgrades_invalid_points"] += 1
                continue
            relative = path.relative_to(legion_root).as_posix()
            row_key = f"{relative}#{row_index}" if len(rows) > 1 else relative
            card = {
                "id": unique_id("legion-v2-upgrade", row_key, item.get("name", ""), item, seen_ids),
                "gameSystem": "LEGION_V2",
                "kind": "LEGION_UPGRADE",
                "name": str(item.get("name") or path.stem.replace("_", " ").title()).strip(),
                "points": points,
                "factionId": faction_id(None, None),
                "legionRank": None,
                "upgradeSlots": [enum_slot(item.get("upgrade_slot"), legion=True)],
                "allowedUpgradeSlots": [],
                "commander": False,
                "unique": str(item.get("limit", "")).strip().casefold() == "unique",
                "imageUrl": None,
                "imageAssetPath": None,
                "rulesText": render_rules(item, "LEGION_V2", "upgrade"),
            }
            valid_card(card)
            cards.append(card)
            counts["legion_upgrades_included"] += 1

    armada_categories = (
        ("ship-card", "ARMADA_SHIP"),
        ("squadron-card", "ARMADA_SQUADRON"),
        ("upgrade-card", "ARMADA_UPGRADE"),
    )
    for category, kind in armada_categories:
        for path in sorted((armada_root / category).rglob("*.json")):
            raw = json.loads(path.read_text(encoding="utf-8"))
            rows = raw if isinstance(raw, list) else [raw]
            relative = path.relative_to(armada_root).as_posix()
            for row_index, row in enumerate(rows):
                counter_name = category.replace("-card", "s")
                counts[f"armada_{counter_name}_seen"] += 1
                points = clean_points(row.get("points"))
                if points is None:
                    skipped[f"armada_{counter_name}_invalid_points"] += 1
                    continue
                row_key = f"{relative}#{row_index}" if len(rows) > 1 else relative
                slots = slot_values(row.get("slots"))
                card = {
                    "id": unique_id("armada-v15-" + slug(category), row_key, row.get("name", ""), row, seen_ids),
                    "gameSystem": "ARMADA_V15",
                    "kind": kind,
                    "name": str(row.get("name") or "Unknown card").strip(),
                    "points": points,
                    "factionId": faction_id(row.get("faction")),
                    "legionRank": None,
                    "upgradeSlots": slots if kind == "ARMADA_UPGRADE" else [],
                    "allowedUpgradeSlots": slots if kind == "ARMADA_SHIP" else [],
                    "commander": kind == "ARMADA_UPGRADE" and "COMMANDER" in slots,
                    "unique": bool(row.get("unique", False)),
                    "imageUrl": None,
                    "imageAssetPath": None,
                    "rulesText": render_rules(row, "ARMADA_V15", category),
                }
                valid_card(card)
                cards.append(card)
                counts[f"armada_{counter_name}_included"] += 1

    if len({card["id"] for card in cards}) != len(cards):
        raise ValueError("Generated duplicate card IDs")
    cards.sort(key=lambda card: card["id"])
    document = {"schemaVersion": SCHEMA_VERSION, "cards": cards}
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text(json.dumps(document, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")

    sources = {
        "description": "Source attribution and data-rights/freshness notes for the generated offline catalog.",
        "catalogSchemaVersion": SCHEMA_VERSION,
        "catalogPath": str(output),
        "catalogCardCount": len(cards),
        "counts": dict(sorted(counts.items())),
        "excluded": dict(sorted(skipped.items())),
        "artworkPolicy": "No donor artwork is copied or linked. imageUrl and imageAssetPath are null for every card.",
        "slotMapping": {
            "armada": "Known donor labels map to the existing ArmadaSlot enum names; unrecognized values map to OTHER.",
            "legion": "Recognized Legion slot labels map to same-named values in the existing ArmadaSlot enum; labels without an available equivalent map to OTHER. Exact source slot names are retained in rulesText. No Kotlin enum was modified.",
        },
        "sources": [
            {
                "name": "Star Wars: Legion As Text",
                "repository": "https://github.com/DoctorDizzee/Star-Wars-Legion-As-Text",
                "snapshotRevision": git_revision(legion_root),
                "inputs": ["compiled/all_units.json", "data/upgrades/**/*.json"],
                "attribution": "DoctorDizzee / Star-Wars-Legion-As-Text; donor source rows converted into the app schema.",
                "license": "No LICENSE file was located in the supplied checkout. Permission to redistribute the donor data is therefore unverified; obtain legal review/permission before public or commercial distribution.",
                "freshness": "Snapshot is only as current as the supplied checkout. A May 2026 update / Legion 2.5 coverage was mentioned but not independently confirmed; points and rules can become stale.",
            },
            {
                "name": "star-wars-armada-data",
                "repository": "https://github.com/rkbodenner/star-wars-armada-data",
                "snapshotRevision": git_revision(armada_root.parent),
                "inputs": ["data/ship-card/**/*.json", "data/squadron-card/**/*.json", "data/upgrade-card/**/*.json"],
                "attribution": "Jeffrey M. Thompson / rkbodenner; retain the repository MIT copyright and permission notice when redistributing applicable MIT-licensed portions.",
                "license": "Repository includes an MIT License, but its README explicitly states card text and all images are copyright/trademark Lucasfilm Ltd. Artwork/image references are omitted; MIT licensing of repository code does not establish permission to redistribute card text or other third-party content.",
                "freshness": "The supplied data is an older snapshot (described as 2020); it may omit later changes and points/rules/errata. Verify against current authoritative material before use.",
            },
        ],
        "distributionCaveat": "This file is source attribution, not a legal opinion or a claim that game names/rules text are cleared for distribution. Review upstream licenses and applicable third-party rights before release.",
    }
    sources_output.parent.mkdir(parents=True, exist_ok=True)
    sources_output.write_text(json.dumps(sources, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    return sources


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--legion", type=Path, default=Path("/tmp/legion-dataset"))
    parser.add_argument("--armada", type=Path, default=Path("/tmp/armada-dataset/data"))
    parser.add_argument("--output", type=Path, default=Path("app/src/main/assets/catalog.json"))
    parser.add_argument("--sources-output", type=Path, default=Path("catalogSources.json"))
    parser.add_argument("--skip-stats", action="store_true",
                        help="Ne pas ré-injecter les stats Armada (BSData / fleet builder) après build")
    args = parser.parse_args()
    summary = build(args.legion, args.armada, args.output, args.sources_output)
    if not args.skip_stats:
        # Injecte hull / boucliers / vitesse depuis armada_bsdata_catalog.json
        # (dataset "Armada Fleet Builder") dans un champ shipStats des vaisseaux/escadrons.
        from enrich_armada_stats import enrich
        enrich(args.output.parent, args.output)
        # Ajoute Republic/Separatist (vaisseaux/escadrons/commandants) + images de cartes
        # (miniatures colonne gauche + carte en mode partie) depuis le cache local
        # des images Ryan Kingston. Silencieux si le cache n'est pas présent.
        try:
            from enrich_armada_images import main as enrich_images
            enrich_images()
        except Exception as e:
            print("WARN: enrich_armada_images skipped:", e)
        # Renomme les images dans la structure cards/<game>/<faction>/<nom-officiel>.webp
        try:
            from rename_card_images import main as rename_images
            rename_images()
        except Exception as e:
            print("WARN: rename_card_images skipped:", e)
    print(json.dumps({"catalogPath": str(args.output), "sourceMetadataPath": str(args.sources_output),
                      "catalogCardCount": summary["catalogCardCount"],
                      "counts": summary["counts"], "excluded": summary["excluded"]}, indent=2))


if __name__ == "__main__":
    main()
