#!/usr/bin/env python3
"""Inject imageAssetPaths (map par langue) dans catalog.json pour les cartes
Legion qui ont des images localisées déjà présentes dans assets/cards/swl/<faction>/<locale>/.

Le bundle/les données n'ont qu'une image par carte (imageAssetPath, EN/FR selon source).
Depuis la session FR (dpi=300) et DE (Asmodee), on a des images localisées committées
dans des sous-dossiers <locale>/ à côté de l'image de base (ex neutral/de/anger.webp).
Ce script scanne le disque pour chaque carte et remplit imageAssetPaths['de'], ['fr']
quand le fichier existe. Non destructif : ne touche pas aux cartes sans image localisée.
Usage: python3 inject_localized_image_paths.py
"""
from __future__ import annotations
import json, os, re
from pathlib import Path

ASSET = Path("/root/legion-forge-ci/app/src/main/assets")
CATALOG = ASSET / "catalog.json"
LOCALES = ["de", "fr", "es"]

def load_catalog():
    return json.loads(CATALOG.read_text())

def scan(card: dict):
    """Retourne un dict {locale: chemin_asset_localise} pour la carte, en
    vérifiant que le fichier existe réellement sur disque."""
    base = card.get("imageAssetPath")
    if not base:
        return {}
    # Base est genre "cards/swl/neutral/anger.webp". Le fichier localisé est
    # "cards/swl/neutral/de/anger.webp" (même nom, inséré "de/" avant le nom).
    rel = Path(base)
    name = rel.name
    parent = rel.parent  # cards/swl/neutral
    paths = {}
    for loc in LOCALES:
        cand = parent / loc / name
        if (ASSET / cand).exists():
            paths[loc] = str(cand)
    return paths

def main():
    doc = load_catalog()
    cards = doc["cards"] if isinstance(doc, dict) else doc
    injected = 0
    for card in cards:
        paths = scan(card)
        if paths:
            card["imageAssetPaths"] = {**card.get("imageAssetPaths", {}), **paths}
            injected += 1
    out = {"schemaVersion": 1, "cards": cards}
    CATALOG.write_text(json.dumps(out, ensure_ascii=False, indent=2) + "\n")
    de = sum(1 for c in cards if (c.get("imageAssetPaths") or {}).get("de"))
    fr = sum(1 for c in cards if (c.get("imageAssetPaths") or {}).get("fr"))
    es = sum(1 for c in cards if (c.get("imageAssetPaths") or {}).get("es"))
    print(f"Cartes avec imageAssetPaths injectés: {injected}")
    print(f"  dont locales: de={de}, fr={fr}, es={es}")

if __name__ == "__main__":
    main()
