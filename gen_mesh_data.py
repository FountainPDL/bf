#!/usr/bin/env python3
"""
Block Front — one-time mesh bake.
Run from ~/bf:
  python3 gen_mesh_data.py \
    ~/storage/downloads/tripo_convert_ea237646-20d8-47e2-be62-ae6c9ebd536e.obj \
    ~/storage/downloads/tripo_convert_9c4f327c-63ce-4e52-9a72-128f2fd3fc42.obj
"""
import sys, os

def bake(path, class_name, scale, keep_every, out_dir):
    raw_v, raw_vt, faces = [], [], []
    print(f'  Parsing {os.path.basename(path)} ...')
    with open(path) as f:
        for line in f:
            line = line.strip()
            if line.startswith('v '):
                p = line.split()
                raw_v.append((float(p[1])*scale, float(p[2])*scale, float(p[3])*scale))
            elif line.startswith('vt '):
                p = line.split()
                raw_vt.append((float(p[1]), float(p[2])))
            elif line.startswith('f '):
                parts = line.split()[1:]
                tri = []
                for pt in parts[:3]:
                    sub = pt.split('/') if '/' in pt else [pt,'0']
                    vi  = int(sub[0])-1
                    vti = int(sub[1])-1 if len(sub)>1 and sub[1] else -1
                    tri.append((vi,vti))
                faces.append(tri)
    kept=[f for i,f in enumerate(faces) if i%keep_every==0]
    vmap,pos,uvs,idx={},{},{},[],[]
    vmap,pos_l,uv_l,idx=[],[]
    vmap={}
    for tri in kept:
        for vi,vti in tri:
            k=(vi,vti)
            if k not in vmap:
                vmap[k]=len(pos_l)
                pos_l.append(raw_v[vi] if vi<len(raw_v) else (0,0,0))
                uv_l.append(raw_vt[vti] if 0<=vti<len(raw_vt) else (0.5,0.5))
            idx.append(vmap[k])
    verts=[]
    for p2,u in zip(pos_l,uv_l):
        verts.extend([p2[0],p2[1],p2[2],u[0],u[1]])
    lines=[
        'package com.fountainpdl.blockfront;',
        '/** Auto-generated — do not edit. */',
        f'public final class {class_name} {{',
        f'    public static final int VERT_COUNT={len(pos_l)};',
        f'    public static final int IDX_COUNT={len(idx)};',
        '    public static final float[] V={',
    ]
    row=[]
    for v in verts:
        row.append(f'{v:.6f}f')
        if len(row)==5: lines.append('        '+','.join(row)+','); row=[]
    if row: lines.append('        '+','.join(row)+',')
    lines+=['    };','    public static final short[] I={']
    row=[]
    for i in idx:
        row.append(str(i))
        if len(row)==24: lines.append('        '+','.join(row)+','); row=[]
    if row: lines.append('        '+','.join(row)+',')
    lines+=['    };','}']
    out=os.path.join(out_dir,f'{class_name}.java')
    with open(out,'w') as f: f.write('\n'.join(lines))
    print(f'  → {out}: {len(pos_l)} verts, {len(idx)//3} tris')

if __name__=='__main__':
    if len(sys.argv)<3: print(__doc__); sys.exit(1)
    d='app/src/main/java/com/fountainpdl/blockfront'
    os.makedirs(d,exist_ok=True)
    print('Baking OperatorMeshData ...')
    bake(sys.argv[1],'OperatorMeshData',scale=2.0,keep_every=20,out_dir=d)
    print('Baking RuinsMeshData ...')
    bake(sys.argv[2],'RuinsMeshData',scale=18.0,keep_every=14,out_dir=d)
    print('Done. git add -A && git commit -m "Add OBJ mesh data" && git push')
