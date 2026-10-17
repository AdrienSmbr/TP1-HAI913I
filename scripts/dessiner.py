#!/usr/bin/env python3
"""Dessine les exports d'une exécution : seuls les libellés sont abrégés."""
import csv,json,re,subprocess,sys
from pathlib import Path
folder=Path(sys.argv[1]);q=lambda s:json.dumps(s,ensure_ascii=False)
with (folder/'graphe.csv').open() as f:rows=list(csv.DictReader(f,delimiter=';'))
nodes=[r['classA'] for r in rows if r['kind']=='node']
lines=['graph Couplage { node [shape=box, fontname="DejaVu Sans"];']
for name in nodes:lines.append(q(name)+' [label='+q(name.rsplit('.',1)[-1])+'];')
for r in rows:
    if r['kind']=='edge':lines.append(q(r['classA'])+' -- '+q(r['classB'])+' [label='+q(f"{float(r['weight']):.4f}")+'];')
lines.append('}');(folder/'graphe-image.dot').write_text('\n'.join(lines))
s=(folder/'dendrogramme.dot').read_text()
s=re.sub(r'label="([^"#]+)"',lambda m:'label='+q(m[1].rsplit('.',1)[-1]),s)
(folder/'dendrogramme-image.dot').write_text(s)
for name in ['graphe','dendrogramme']:
    subprocess.run(['dot','-Tpng',str(folder/(name+'-image.dot')),'-o',str(folder/(name+'.png'))],check=True)
print('Images réellement reconstruites depuis',folder/'graphe.csv','et',folder/'dendrogramme.dot')
