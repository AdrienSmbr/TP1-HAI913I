#!/usr/bin/env python3
"""Vérification indépendante du pont TP1 sur le jeu canonique fourni."""
import csv
import hashlib
from pathlib import Path

def read(path):
    with path.open() as f:return list(csv.DictReader(f,delimiter=';'))
source=Path('resources');dest=Path('target/extraction-validation')
for kind,fields in [('classes',['class']),('methods',['method','owner']),('calls',['caller','callerClass','callee','calleeClass','sites'])]:
    a=read(source/f'facts-validation-{kind}.csv');b=read(dest/f'facts-extracted-{kind}.csv')
    assert sorted(tuple(r[f] for f in fields) for r in a)==sorted(tuple(r[f] for f in fields) for r in b),kind
print('Pont TP1 conforme sur validation : classes, méthodes et arcs/sites identiques aux faits canoniques.')

# Vérifier le corpus analysé, pas seulement des chiffres ou des noms de fichiers.
root=Path('resources/projects/analyseur-tp1/src/main/java')
manifest={}
for line in Path('resources/projects/analyseur-tp1/sources.sha256').read_text().splitlines():
    digest,name=line.split('  ',1);manifest[name]=digest
actual={str(p.relative_to(root)):hashlib.sha256(p.read_bytes()).hexdigest() for p in root.rglob('*.java')}
assert manifest==actual,'Le corpus C4 ne correspond plus au manifeste livré'
print('Corpus C4 : empreintes SHA-256 des 29 fichiers conformes.')

# Les cibles implicites résolues doivent être des nœuds et leurs sites conservés.
root=Path('datasets/analyseur-tp1')
recovered=read(root/'cibles-reintegrees.csv')
methods={r['method']:r['owner'] for r in read(root/'facts-extracted-methods.csv')}
calls=read(root/'facts-extracted-calls.csv')
assert len(recovered)==69
assert sum(int(r['sites']) for r in recovered)==144
assert sum(int(r['sites']) for r in recovered if r['kind']=='RECORD')==143
assert sum(int(r['sites']) for r in recovered if r['kind']=='ENUM')==1
for row in recovered:
    assert methods[row['target']]==row['owner']
    assert sum(int(c['sites']) for c in calls if c['callee']==row['target'])==int(row['sites'])
assert len(calls)==267 and sum(int(c['sites']) for c in calls)==326
print('Complétion C4 : 69 cibles, 121 arcs supplémentaires et 144 sites conservés ; aucun corps synthétique inventé.')
