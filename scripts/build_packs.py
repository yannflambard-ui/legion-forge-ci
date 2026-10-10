#!/usr/bin/env python3
"""Génère assets/packs.json : packs Legion + Armada -> {id, name, gameSystem,
faction, imageAssetPath, cards:[catalog ids]}.

Sources :
- Legion : pages d'expansions Fandom (Catégorie:Expansion Packs). La section
  '===Unit Cards===' du wikitext liste la/les carte(s) d'unité de la boîte.
- Armada : script armada_scrape_packs.py (codes SWM) déjà exécuté.

Le mapping nom-Fandom -> id-catalogue est fait par normalisation + base-name,
avec une table d'override CURATED pour les cas où les deux datasets divergent.
Les cartes absentes du catalogue (unités non encore intégrées) sont omises :
un pack ne marque possédé que ce qui existe réellement dans le catalogue.

Images : téléchargées depuis static.wikia.nocookie.net (via imageinfo),
recadrées/convertis en webp miniature dans assets/cards/packs/.
"""
import json, re, os, sys, time, subprocess, urllib.request, urllib.parse, unicodedata

UA = {'User-Agent': 'Mozilla/5.0 (X11; Linux x86_64) Chrome/120'}
ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..')
IMG_DIR = os.path.join(ROOT, 'app/src/main/assets/cards/packs')

# ── matching helpers ─────────────────────────────────────────────
def norm(n):
    n = unicodedata.normalize('NFKD', n)
    n = ''.join(ch for ch in n if not unicodedata.combining(ch))
    n = re.sub(r'\s*\([^)]*\)\s*', ' ', n.lower())  # retire titre entre ()
    n = re.sub(r'[^a-z0-9 ]', ' ', n)
    return re.sub(r'\s+', ' ', n).strip()

def fetch_wikitext(wiki, title):
    url = f"https://{wiki}.fandom.com/api.php?action=query&prop=revisions&rvprop=content&rvslots=main&titles={urllib.parse.quote(title)}&format=json&formatversion=2"
    try:
        d = json.load(urllib.request.urlopen(urllib.request.Request(url, headers=UA), timeout=25))
        p = d['query']['pages'][0]
        return p.get('revisions', [{}])[0].get('slots', {}).get('main', {}).get('content')
    except Exception:
        return None

def img_url(wiki, filename):
    url = f"https://{wiki}.fandom.com/api.php?action=query&titles={urllib.parse.quote('File:' + filename)}&prop=imageinfo&iiprop=url&format=json&formatversion=2"
    try:
        d = json.load(urllib.request.urlopen(urllib.request.Request(url, headers=UA), timeout=25))
        p = d['query']['pages'][0]
        return p.get('imageinfo', [{}])[0].get('url')
    except Exception:
        return None

def unit_links(c):
    i = c.find('===Unit Cards===')
    if i < 0:
        return []
    seg = c[i + len('===Unit Cards==='):]
    j = seg.find('\n==')
    if j >= 0:
        seg = seg[:j]
    out = []
    for m in re.finditer(r'\*?\s*\[\[([^\]|]+)', seg):
        t = m.group(1).strip()
        if not t.lower().startswith('file:'):
            out.append(t)
    return out

def first_image(c):
    m = re.search(r'\[\[File:([^\]|]+)', c)
    return m.group(1).strip() if m else None

# ── CURATED override: Fandom unit name -> catalog display name ──
# (cas où norm() ne matche pas car les datasets nomment différemment)
OVERRIDE = {
    'Phase I Clone Troopers': 'Clone Trooper Infantry',
    'Phase II Clone Troopers': 'Clone Trooper Infantry',
    'Phase I Clone Troopers Upgrade Expansion': None,  # pas une unité
    'TX-130 Saber-class Fighter Tank': 'Saber-Class Tank',
    'NR-N99 Persuader-Class Tank Droid': 'Persuader-Class Tank Droid',
    'AAT Trade Federation Battle Tank': 'AAT Battle Tank',
    'TX-225 GAVw Occupier Combat Assault Tank': 'TX-225 GAVw Occupier Tank',
    'Republic AT-RT': 'AT-RT',
    'Imperial Royal Guards': None,  # non intégré au catalogue
    'Mandalorian Resistance': 'Mandalorian Warriors',
    'Mandalorian Resistance (Clan Wren)': 'Ursa Wren (Leader of Clan Wren)',
    'Inferno Squad': 'Imperial Special Forces (Inferno Squad)',
    'Super Tactical Droid': 'Super Tactical Command Droid (Command and Control Droid)',
    'Super Tactical Droid (Kraken)': 'Super Tactical Command Droid (Auxiliary Command Droid)',
    'IG-100 MagnaGuards': 'IG-100 MagnaGuard',
    'Emperor Palpatine': 'Darth Vader (The Emperor\'s Apprentice)',
    'Wookiee Warriors': 'Wookiee Warriors (Kashyyyk Defenders)',
    'Chewbacca (Republic)': 'Wookiee Chieftain (Clan Leader)',
    'Boba Fett Operative': 'Boba Fett (Infamous Bounty Hunter)',
    'Boba Fett': 'Boba Fett (Infamous Bounty Hunter)',
    'Bossk': 'Bossk (Trandoshan Terror)',
    'Chewbacca Operative': 'Chewbacca (Walking Carpet)',
    'Luke Skywalker Operative': 'Luke Skywalker (Hero of the Rebellion)',
    'Darth Vader Operative': 'Darth Vader (Dark Lord of the Sith)',
    'Sabine Wren Operative': 'Sabine Wren (Explosive Artist)',
    'Han Solo Commander': 'Han Solo (Unorthodox General)',
    'Yoda': 'Yoda (Grand Master of the Jedi Order)',
    'Clone Commander': 'Clone Commander (Trained For Leadership)',
    'Jyn Erso': 'Jyn Erso (Stardust)',
    'Obi-Wan Kenobi': 'Obi-Wan Kenobi (Civilized Warrior)',
    'Boba Fett (Daimyo)': 'Boba Fett (Daimyo of Mos Espa)',
}

# Pack-level aliases : certains packs listent une unité par un autre nom
# (ex. les "Strike Team" sont dans la même boîte que l'unité de base).
PACK_UNIT_FALLBACK = {
    'lg:arc-troopers-unit-expansion': ['ARC Troopers'],
    'lg:bx-series-droid-commandos-unit-expansion': ['BX-Series Droid Commandos'],
    'lg:rebel-commandos-unit-expansion': ['Rebel Commandos'],
    'lg:scout-troopers-unit-expansion': ['Scout Troopers'],
    'lg:super-tactical-droid-commander-expansion': ['Super Tactical Command Droid (Command and Control Droid)'],
    'lg:grand-master-yoda-commander-expansion': ['Yoda (Grand Master of the Jedi Order)'],
}

def main():
    os.makedirs(IMG_DIR, exist_ok=True)
    catalog = json.load(open(os.path.join(ROOT, 'app/src/main/assets/catalog.json')))['cards']
    # legion unit_id by normalized display name
    cat_by_norm = {}
    cat_by_display = {}
    for c in catalog:
        if c['gameSystem'] == 'LEGION_V2' and c['kind'] == 'LEGION_UNIT':
            cat_by_norm.setdefault(norm(c['name']), c['id'])
            cat_by_display[c['name']] = c['id']

    legion_packs = [
        "1.4 FD Laser Cannon Team Unit Expansion", "74-Z Speeder Bikes Unit Expansion",
        "A-A5 Speeder Truck Unit Expansion", "AAT Trade Federation Battle Tank Unit Expansion",
        "ARC Troopers Unit Expansion", "AT-RT Unit Expansion", "AT-ST Unit Expansion",
        "Agent Kallus Commander Expansion", "Anakin Skywalker Commander Expansion",
        "B1 Battle Droids Unit Expansion", "B2 Super Battle Droids Unit Expansion",
        "BARC Speeder Unit Expansion", "BX-series Droid Commandos Unit Expansion",
        "Boba Fett (Daimyo) Operative Expansion", "Boba Fett Operative Expansion",
        "Bossk Operative Expansion", "Cad Bane Operative Expansion",
        "Cassian Andor and K-2SO Commander Expansion", "Chewbacca Operative Expansion",
        "Clan Wren Unit Expansion", "Clone Captain Rex Commander Expansion", "Clone Wars Core Set",
        "Count Dooku Commander Expansion", "DSD1-Dwarf Spider Droid Unit Expansion",
        "Darth Maul and Sith Probe Droids Operative Expansion", "Darth Vader Operative Expansion",
        "Dewback Rider Unit Expansion", "Director Orson Krennic Commander Expansion",
        "Droidekas Unit Expansion", "E-Web Heavy Blaster Team Unit Expansion",
        "Echo Base Defenders Battle Force Starter Set", "Emperor Palpatine Commander Expansion",
        "Fleet Troopers Unit Expansion", "General Veers Commander Expansion",
        "Grand Master Yoda Commander Expansion", "Han Solo Commander Expansion",
        "IG-100 MagnaGuards Unit Expansion", "IG-Series Assassin Droid Operative Expansion",
        "Iden Versio and ID10 Commander Expansion", "Imperial Death Troopers Unit Expansion",
        "Imperial Royal Guards Unit Expansion", "Imperial Shoretroopers Unit Expansion",
        "Imperial Specialists Personnel Expansion", "Imperial Stormtroopers Upgrade Expansion",
        "Infantry Support Platform Unit Expansion", "Inferno Squad Unit Expansion",
        "Jyn Erso Commander Expansion", "LAAT/le Patrol Transport Unit Expansion",
        "Lando Calrissian Commander Expansion", "Legion Core Set", "Leia Organa Commander Expansion",
        "Luke Skywalker Operative Expansion", "NR-N99 Persuader-class Tank Droid Unit Expansion",
        "Padmé Amidala Operative Expansion", "Phase I Clone Troopers Unit Expansion",
        "Phase II Clone Troopers Unit Expansion", "Rebel Commandos Unit Expansion",
        "Rebel Pathfinders Unit Expansion", "Rebel Specialists Personnel Expansion",
        "Rebel Troopers Unit Expansion", "Rebel Veterans Unit Expansion",
        "Republic AT-RT Unit Expansion", "Republic Specialists Personnel Expansion",
        "STAP Riders Unit Expansion", "Sabine Wren Operative Expansion",
        "Scout Troopers Unit Expansion", "Separatist Specialists Personnel Expansion",
        "Snowtroopers Unit Expansion", "Stormtroopers Unit Expansion",
        "Super Tactical Droid Commander Expansion", "T-47 Airspeeder Unit Expansion",
        "TX-130 Saber-class Fighter Tank Unit Expansion",
        "TX-225 GAVw Occupier Combat Assault Tank Unit Expansion", "Tauntaun Riders Unit Expansion",
        "Wookiee Warriors Unit Expansion", "Wookiee Warriors Unit Expansion (Republic)",
        "X-34 Landspeeder Unit Expansion",
    ]

    out = []
    for name in legion_packs:
        c = fetch_wikitext('starwarslegion', name)
        if c is None:
            print(f"[MISS] {name}", file=sys.stderr)
            continue
        img = first_image(c)
        fn_unit = unit_links(c)
        # mapper chaque unité Fandom -> id catalogue
        ids = []
        mapped_names = []
        for fu in fn_unit:
            target = None
            if fu in OVERRIDE:
                tgt = OVERRIDE[fu]
                target = cat_by_display.get(tgt) if tgt else None
            else:
                target = cat_by_norm.get(norm(fu))
            if target:
                ids.append(target)
                mapped_names.append(fu)
        pid = 'lg:' + re.sub(r'[^a-z0-9]+', '-', name.lower()).strip('-')
        pid = re.sub(r'-+', '-', pid)
        # Fallback pack-level si aucun mapping direct (Strike Teams, Cores Sets...)
        if not ids and pid in PACK_UNIT_FALLBACK:
            for disp in PACK_UNIT_FALLBACK[pid]:
                cid = cat_by_display.get(disp) or cat_by_norm.get(norm(disp))
                if cid:
                    ids.append(cid)
                    mapped_names.append(disp)
        out.append({'id': pid, 'gameSystem': 'LEGION_V2', 'name': name,
                    'imageName': img, 'cards': sorted(set(ids)),
                    'mappedUnits': mapped_names})
        print(f"[{pid}] {name} img={img} units={fn_unit} mapped={len(set(ids))}/{len(fn_unit)}", file=sys.stderr)
        time.sleep(0.2)

    json.dump({'schemaVersion': 1, 'packs': out},
              open(os.path.join(ROOT, 'app/src/main/assets/packs.json'), 'w'),
              ensure_ascii=False, indent=1)
    sys.stderr.write(f"\nTOTAL Legion: {len(out)} packs\n")

if __name__ == '__main__':
    main()