#!/usr/bin/env python3
"""Télécharge les images de packs (boîtes) depuis static.wikia.nocookie.net et
les convertit en webp miniatures dans assets/cards/packs/<imageName>.

Source : le champ imageName de assets/packs.json (nom Fandom du fichier).
Taille cible : ~520px max (miniature lisible à la table), quality 82.
Réutilise les headers UA (static.wikia accepte un simple User-Agent navigateur).
"""
import json, os, re, sys, subprocess, time, urllib.request, urllib.parse
from PIL import Image

UA = {'User-Agent': 'Mozilla/5.0 (X11; Linux x86_64) Chrome/120'}
ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..')
SRC_FILE = os.path.join(ROOT, 'app/src/main/assets/packs.json')
OUT_DIR = os.path.join(ROOT, 'app/src/main/assets/cards/packs')
MAXDIM = 520
QUALITY = 82

def wikia_url(wiki, filename):
    """Résout File:<filename> -> URL directe static.wikia.nocookie.net via imageinfo."""
    url = f"https://{wiki}.fandom.com/api.php?action=query&titles={urllib.parse.quote('File:' + filename)}&prop=imageinfo&iiprop=url&format=json&formatversion=2"
    try:
        d = json.load(urllib.request.urlopen(urllib.request.Request(url, headers=UA), timeout=25))
        p = d['query']['pages'][0]
        return p.get('imageinfo', [{}])[0].get('url')
    except Exception:
        return None

def convert_webp(path):
    im = Image.open(path).convert('RGB')
    w, h = im.size
    if max(w, h) > MAXDIM:
        scale = MAXDIM / max(w, h)
        im = im.resize((int(w * scale), int(h * scale)), Image.LANCZOS)
    im.save(path, 'WEBP', quality=QUALITY, method=4)

def main():
    os.makedirs(OUT_DIR, exist_ok=True)
    packs = json.load(open(SRC_FILE))['packs']
    done = 0
    for pk in packs:
        fn = pk.get('imageName')
        if not fn:
            continue
        fn_clean = re.sub(r'[\\/:*?"<>|]', '_', fn)
        if not fn_clean.lower().endswith(('.png', '.jpg', '.jpeg', '.webp')):
            fn_clean += '.webp'
        out_path = os.path.join(OUT_DIR, fn_clean.replace('.png', '.webp').replace('.jpg', '.webp').replace('.jpeg', '.webp'))
        if os.path.exists(out_path):
            done += 1
            continue
        # résout l'URL (léger cache mémoire par nom)
        direct = wikia_url('starwarslegion', fn)
        if not direct:
            print(f"[NO-URL] {pk['name']} ({fn})", file=sys.stderr)
            continue
        # version thumbnail à 520px, suffixe scale-to-width-down/520
        url = direct + '/scale-to-width-down/520'
        tmp = out_path + '.raw'
        try:
            # curl (pas urllib) : static.wikia répond 403 à urllib mais 200 à curl
            # avec des headers navigateur complets (UA + Referer + Accept image)
            subprocess.run(
                ['curl', '-s', '-f', '-o', tmp, '--max-time', '40',
                 '-H', 'User-Agent: Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36',
                 '-H', 'Accept: image/avif,image/webp,image/apng,image/*,*/*;q=0.8',
                 '-H', 'Referer: https://starwarslegion.fandom.com/',
                 url],
                check=True, capture_output=True)
            data = open(tmp, 'rb').read()
            convert_webp(tmp)
            os.replace(tmp, out_path)
            done += 1
            print(f"[OK] {pk['name']} -> {os.path.basename(out_path)} ({len(data)}b)", file=sys.stderr)
        except Exception as e:
            print(f"[ERR] {pk['name']}: {e}", file=sys.stderr)
            if os.path.exists(tmp):
                os.remove(tmp)
        time.sleep(0.2)
    print(f"\nTotal images: {done}/{len(packs)} pays => {OUT_DIR}")

if __name__ == '__main__':
    main()