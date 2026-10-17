#!/usr/bin/env python3
"""Oracle indépendant en fractions exactes, sans appeler les classes Java du moteur.
Exécuter depuis la racine du dépôt après mvn package. Aucun paquet Python externe requis.
"""
import csv,json,random,subprocess,tempfile,os
from fractions import Fraction as F
from itertools import combinations
from pathlib import Path

os.chdir(Path(__file__).resolve().parents[1])
EPS=F(1,10**9)
def read(path):
    with path.open() as f:return list(csv.DictReader(f,delimiter=';'))
def oracle(folder,prefix,cp,linkage,relations=None):
    names=sorted(x['class'] for x in read(folder/(prefix+'-classes.csv')))
    counts={};inter=0
    # Les propriétaires sont lus dans la table des méthodes, pas déduits des appels.
    owners={r['method']:r['owner'] for r in read(folder/(prefix+'-methods.csv'))}
    for row in read(folder/(prefix+'-calls.csv')):
        a,b=owners[row['caller']],owners[row['callee']]
        if a!=b:
            pair=tuple(sorted([a,b]));counts[pair]=counts.get(pair,0)+1;inter+=1
    if relations:
        for row in read(relations):
            if row['target'] in names:
                pair=tuple(sorted([row['type'],row['target']]));counts[pair]=counts.get(pair,0)+1;inter+=1
    weights={k:F(v,inter) for k,v in counts.items()}
    def weight(a,b):return weights.get(tuple(sorted([a,b])),F(0))
    def mean(members):
        pairs=list(combinations(members,2));return sum((weight(a,b) for a,b in pairs),F(0))/len(pairs) if pairs else F(0)
    groups={i:(c,) for i,c in enumerate(names)};children={};trace=[];all_groups=dict(groups)
    while len(groups)>1:
        candidates={}
        for i,j in combinations(sorted(groups),2):
            values=[weight(a,b) for a in groups[i] for b in groups[j]]
            candidates[i,j]=max(values) if linkage=='single' else sum(values,F(0))/len(values)
        maximum=max(candidates.values());pair=min(k for k,v in candidates.items() if maximum-v<=EPS)
        i,j=pair;new=len(names)+len(trace);merged=tuple(sorted(groups.pop(i)+groups.pop(j)))
        groups[new]=merged;all_groups[new]=merged;children[new]=(i,j)
        trace.append((new,i,j,candidates[pair],merged))
    root=next(iter(groups));modules=[]
    def cut(node):
        if node not in children or mean(all_groups[node])>F(cp):modules.append(node)
        else:
            for child in children[node]:cut(child)
    cut(root);modules.sort(key=lambda n:all_groups[n][0])
    return names,weights,trace,[(n,all_groups[n],mean(all_groups[n])) for n in modules]

def check(folder,prefix,cp,linkage,out,relations=None):
    names,weights,trace,modules=oracle(folder,prefix,cp,linkage,relations)
    command=['java','-jar','target/hai913i-tp1-analyzer.jar',str(folder),'--prefix',prefix,'--cp',cp,'--linkage',linkage,'--out',str(out)]
    if relations:command+=['--inheritance',str(relations)]
    process=subprocess.run(command,capture_output=True,text=True)
    expected=0 if len(modules)<=len(names)//2 else 3
    assert process.returncode==expected,(command,process.stderr)
    graph=read(out/'graphe.csv');assert sorted(r['classA'] for r in graph if r['kind']=='node')==names
    edges={tuple(sorted([r['classA'],r['classB']])):float(r['weight']) for r in graph if r['kind']=='edge'}
    assert edges.keys()==weights.keys()
    for pair,v in weights.items():assert abs(edges[pair]-float(v))<1e-12
    actual=read(out/'fusions.csv');assert len(actual)==len(trace)
    for row,(idx,i,j,value,members) in zip(actual,trace):
        assert (int(row['id']),int(row['left']),int(row['right']))==(idx,i,j)
        assert abs(float(row['similarity'])-float(value))<1e-12
        assert tuple(row['members'].split('|'))==members
    actual=read(out/'modules.csv');assert len(actual)==len(modules)
    for row,(idx,members,mean) in zip(actual,modules):
        assert int(row['node'])==idx and tuple(row['classes'].split('|'))==members
        if len(members)>1:assert abs(float(row['internalMean'])-float(mean))<1e-12
        else:assert row['internalMean']=='NA'

def random_facts(folder,seed):
    rng=random.Random(seed);n=rng.randint(2,9);names=[f'random{seed}.C{i}' for i in range(n)]
    classes=[[c,c.rsplit('.',1)[-1],f'random{seed}'] for c in names]
    methods=[[c+f'#m{k}()',c] for c in names for k in range(2)]
    arcs=[]
    for (a,owner_a),(b,owner_b) in combinations(methods,2):
        if rng.random()<.18:arcs.append([a,owner_a,b,owner_b,str(rng.randint(1,5))])
        if rng.random()<.12:arcs.append([b,owner_b,a,owner_a,str(rng.randint(1,5))])
    # Deux systèmes sans appel interclasse, pour éprouver les fusions à zéro.
    if seed%7==0:arcs=[]
    for suffix,header,data in [('classes',['class','simpleName','package'],classes),('methods',['method','owner'],methods),('calls',['caller','callerClass','callee','calleeClass','sites'],arcs)]:
        rng.shuffle(data)
        with (folder/f'facts-{suffix}.csv').open('w') as f:
            w=csv.writer(f,delimiter=';',lineterminator='\n');w.writerow(header);w.writerows(data)

runs=0
with tempfile.TemporaryDirectory(prefix='tp2-oracle-') as temp:
    base=Path(temp)
    for folder,prefix,thresholds in [(Path('resources'),'facts-validation',['0.015','0.025','0.045']),
          (Path('examples'),'facts-mini',['0.20','0.50']),
          (Path('datasets/analyseur-tp1'),'facts-extracted',['0.010','0.025'])]:
        for linkage in ['average','single']:
            for cp in thresholds:
                check(folder,prefix,cp,linkage,base/f'run-{runs}');runs+=1
    for cp in ['0.020','0.025']:
        check(Path('resources'),'facts-validation',cp,'average',base/f'run-{runs}',Path('resources/facts-validation-inheritance.csv'));runs+=1
    for seed in range(16):
        folder=base/f'input-{seed}';folder.mkdir();random_facts(folder,seed)
        for linkage in ['average','single']:
            for cp in ['0','0.025','0.20']:
                check(folder,'facts',cp,linkage,base/f'run-{runs}');runs+=1
summary={'status':'PASS','executions':runs,'random_systems':16,'arithmetic':'fractions exactes Python',
         'checks':['couplages','normalisation','identifiants et départage','fusions','membres','coupe et codes 0/3']}
Path('target/oracle-validation.json').write_text(json.dumps(summary,ensure_ascii=False,indent=2)+'\n')
print(json.dumps(summary,ensure_ascii=False))
