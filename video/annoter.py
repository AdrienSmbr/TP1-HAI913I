#!/usr/bin/env python3
"""Met en évidence des résultats réels, sans modifier les poids ni la topologie."""
import csv,json,subprocess,sys
from pathlib import Path
folder=Path(sys.argv[1])
modules=list(csv.DictReader((folder/'modules.csv').open(),delimiter=';'))
main=max(modules,key=lambda row:int(row['size']))
def draw(name,annotation):
    original=(folder/(name+'-image.dot')).read_text()
    position=original.rfind('}')
    output=folder/(name+'-focus-image.dot')
    output.write_text(original[:position]+annotation+'\n'+original[position:])
    subprocess.run(['dot','-Tpng',str(output),'-o',str(folder/(name+'-focus.png'))],check=True)
q=lambda text:json.dumps(text,ensure_ascii=False)
draw('graphe','\n'.join(q(name)+' [style=filled, fillcolor="#ffe28a", color="#ad7400", penwidth=3];' for name in ['library.model.Dvd','library.service.LoanException'])+'\nlabel="En jaune : les deux classes isolées ; aucun lien ajouté"; labelloc=t;')
node='n'+main['node']
draw('dendrogramme',f'{node} [style=filled, fillcolor="#ffe28a", color="#ad7400", penwidth=3];\nlabel="En jaune : branche retenue de '+main['size']+' classes, moyenne interne = '+f"{float(main['internalMean']):.6f}"+' > CP = '+main['cp']+'\\nLa similarité de fusion reste celle inscrite dans le nœud"; labelloc=t;')
print('Repères jaunes : isolés du graphe et branche du module principal ; valeurs et topologie inchangées.')
