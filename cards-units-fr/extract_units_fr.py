#!/usr/bin/env python3
"""Extract unit FR cards from iron-squadron Units PDF plates (AMG Legion).

Property (verified vision) : pages upside-down (180) AND cards rotated 90 CCW.
App stores squad-unit cards LANDSCAPE 1039x726 (W/H 1.431). So: ROTATE_180 page,
find card-sheet bbox, logical 3x3 split, rotate each cell CCW -> landscape unit card.

Usage: python3 extract_units_fr.py <out_dir> <pdf1> [...]
"""
import cv2, numpy as np, subprocess, os, sys, json, glob, shutil

SOURCES_MANIFEST=os.environ.get('SOURCES_MANIFEST','/tmp/sources_manifest.json')
try:
    SOURCES={k:v for k,v in json.load(open(SOURCES_MANIFEST)).items()}
except Exception:
    SOURCES={}
def src_meta(bn):
    e=SOURCES.get(bn) or next((v for k,v in SOURCES.items() if os.path.basename(k)==bn), {})
    return e.get('url'), e.get('date'), bool(e.get('dateUnknown'))

def render_pdf(pdf, outdir, dpi=300):
    shutil.rmtree(outdir, ignore_errors=True); os.makedirs(outdir,exist_ok=True)
    subprocess.run(['pdftoppm','-png',f'-r',str(dpi),pdf,os.path.join(outdir,'p')],check=True)
    return sorted(glob.glob(os.path.join(outdir,'p-*.png')))

def sheet_bbox(img):
    H,W=img.shape[:2]; g=cv2.cvtColor(img,cv2.COLOR_BGR2GRAY)
    thr=cv2.adaptiveThreshold(g,255,cv2.ADAPTIVE_THRESH_MEAN_C,cv2.THRESH_BINARY_INV,51,10)
    cnt,_=cv2.findContours(thr,cv2.RETR_EXTERNAL,cv2.CHAIN_APPROX_SIMPLE)
    best=None
    for c in cnt:
        x,y,w,h=cv2.boundingRect(c)
        if w<500 or h<500: continue
        r=w/h
        if 0.5<r<1.0 and (best is None or w*h>best[2]):
            best=(x,y,w*h,x+w,y+h)
    if best: return best[0],best[1],best[3]-best[0],best[4]-best[1],best[3],best[4]
    dark=g<100
    rows=np.where(dark.sum(1)>W*0.01)[0]; cols=np.where(dark.sum(0)>H*0.01)[0]
    if not len(rows) or not len(cols): return None
    return cols[0],rows[0],cols[-1]-cols[0],rows[-1]-rows[0],cols[-1],rows[-1]

def cell_empty(cell, thr=0.03):
    return bool((cell<80).mean()<thr)

def extract_page(img, out_json, counter, src_bn):
    img=cv2.rotate(img,cv2.ROTATE_180)   # pages upside-down
    bb=sheet_bbox(img)
    res=[]
    if not bb: return res
    x,y,_,_,x1,y1=bb
    cw=(x1-x)//3; ch=(y1-y)//3
    for j in range(3):
        for i in range(3):
            cell=img[y+j*ch:y+(j+1)*ch, x+i*cw:x+(i+1)*cw]
            if cell.size==0 or cell_empty(cell): continue
            card=cv2.rotate(cell,cv2.ROTATE_90_COUNTERCLOCKWISE)  # paysage unit card
            fname=f"card_{counter[0]:04d}.png"
            cv2.imwrite(os.path.join(out_json,'imgs',fname),card)
            url,dt,unk=src_meta(src_bn)
            res.append({'name':'','img':fname,'src':src_bn,'src_url':url,'src_date':dt,'src_date_unknown':unk})
            counter[0]+=1
    return res

def extract_pdf(pdf, outdir, out_json, counter):
    pages=render_pdf(pdf,outdir)
    # detect grid ONCE from first page with a full sheet, reuse for all pages
    fixed=None
    for pg in pages:
        img=cv2.imread(pg); img=cv2.rotate(img,cv2.ROTATE_180)
        bb=sheet_bbox(img)
        if bb:
            x,y,_,_,x1,y1=bb
            cw=(x1-x)//3; ch=(y1-y)//3
            if cw>500 and ch>700:   # full 3x3 sheet
                fixed=(x,y,cw,ch); break
    allres=[]
    for pg in pages:
        img=cv2.imread(pg); img=cv2.rotate(img,cv2.ROTATE_180)
        if fixed is None:
            bb=sheet_bbox(img)
            if not bb: continue
            x,y,_,_,x1,y1=bb; cw=(x1-x)//3; ch=(y1-y)//3
        else:
            x,y,cw,ch=fixed
        for j in range(3):
            for i in range(3):
                cell=img[y+j*ch:y+(j+1)*ch, x+i*cw:x+(i+1)*cw]
                if cell.size==0 or cell_empty(cell): continue
                card=cv2.rotate(cell,cv2.ROTATE_90_COUNTERCLOCKWISE)
                fname=f"card_{counter[0]:04d}.png"
                cv2.imwrite(os.path.join(out_json,'imgs',fname),card)
                url,dt,unk=src_meta(os.path.basename(pdf))
                allres.append({'name':'','img':fname,'src':os.path.basename(pdf),'src_url':url,'src_date':dt,'src_date_unknown':unk})
                counter[0]+=1
        print(f"  {os.path.basename(pg)} -> cumul {counter[0]}",flush=True)
    return allres

if __name__=='__main__':
    out_json=sys.argv[1]; pdfs=sys.argv[2:]
    shutil.rmtree(out_json,ignore_errors=True); os.makedirs(os.path.join(out_json,'imgs'),exist_ok=True)
    allres=[]; counter=[0]
    for pdf in pdfs:
        print("processing",os.path.basename(pdf),flush=True)
        allres+=extract_pdf(pdf,f'/tmp/rawu_{os.path.basename(pdf)[:4]}',out_json,counter)
    json.dump(allres,open(os.path.join(out_json,'cards.json'),'w'),ensure_ascii=False,indent=1)
    print("TOTAL",counter[0])
