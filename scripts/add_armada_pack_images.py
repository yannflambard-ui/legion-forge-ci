#!/usr/bin/env python3
"""Complète les packs Armada de assets/packs.json avec l'image de boîte
(champ image1 du wikitext Fandom), puis télécharge les miniatures.

Usage : python3 add_armada_pack_images.py   (idempotent)
"""
import json, os, re, sys, subprocess, time, urllib.request, urllib.parse

UA = {'User-Agent': 'Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 Chrome/120 Safari/537.36'}
ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..')
ASSETS = os.path.join(ROOT, 'app/src/main/assets')
IMG_DIR = os.path.join(ASSETS, 'cards/packs')

# nom de page Fandom par SWM (utilisé pour résoudre image1)
PACK_TITLE = {
    'swm01': 'Star Wars: Armada Core Set', 'swm02': 'Victory-class Star Destroyer Expansion Pack',
    'swm03': 'CR90 Corellian Corvette Expansion Pack', 'swm04': 'Nebulon-B Frigate Expansion Pack',
    'swm05': 'Assault Frigate Mark II Expansion Pack', 'swm06': 'Gladiator-class Star Destroyer Expansion Pack',
    'swm07': 'Rebel Fighter Squadrons Expansion Pack', 'swm08': 'Imperial Fighter Squadrons Expansion Pack',
    'swm11': 'Imperial-class Star Destroyer Expansion Pack', 'swm12': 'MC30c Frigate Expansion Pack',
    'swm13': 'Home One Expansion Pack', 'swm14': 'Rogues and Villains Expansion Pack',
    'swm15': 'Imperial Raider Expansion Pack', 'swm16': 'Interdictor Expansion Pack',
    'swm17': 'Liberty Expansion Pack', 'swm18': 'Imperial Assault Carriers Expansion Pack',
    'swm19': 'Rebel Transports Expansion Pack', 'swm20': 'Super Star Destroyer Expansion Pack',
    'swm21': 'Phoenix Home Expansion Pack', 'swm22': 'Imperial Light Cruiser Expansion Pack',
    'swm23': 'Rebel Fighter Squadrons II Expansion Pack', 'swm24': 'Imperial Fighter Squadrons II Expansion Pack',
    'swm26': 'Imperial Light Carrier Expansion Pack', 'swm27': 'Hammerhead Corvettes Expansion Pack',
    'swm29': 'Chimaera Expansion Pack', 'swm30': 'Profundity Expansion Pack',
    'swm32': 'Nadiri Starhawk Expansion Pack', 'swm33': 'Onager-class Star Destroyer Expansion Pack',
    'swm34': 'Galactic Republic Fleet Starter', 'swm35': 'Separatist Alliance Fleet Starter',
    'swm36': 'Republic Fighter Squadrons Expansion Pack', 'swm37': 'Separatist Fighter Squadrons Expansion Pack',
    'swm38': 'Upgrade Card Collection', 'swm40': 'Pelta-class Frigate Expansion Pack',
    'swm41': 'Venator-class Star Destroyer Expansion Pack', 'swm42': 'Invisible Hand Expansion Pack',
    'swm43': 'Recusant-class Destroyer Expansion Pack',
}

def fetch(title):
    url = f"https://starwars-armada.fandom.com/api.php?action=query&prop=revisions&rvprop=content&rvslots=main&titles={urllib.parse.quote(title)}&format=json&formatversion=2"
    try:
        d = json.load(urllib.request.urlopen(urllib.request.Request(url, headers=UA), timeout=25))
        p = d['query']['pages'][0]
        return p.get('revisions', [{}])[0].get('slots', {}).get('main', {}).get('content')
    except Exception:
        return None

def image1(c):
    m = re.search(r'image1\s*=\s*([^\n|]+)', c or '')
    if not m:
        return None
    v = m.group(1).strip()
    v = re.sub(r'\}\}.*$', '', v).strip()  # retire résidus de template
    return v or None

def wikia_url(wiki, filename):
    url = f"https://{wiki}.fandom.com/api.php?action=query&titles={urllib.parse.quote('File:' + filename)}&prop=imageinfo&iiprop=url&format=json&formatversion=2"
    try:
        d = json.load(urllib.request.urlopen(urllib.request.Request(url, headers=UA), timeout=25))
        p = d['query']['pages'][0]
        return p.get('imageinfo', [{}])[0].get('url')
    except Exception:
        return None

def conv(path):
    from PIL import Image
    im = Image.open(path).convert('RGB')
    w, h = im.size
    if max(w, h) > 520:
        s = 520 / max(w, h)
        im = im.resize((int(w * s), int(h * s)), Image.LANCZOS)
    im.save(path, 'WEBP', quality=82, method=4)

def main():
    os.makedirs(IMG_DIR, exist_ok=True)
    fp = os.path.join(ASSETS, 'packs.json')
    doc = json.load(open(fp))
    done = 0
    for pk in doc['packs']:
        if pk['gameSystem'] != 'ARMADA_V15':
            continue
        title = PACK_TITLE.get(pk['id'])
        if not title:
            continue
        # re-tente si pas d'image OU si le fichier webp n'existe pas encore
        if pk.get('imageName'):
            base = os.path.splitext(os.path.basename(re.sub(r'[\\/:*?"<>|]', '_', pk['imageName'])))[0]
            if os.path.exists(os.path.join(IMG_DIR, base + '.webp')):
                done += 1
                continue
        c = fetch(title)
        img = image1(c)
        if not img:
            continue
        pk['imageName'] = img
        base = os.path.splitext(os.path.basename(re.sub(r'[\\/:*?"<>|]', '_', img)))[0]
        out = os.path.join(IMG_DIR, base + '.webp')
        if not os.path.exists(out):
            direct = wikia_url('starwars-armada', img)
            if direct:
                url = direct + '/scale-to-width-down/520'
                tmp = out + '.raw'
                try:
                    subprocess.run(['curl', '-s', '-f', '-o', tmp, '--max-time', '40',
                                    '-H', 'User-Agent: Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 Chrome/120 Safari/537.36',
                                    '-H', 'Accept: image/avif,image/webp,image/apng,image/*,*/*;q=0.8',
                                    '-H', 'Referer: https://starwars-armada.fandom.com/', url],
                                   check=True, capture_output=True)
                    conv(tmp)
                    os.replace(tmp, out)
                    done += 1
                    print(f"[OK] {pk['id']} -> {os.path.basename(out)}", file=sys.stderr)
                except Exception as e:
                    print(f"[ERR] {pk['id']}: {e}", file=sys.stderr)
                    if os.path.exists(tmp):
                        os.remove(tmp)
                time.sleep(0.2)
    json.dump(doc, open(fp, 'w'), ensure_ascii=False, indent=1)
    sys.stderr.write(f"\nArmada images now: {done}  (sauvegarde packs.json)\n")

if __name__ == '__main__':
    main()