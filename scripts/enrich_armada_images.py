#!/usr/bin/env python3
"""Enrich catalog.json Armada:
1. Populate imageAssetPath on every Armada card from Ryan Kingston's fleet-builder images,
   converting the source to WebP stored in assets/cards/.
2. Add missing Republic + Separatist ships/squadrons/commanders from the BSData catalog,
   with stats (shipStats embedded from rulesText).
"""
import json, os, re
from PIL import Image

ROOT = os.path.join(os.path.dirname(__file__), "..", "app", "src", "main", "assets")
CATALOG = os.path.join(ROOT, "catalog.json")
BSD = os.path.join(ROOT, "armada_bsdata_catalog.json")
RYK_MANIFEST = "/root/.hermes/cache/browser-use/workspace/ryk_full.json"
RYK_IMGS = "/root/.hermes/cache/browser-use/workspace/ryk_imgs"
CARDS_OUT = os.path.join(ROOT, "cards")

def norm(s):
    n = re.sub(r"\s+", " ", s.lower().replace("-", " ")).strip()
    return n.replace("dreadought", "dreadnought")

def toks(s):
    return set(norm(s).split())

def match_score(title, full):
    return len(toks(title) & toks(full))

def _toks(s):
    t = set(norm(s).split())
    out = set()
    for w in t:
        w2 = {"i":"1","ii":"2","iii":"3","iv":"4","v":"5"}.get(w, w)
        if w.startswith("mk"): w2 = "mark " + w[2:]
        out.add(w2)
    return out

# Curated overrides for vocabulary differences (ISD/SSD/Mk2/Executor) that token
# matching can't resolve. catalog name -> ryk image filename.
IMAGE_OVERRIDES = {
    "Executor I-Class Star Dreadnought": "ssd-executor-1.png",
    "Executor II-Class Star Dreadnought": "ssd-executor-2.png",
    "Imperial Star Destroyer Cymoon 1 Refit": "isd-cymoon-1-refit.png",
    "Imperial Star Destroyer Kuat Refit": "isd-kuat-refit.png",
    "Star Dreadnought Assault Prototype": "ssd-assault-prototype.png",
    "Star Dreadnought Command Prototype": "ssd-command-prototype.png",
    "Assault Frigate Mark II A": "assault-frigate-mark-ii-a.png",
    "Assault Frigate Mark II B": "assault-frigate-mark-ii-b.png",
    "Gozanti-class Cruisers": "swm18-gozanti-class-cruisers.png",
    "Gozanti-class Cruiser": "swm18-gozanti-class-cruisers-2.jpg",
    "Victory I-class Star Destroyer": "victory-i.png",
    "Victory I-Class Star Destroyer": "victory-ib.jpg",
    "Venator II-class Star Destroyer": "venator-2.png",
}

def find_image(title, sets=("ship",)):
    """Return (source_path, webp_basename) for the best-matching ryk image, else (None,None).
    Matches within the given ryk sets (default ships only) using token-subset with
    roman-numeral/mk normalization, plus curated overrides for vocabulary differences."""
    if title in IMAGE_OVERRIDES:
        fname = IMAGE_OVERRIDES[title]
        p = os.path.join(RYK_IMGS, fname)
        if os.path.exists(p):
            return p, f"cd_{fname.rsplit('.',1)[0]}.webp"
    ryk = json.load(open(RYK_MANIFEST, encoding="utf-8"))
    t = _toks(title)
    best_src, best_name, best_score = None, None, 0
    for key in sets:
        lst = ryk.get(key, [])
        if not isinstance(lst, list): continue
        for c in lst:
            if not isinstance(c, dict) or not c.get("image"): continue
            r = _toks(c["title"])
            if not r.issubset(t): continue
            if len(r) > best_score:
                best_score = len(r)
                p = os.path.join(RYK_IMGS, c["image"])
                if os.path.exists(p):
                    best_src = p
                    best_name = f"cd_{c['image'].rsplit('.',1)[0]}.webp"
    if best_score >= 2 and best_src:
        return best_src, best_name
    return None, None

def convert_webp(src, webp_name):
    outp = os.path.join(CARDS_OUT, webp_name)
    if os.path.exists(outp):
        return outp
    os.makedirs(CARDS_OUT, exist_ok=True)
    Image.open(src).convert("RGB").save(outp, "WEBP", quality=82)
    return outp

def ship_stats(rt):
    try: d = json.loads(rt)
    except: return None
    hull = d.get("hull"); shield = d.get("shield") or {}
    if hull is None: return None
    max_speed = None
    for row in d.get("speed-chart-rows", []) or []:
        for k in (row.get("values") or {}).keys():
            if k.isdigit() and (max_speed is None or int(k) > max_speed): max_speed = int(k)
    return {"hull": hull, "shield": {
        "front": shield.get("front",0),"right": shield.get("right",0),
        "left": shield.get("left",0),"rear": shield.get("rear",0)},
        "maxSpeed": max_speed or 1, "kind": "SHIP"}

def squad_stats(rt):
    try: d = json.loads(rt)
    except: return None
    ch = (d.get("characteristics") or {}) if isinstance(d, dict) else {}
    hv = ch.get("Hull Value"); sp = ch.get("Speed")
    if hv is None: return None
    return {"hull": int(hv), "speed": int(sp) if sp and str(sp).isdigit() else 3, "kind": "SQUADRON"}

FAC_APP = {"galactic-empire":"empire","rebel-alliance":"rebel",
           "galactic-republic":"republic","separatist-alliance":"separatist"}

# --- Legion HQ images (partial, v1-era cards) ---
LEGIONHQ_IMG = "/tmp/legionhq/public/images"
# map CardKind -> LegionHQ folder
LEGION_DIRS = {"LEGION_UNIT":"unitCards", "LEGION_UPGRADE":"upgradeCards",
               "COMMANDER":"commandCards"}
def legion_image_lookup():
    """Build {norm(name): source_path} across all LegionHQ image folders."""
    out = {}
    for sub in ["unitCards","upgradeCards","commandCards","battleCards"]:
        imgdir = os.path.join(LEGIONHQ_IMG, sub)
        if not os.path.isdir(imgdir): continue
        for f in os.listdir(imgdir):
            if not f.lower().endswith((".jpeg",".jpg",".png")): continue
            out.setdefault(norm(os.path.splitext(f)[0]), os.path.join(imgdir, f))
    return out

def find_legion_image(legion_map, title):
    n = norm(title)
    if n in legion_map:
        return legion_map[n]
    # token overlap fallback
    t = toks(title)
    best=None; bs=0
    for name, src in legion_map.items():
        sc = len(t & toks(name))
        if sc>bs: bs, best = sc, (name, src)
    return best[1] if best and bs>=2 else None

def main():
    catalog = json.load(open(CATALOG, encoding="utf-8"))
    bs = json.load(open(BSD, encoding="utf-8"))
    if isinstance(bs, dict): bs = bs.get("cards", [])
    existing_ids = {x["id"] for x in catalog["cards"]}

    img = {"ship":0,"squadron":0,"commander":0,"upgrade":0,"legion_unit":0,"legion_upgrade":0}
    tag = {"ARMADA_SHIP":"ship","ARMADA_SQUADRON":"squadron","COMMANDER":"commander","ARMADA_UPGRADE":"upgrade"}
    to_add = []

    legion_map = legion_image_lookup()

    # 1) Existing Armada cards: place images
    ryk_set = {"ARMADA_SHIP":("ship",), "ARMADA_SQUADRON":("squadron",),
               "COMMANDER":("commander",), "ARMADA_UPGRADE":("officer","title","weapons-team",
               "offensive-retrofit","defensive-retrofit","experimental-retrofit","ordnance",
               "ion-cannons","turbolasers","support-team","fleet-command","fleet-support","super-weapon")}
    for card in catalog["cards"]:
        k = card.get("kind")
        if k not in ryk_set: continue
        src, wname = find_image(card["name"], sets=ryk_set[k])
        if src:
            convert_webp(src, wname)
            card["imageAssetPath"] = os.path.join("cards", wname)
            img[tag[k]] += 1

    # 1b) Legion cards: place partial images from LegionHQ
    for card in catalog["cards"]:
        k = card.get("kind")
        if k not in ("LEGION_UNIT","LEGION_UPGRADE"): continue
        src = find_legion_image(legion_map, card["name"])
        if src:
            wname = f"lg_{card['id'].rsplit('/',1)[-1]}.webp"
            convert_webp(src, wname)
            card["imageAssetPath"] = os.path.join("cards", wname)
            if k=="LEGION_UNIT": img["legion_unit"] += 1
            else: img["legion_upgrade"] += 1

    # 2) Add Republic/Sep missing ships, squadrons, commanders from BSData
    for x in bs:
        if x.get("factionId") not in ("galactic-republic","separatist-alliance"): continue
        k = x.get("kind")
        if k not in ("ARMADA_SHIP","ARMADA_SQUADRON","COMMANDER"): continue
        if x["id"] in existing_ids: continue
        src, wname = find_image(x["name"], sets=ryk_set[k])
        ap = None
        if src:
            convert_webp(src, wname)
            ap = os.path.join("cards", wname)
        st = None
        if k == "ARMADA_SHIP": st = ship_stats(x.get("rulesText"))
        elif k == "ARMADA_SQUADRON": st = squad_stats(x.get("rulesText"))
        new = {
            "id": x["id"], "gameSystem":"ARMADA_V15", "kind": k, "name": x["name"],
            "points": x["points"], "factionId": FAC_APP.get(x["factionId"], x["factionId"]),
            "legionRank": None, "upgradeSlots": x.get("upgradeSlots", []),
            "allowedUpgradeSlots": x.get("allowedUpgradeSlots", []),
            "commander": k=="COMMANDER" or "COMMANDER" in x.get("upgradeSlots",[]),
            "unique": x.get("unique", False),
            "imageUrl": None, "imageAssetPath": ap,
            "rulesText": x.get("rulesText"),
            "shipStats": json.dumps(st, separators=(",",":")) if st else None,
        }
        to_add.append(new)
        if src: img[tag[k]] += 1

    catalog["cards"].extend(to_add)

    # Cleanup: null any imageAssetPath whose file is not on disk (stale refs from
    # earlier runs / matcher changes).
    for card in catalog["cards"]:
        ap = card.get("imageAssetPath")
        if ap and not os.path.exists(os.path.join(CARDS_OUT, os.path.basename(ap))):
            card["imageAssetPath"] = None

    json.dump(catalog, open(CATALOG,"w",encoding="utf-8"), ensure_ascii=False, indent=1)

    total_webp = sum(len(f) for _,_,f in os.walk(CARDS_OUT))
    print("images placed:", img)
    print("republic/sep cards added:", len(to_add))
    print("webp files in assets/cards:", total_webp)
    print("total cards:", len(catalog["cards"]))

if __name__ == "__main__":
    main()
