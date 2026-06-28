#!/usr/bin/env python3
"""
Block Front — one-time mesh bake (binary output).
Run from ~/bf:
  python3 gen_mesh_data.py \
    ~/storage/downloads/tripo_convert_ea237646-20d8-47e2-be62-ae6c9ebd536e.obj \
    ~/storage/downloads/tripo_convert_9c4f327c-63ce-4e52-9a72-128f2fd3fc42.obj \
    ~/storage/downloads/tripo_model_basecolor.JPEG \
    "~/storage/downloads/tripo_model_basecolor(1).JPEG"

Writes to app/src/main/assets/:
  ruins.bin        — binary mesh for ruins map
  operator.bin     — binary mesh for menu operator
  ruins_tex.jpg    — ruins texture (512x512)
  operator_tex.jpg — operator texture (512x512)

Binary format (big-endian per DataInputStream):
  4 bytes  int   vertCount
  4 bytes  int   idxCount
  vertCount*5*4  floats  x,y,z,u,v per vertex
  idxCount*2     shorts  triangle indices
"""
import sys, os, struct, shutil

def parse_obj(path, scale, keep_every):
    raw_v, raw_vt, faces = [], [], []
    print(f'  Parsing {os.path.basename(path)} ...')
    with open(path) as f:
        for line in f:
            line = line.strip()
            if line.startswith('v '):
                p=line.split(); raw_v.append((float(p[1])*scale,float(p[2])*scale,float(p[3])*scale))
            elif line.startswith('vt '):
                p=line.split(); raw_vt.append((float(p[1]),float(p[2])))
            elif line.startswith('f '):
                parts=line.split()[1:]
                tri=[]
                for pt in parts[:3]:
                    sub=pt.split('/') if '/' in pt else [pt,'0']
                    vi=int(sub[0])-1; vti=int(sub[1])-1 if len(sub)>1 and sub[1] else -1
                    tri.append((vi,vti))
                faces.append(tri)

    kept=[f for i,f in enumerate(faces) if i%keep_every==0]
    vmap,pos_l,uv_l,idx={},{},{},[]
    vmap={}; pos_l=[]; uv_l=[]; idx=[]
    for tri in kept:
        for vi,vti in tri:
            k=(vi,vti)
            if k not in vmap:
                vmap[k]=len(pos_l)
                pos_l.append(raw_v[vi] if vi<len(raw_v) else (0,0,0))
                uv_l.append(raw_vt[vti] if 0<=vti<len(raw_vt) else (0.5,0.5))
            idx.append(vmap[k])
    return pos_l, uv_l, idx

def write_bin(pos_l, uv_l, idx, out_path):
    vc=len(pos_l); ic=len(idx)
    with open(out_path,'wb') as f:
        f.write(struct.pack('>ii', vc, ic))
        for p,u in zip(pos_l,uv_l):
            f.write(struct.pack('>5f', p[0],p[1],p[2], u[0],u[1]))
        for i in idx:
            f.write(struct.pack('>H', i))
    kb=os.path.getsize(out_path)//1024
    print(f'  → {out_path}: {vc} verts, {ic//3} tris, {kb}KB')

def copy_texture(src, dest_name):
    assets='app/src/main/assets'
    os.makedirs(assets,exist_ok=True)
    out=os.path.join(assets,dest_name)
    try:
        from PIL import Image; import io
        img=Image.open(src).resize((512,512))
        buf=io.BytesIO(); img.save(buf,format='JPEG',quality=72)
        with open(out,'wb') as f: f.write(buf.getvalue())
        print(f'  Texture 512×512 → {out} ({len(buf.getvalue())//1024}KB)')
    except ImportError:
        shutil.copy(src,out); print(f'  Texture copied → {out}')
    except Exception as e:
        print(f'  Warning: {e}')

if __name__=='__main__':
    if len(sys.argv)<3: print(__doc__); sys.exit(1)
    op_obj=sys.argv[1]; ru_obj=sys.argv[2]
    ruins_tex   =sys.argv[3] if len(sys.argv)>3 else None
    operator_tex=sys.argv[4] if len(sys.argv)>4 else None

    assets='app/src/main/assets'
    os.makedirs(assets,exist_ok=True)

    print('Baking operator.bin ...')
    pl,ul,idx=parse_obj(op_obj,scale=2.0,keep_every=20)
    write_bin(pl,ul,idx,os.path.join(assets,'operator.bin'))

    print('Baking ruins.bin ...')
    pl,ul,idx=parse_obj(ru_obj,scale=18.0,keep_every=14)
    write_bin(pl,ul,idx,os.path.join(assets,'ruins.bin'))

    if ruins_tex:
        print('Copying ruins texture ...')
        copy_texture(ruins_tex,'ruins_tex.jpg')
    if operator_tex:
        print('Copying operator texture ...')
        copy_texture(operator_tex,'operator_tex.jpg')

    print()
    print('Done! Now run:')
    print('  git add -A && git commit -m "Add binary mesh assets" && git push')
