#!/usr/bin/env python3
"""Complète les images Armada manquantes (titres de vaisseaux, escadrons, commandant).

Contexte: enrich_armada_images.py ne matchait que les SHIPS (set 'ship') et rejetait
tout titre à 1 seul token (best_score >= 2) -> 66 cartes Armada (48 upgrades-titres,
17 escadrons, 1 commandant) sont restées SANS image alors que l'image existe chez
Ryan Kingston (téléchargée dans ryk_imgs/).

On matche chaque carte manquante vers l'image RYK (tous les sets: title/squadron/
commander/officer/super-weapon), convertit en WebP sous cards/swa/<faction>/<slug>.webp
et met à jour imageAssetPath. Collisions de slug -> suffixe par points (convention
rename_card_images.py).
"""
from __future__ import annotations
import json, os, re
from collections import defaultdict, Counter
from PIL import Image

ROOT = os.path.join(os.path.dirname(__file__), "..", "app", "src", "main", "assets")
CATALOG = os.path.join(ROOT, "catalog.json")
CARDS = os.path.join(ROOT, "cards", "swa")
RYK_MANIFEST = "/root/.hermes/cache/browser-use/workspace/ryk_full.json"
RYK_IMGS = "/root/.hermes/cache/browser-use/workspace/ryk_imgs"

# Quel set RYK chercher selon le kind de la carte manquante.
RYK_SETS = {
    "ARMADA_SQUADRON": ("squadron",),
    "COMMANDER": ("commander",),
    "ARMADA_UPGRADE": ("title", "officer", "super-weapon", "weapons-team",
                       "offensive-retrofit", "defensive-retrofit", "experimental-retrofit",
                       "ordnance", "ion-cannons", "turbolasers", "support-team",
                       "fleet-command", "fleet-support"),
}

def slug(s):
    s = s.lower().replace("'", "").replace('"', "")
    return re.sub(r"[^a-z0-9]+", "-", s).strip("-") or "x"

def norm(s):
    n = re.sub(r"\s+", " ", s.lower().replace("-", " ")).strip()
    n = n.replace('"', "").replace("'", "").replace("’", "")
    return n.replace("dreadought", "dreadnought")

def tokens(s):
    t = set(norm(s).split())
    out = set()
    for w in t:
        w2 = {"i": "1", "ii": "2", "iii": "3", "iv": "4", "v": "5"}.get(w, w)
        if w.startswith("mk"):
            w2 = "mark " + w[2:]
        out.add(w2)
    return out

# Overrides manuels pour cas de vocabulaire / nomin. c[?"carte"] -> (set_ryk, image)
OVERRIDES = {
    "magnite crysal tractor beam array": ("super-weapon", "magnite-crystal-tractor-beam-array.jpg"),
    "raymus antilles": ("officer", "raymus-antilles.png"),
    "baktoid prototypes": ("squadron", "baktoid-prototype.png"),
    "darth vader": ("squadron", "darth-vader.png"),
    "hera syndulla": ("squadron", "hera-syndulla.png"),
    "arc 170 squadron": ("squadron", "arc-170.png"),
    "anakin skywalker": ("squadron", "anakin-skywalker.png"),
}

def best_image(title, sets):
    """Retourne (image_fname, None) la meilleure image RYK dans les sets donnés, sinon (None,None)."""
    t = tokens(title)
    n = norm(title)
    # 1) exact d'abord
    ryk = json.load(open(RYK_MANIFEST, encoding="utf-8"))
    exact = {}
    partial = []
    for key in sets:
        for c in ryk.get(key, []) or []:
            if not isinstance(c, dict) or not c.get("image"):
                continue
            ct = norm(c.get("title") or "")
            if ct == n:
                exact[(key, ct)] = fixture if (fixture := c["image"]) else None
    if exact:
        return next(iter(exact.values())), None
    # 2) subset token-score (accepte les titres à 1 token, contrairement à l'ancien >=2)
    best, bs = None, -1
    for key in sets:
        for c in ryk.get(key, []) or []:
            if not isinstance(c, dict) or not c.get("image"):
                continue
            r = tokens(c.get("title") or "")
            if r and r.issubset(t) and len(r) > bs:
                bs, best = len(r), c["image"]
    if best and bs >= 1:
        return best, None
    return None, None

def resolve(card):
    """Retourne le chemin source RYK de l'image ou None."""
    n = norm(card["name"])
    if n in OVERRIDES:
        key, fname = OVERRIDES[n]
        src = os.path.join(RYK_IMGS, fname)
        return src if os.path.exists(src) else None
    img, _ = best_image(card["name"], RYK_SETS[card["kind"]])
    if not img:
        return None
    src = os.path.join(RYK_IMGS, img)
    if not os.path.exists(src):
        print(f"  ! image RYK absente sur disque: {img}")
        return None
    return src

def main():
    if not os.path.isdir(RYK_IMGS):
        print("ryk_imgs absent — lancer fetch_ryk_images.py d'abord")
        return
    doc = json.load(open(CATALOG, encoding="utf-8"))
    cards = doc["cards"]
    missing = [c for c in cards if c["gameSystem"] == "ARMADA_V15" and not c.get("imageAssetPath")]

    # assigner une cible de fichier par carte (+collision de slug -> suffixe points)
    plan = []  # (card, src)
    for c in missing:
        src = resolve(c)
        if src:
            plan.append((c, src))

    # collisions de slug (même nom, p.ex. 2x Anakin Skywalker squadron)
    groups = defaultdict(list)
    for c, src in plan:
        groups[(c["factionId"], slug(c["name"]))].append((c, src))

    assigned = 0
    for (fac, sl), items in groups.items():
        if len(items) == 1:
            c, src = items[0]
            rel = os.path.join("cards", "swa", fac, sl + ".webp")
            _write(c, src, rel)
            assigned += 1
        else:
            for c, src in items:
                rel = os.path.join("cards", "swa", fac, f"{sl}-{c.get('points', 0)}.webp")
                _write(c, src, rel)
                assigned += 1

    json.dump(doc, open(CATALOG, "w", encoding="utf-8"), ensure_ascii=False, indent=1)
    print("cartés complétées:", assigned)
    still = [c["name"] for c in cards if c["gameSystem"] == "ARMADA_V15" and not c.get("imageAssetPath")]
    print("restantes sans image:", len(still))
    for n in still:
        print("  -", n)

def _write(c, src, rel):
    outp = os.path.join(ROOT, *rel.split("/"))
    os.makedirs(os.path.dirname(outp), exist_ok=True)
    if not os.path.exists(outp):
        try:
            Image.open(src).convert("RGB").save(outp, "WEBP", quality=82)
        except Exception as e:
            print(f"  ! conversion échouée {src}: {e}")
            return
    c["imageAssetPath"] = rel

if __name__ == "__main__":
    main()
