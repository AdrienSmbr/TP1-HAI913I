#!/usr/bin/env python3
"""Restitution comparative déterministe depuis les seuls exports calculés."""
import csv,json
from pathlib import Path
r=Path(__file__).resolve().parents[1]
def read(path):
    with path.open() as f:return list(csv.DictReader(f,delimiter=';'))
labels=['cp-0.025','c1-lien-simple','c2-relations-enrichies','c2-cp-0.020','c4-cp-0.010','c4-cp-0.025']
summary={}
for label in labels:
    p=r/'sorties'/label;ms=read(p/'modules.csv');g=read(p/'graphe.csv');f=read(p/'fusions.csv')
    summary[label]={'classes':sum(x['kind']=='node' for x in g),'edges':sum(x['kind']=='edge' for x in g),
        'merges':len(f),'modules':len(ms),'sizes':[int(m['size']) for m in ms],'accepted':ms[0]['accepted']=='true',
        'cp':float(ms[0]['cp'])}
a=read(r/'sorties/cp-0.025/fusions.csv');b=read(r/'sorties/c1-lien-simple/fusions.csv')
for x,y in zip(a,b):
    if x['members']!=y['members'] or abs(float(x['similarity'])-float(y['similarity']))>1e-9:
        summary['first_divergence']={'step':int(x['step']),'average':x,'single':y};break
(r/'sorties/bonus-comparaison.json').write_text(json.dumps(summary,ensure_ascii=False,indent=2)+'\n')
print(json.dumps(summary,ensure_ascii=False,indent=2))
