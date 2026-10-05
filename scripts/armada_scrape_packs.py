#!/usr/bin/env python3
"""Scrape Armada pack pages (Fandom API) -> carte -> [SWM codes].
Chaque page de pack (ex "Chimaera Expansion Pack") liste ses cartes dans
les sections Ship Cards / Squadron Cards / Upgrades. On extrait les noms
et on les associe au code SWM de la page (sku = SWMxx).
"""
import json, re, urllib.request, urllib.parse, time, sys

UA = {'User-Agent': 'Mozilla/5.0 (X11; Linux x86_64) Chrome/120'}
API = "https://starwars-armada.fandom.com/api.php"

def get_wikitext(title):
    url = f"{API}?action=query&prop=revisions&rvprop=content&rvslots=main&titles={urllib.parse.quote(title)}&format=json&formatversion=2"
    d = json.load(urllib.request.urlopen(urllib.request.Request(url, headers=UA), timeout=25))
    p = d['query']['pages'][0]
    if 'missing' in p:
        return None
    return p['revisions'][0]['slots']['main']['content']

def extract_sku(c):
    m = re.search(r'sku\s*=\s*([A-Za-z]*\d+)', c)
    return m.group(1) if m else None

def extract_cards(c):
    """Extrait les noms de cartes listés dans la page pack."""
    names = set()
    # Lignes * [[Nom]] ou * • [[Nom]] ou * [[Nom|x]] (x2)
    for m in re.finditer(r'^\s*\*[•*]?\s*\[\[([^\]|]+)', c, re.M):
        names.add(m.group(1).strip())
    # Lignes *• [[Nom]] sans espace
    for m in re.finditer(r'^\s*\*•\s*\[\[([^\]|]+)', c, re.M):
        names.add(m.group(1).strip())
    return names

def main():
    # packs Armada depuis la page Products (manuellement listés ici, codes connus)
    packs = [
        ("Armada Core Set","SWM01"),("Victory-class Star Destroyer Expansion Pack","SWM02"),
        ("CR90 Corellian Corvette Expansion Pack","SWM03"),("Nebulon-B Frigate Expansion Pack","SWM04"),
        ("Assault Frigate Mark II Expansion Pack","SWM05"),("Gladiator-class Star Destroyer Expansion Pack","SWM06"),
        ("Rebel Fighter Squadrons Expansion Pack","SWM07"),("Imperial Fighter Squadrons Expansion Pack","SWM08"),
        ("Imperial-class Star Destroyer Expansion Pack","SWM11"),("MC30c Frigate Expansion Pack","SWM12"),
        ("Home One Expansion Pack","SWM13"),("Rogues and Villains Expansion Pack","SWM14"),
        ("Imperial Raider Expansion Pack","SWM15"),("Interdictor Expansion Pack","SWM16"),
        ("Liberty Expansion Pack","SWM17"),("Imperial Assault Carriers Expansion Pack","SWM18"),
        ("Rebel Transports Expansion Pack","SWM19"),("Super Star Destroyer Expansion Pack","SWM20"),
        ("Phoenix Home Expansion Pack","SWM21"),("Imperial Light Cruiser Expansion Pack","SWM22"),
        ("Rebel Fighter Squadrons II Expansion Pack","SWM23"),("Imperial Fighter Squadrons II Expansion Pack","SWM24"),
        ("Imperial Light Carrier Expansion Pack","SWM26"),("Hammerhead Corvettes Expansion Pack","SWM27"),
        ("Chimaera Expansion Pack","SWM29"),("Profundity Expansion Pack","SWM30"),
        ("Nadiri Starhawk Expansion Pack","SWM32"),("Onager-class Star Destroyer Expansion Pack","SWM33"),
        ("Galactic Republic Fleet Starter","SWM34"),("Separatist Alliance Fleet Starter","SWM35"),
        ("Republic Fighter Squadrons Expansion Pack","SWM36"),("Separatist Fighter Squadrons Expansion Pack","SWM37"),
        ("Upgrade Card Collection","SWM38"),("Pelta-class Frigate Expansion Pack","SWM40"),
        ("Venator-class Star Destroyer Expansion Pack","SWM41"),("Invisible Hand Expansion Pack","SWM42"),
        ("Recusant-class Destroyer Expansion Pack","SWM43"),
    ]
    out = {}
    for name, code in packs:
        c = get_wikitext(name)
        if c is None:
            print(f"[MISS] {code} {name}", file=sys.stderr)
            continue
        cards = extract_cards(c)
        for card in cards:
            out.setdefault(card, []).append(code)
        print(f"[OK] {code} {name} -> {len(cards)} cartes", file=sys.stderr)
        time.sleep(0.3)
    json.dump(out, open('/tmp/armada_cards_packs.json','w'), ensure_ascii=False, indent=1)
    print(f"\nTOTAL {len(out)} cartes distinctes mappées")

if __name__ == "__main__":
    main()