from pathlib import Path
import csv,subprocess,os
from PIL import Image,ImageDraw,ImageFont
r=Path(__file__).resolve().parents[1];f=r/'sorties/figures'
read=lambda path:list(csv.DictReader(path.open(),delimiter=';'))
classes=sorted(x['class'] for x in read(r/'source/resources/facts-validation-classes.csv'))
merges=read(r/'sorties/cp-0.025/fusions.csv'); modules=read(r/'sorties/cp-0.025/modules.csv')
W,H=1500,1020
im=Image.new('RGB',(W,H),'white'); d=ImageDraw.Draw(im)
font=lambda n:ImageFont.truetype('/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf',n)
d.text((30,15),'Dendrogramme — lien moyen, ordre des fusions',font=font(28),fill='#183047')
d.text((30,58),'La distance horizontale représente l’étape, pas la similarité.',font=font(20),fill='#456070')
# Ordre des feuilles induit par l'arbre, évite les croisements.
children={int(x['id']):(int(x['left']),int(x['right'])) for x in merges}
def leaves(i):return [i] if i<len(classes) else leaves(children[i][0])+leaves(children[i][1])
order=leaves(28);positions={idx:(400,140+i*43) for i,idx in enumerate(order)}
colors=['#007f88','#56789a','#a06543','#645888','#209044','#bc6556','#3e629f']
assignment={c:int(m['module'])-1 for m in modules for c in m['classes'].split('|')}
for i,(x,y) in positions.items():
 d.text((20,y-12),classes[i].rsplit('.',1)[-1],font=font(21),fill=colors[assignment[classes[i]]])
for row in merges:
 step=int(row['step']);i=int(row['id']);a,b=children[i];x=400+step*55
 xa,ya=positions[a];xb,yb=positions[b];y=(ya+yb)/2
 d.line([(xa,ya),(x,ya),(x,yb),(xb,yb)],fill='#416176',width=3)
 d.ellipse((x-4,y-4,x+4,y+4),fill='#17334b');positions[i]=(x,y)
 # Similarités rangées sous l'arbre.
 xx=420+(step-1)*74
 d.text((xx,835),'#'+str(i),font=font(15),fill='#17334b')
 d.text((xx,860),str(step),font=font(17),fill='#17334b')
 d.text((xx,887),f"{float(row['similarity']):.4f}",font=font(14),fill='#17334b')
d.text((20,835),'Identifiant',font=font(17),fill='#17334b')
d.text((20,860),'Étape',font=font(17),fill='#17334b')
d.text((20,887),'Similarité réelle',font=font(17),fill='#17334b')
d.text((25,949),'Couleurs des feuilles : modules retenus à CP = 0,025. Les deux dernières fusions valent zéro.',font=font(21),fill='#456070')
im.save(f/'dendrogramme-etapes.png')
# Vue architecturale : agrégation des 19 arêtes entre les sept modules.
assign={c:int(m['module']) for m in modules for c in m['classes'].split('|')}
weights={}
for edge in read(r/'sorties/cp-0.025/graphe.csv'):
 if edge['kind']!='edge':continue
 a,b=sorted([assign[edge['classA']],assign[edge['classB']]])
 if a!=b:weights[a,b]=weights.get((a,b),0)+float(edge['weight'])
lines=['graph Vue { graph [rankdir=LR]; node [shape=box,style="rounded,filled",fillcolor="#eef5fa",fontname="DejaVu Sans"];']
for m in modules:
 names='\\n'.join(c.rsplit('.',1)[-1] for c in m['classes'].split('|'))
 lines.append('m'+m['module']+' [label="Module '+m['module']+' (n='+m['size']+')\\n'+names+'"];')
for (a,b),w in weights.items():lines.append(f'm{a} -- m{b} [label="{w:.4f}"];')
lines.append('}')
p=f/'vue-modules.dot';p.write_text('\n'.join(lines))
env=os.environ.copy()
for fmt in ['png','svg','pdf']:subprocess.run(['dot','-T'+fmt,str(p),'-o',str(p.with_suffix('.'+fmt))],env=env,check=True)
