#!/usr/bin/env python3
"""Enregistre réellement un écran X11 en continu et encode sans couper les scènes.
Les vrais terminaux et fenêtres sont pilotés par RealTerminalDemo.java.
"""
import argparse,json,os,shutil,subprocess,tempfile,time,re,textwrap
from pathlib import Path
from PIL import Image,ImageDraw,ImageFont,ImageChops
import imageio_ffmpeg
ROOT=Path(__file__).resolve().parents[1]

def encode(folder):
    rows=[(float(t),name) for t,name in (line.split('\t') for line in (folder/'frames.tsv').read_text().splitlines())]
    if not rows:raise RuntimeError('Aucune capture écran')
    first=Image.open(folder/rows[0][1]).convert('RGB')
    notes=[(float(t),title,note) for t,title,note in (line.split('\t',2) for line in (folder/'notes.tsv').read_text().splitlines())]
    font=ImageFont.truetype('/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf',20)
    bold=ImageFont.truetype('/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf',16)
    def badge(title):
        for prefix,label in [('Construction','A0 · ENVIRONNEMENT ET CONSTRUCTION'),('A0','A0–A2 · FAITS ET COUPLAGE'),('A1','A1 · COUPLAGE'),('A2','A2 · GRAPHE PONDÉRÉ'),('B1–B2','B1–B2 · DENDROGRAMME'),('B1','B1 · CLUSTERING'),('B3','B3 · COUPE EN MODULES'),('B4','B4 · INTERPRÉTATION'),('Robustesse','ROBUSTESSE · LIGNE DE COMMANDE'),('Déterminisme','B1 · DÉTERMINISME'),('C1','C1 · LIEN SIMPLE'),('C2','C2 · RELATIONS ENRICHIES'),('C3','C3 · INTERFACE GRAPHIQUE'),('C4','C4 · SECOND PROJET'),('Export','C3 · EXPORT ET SYNTHÈSE')]:
            if title.startswith(prefix):return label
        return title.upper()
    def caption(im,t):
        applicable=[row for row in notes if row[0]<=t]
        if not applicable:return im
        _,title,note=applicable[-1]
        draw=ImageDraw.Draw(im)
        code_scene=any(part in title for part in ['Du code','Lire le lien','Départager','lecture de branches','lecture du code'])
        if code_scene:
            crop=im.crop((65,90,1855,970))
            delta=ImageChops.difference(crop,Image.new('RGB',crop.size,'#665018')).convert('L')
            box=delta.point(lambda value:255 if value<2 else 0).getbbox()
            if box and box[2]-box[0]>100 and box[3]-box[1]>30:
                draw.rectangle((box[0]+59,box[1]+84,box[2]+71,box[3]+96),outline='#ffb52b',width=3)
        draw.rectangle((0,994,1920,1080),fill='#101216')
        label=badge(title);width=int(draw.textlength(label,font=bold))+30
        draw.rectangle((18,1011,18+width,1055),fill='#ffb52b')
        draw.text((33,1024),label,font=bold,fill='#111111')
        x=18+width+44;limit=1890-x;lines=[];current=''
        for word in note.split():
            candidate=(current+' '+word).strip()
            if draw.textlength(candidate,font=font)>limit and current:lines.append(current);current=word
            else:current=candidate
        if current:lines.append(current)
        y=1022 if len(lines)==1 else 1009
        for index,line in enumerate(lines[:2]):draw.text((x,y+index*28),line,font=font,fill='#eeeeee')
        return im
    if all(lo==hi==0 for lo,hi in first.getextrema()):raise RuntimeError('Capture noire : utiliser X11/Xvfb')
    temporary=ROOT/'video/demonstration-nouvelle.mp4'
    writer=imageio_ffmpeg.write_frames(str(temporary),first.size,fps=12,codec='libx264',macro_block_size=1,
        output_params=['-crf','20','-preset','fast','-movflags','+faststart'])
    writer.send(None);written=0
    for i,(timestamp,name) in enumerate(rows):
        until=rows[i+1][0] if i+1<len(rows) else timestamp+.5
        count=max(0,round(until*12)-written)
        if count:
            with Image.open(folder/name) as im:data=caption(im.convert('RGB'),timestamp).tobytes()
            for _ in range(count):writer.send(data)
            written+=count
    writer.close();temporary.replace(ROOT/'video/demonstration.mp4')
    chapters=[]
    for line in (folder/'chapitres.tsv').read_text().splitlines():
        seconds,title=line.split('\t',1);v=int(float(seconds));chapters.append(f'{v//60:02d}:{v%60:02d}  {title}')
    (ROOT/'video/chapitres.txt').write_text('\n'.join(chapters)+'\n')
    shutil.copy(folder/'executions.log',ROOT/'video/executions.log')
    raw=(folder/'terminal.typescript').read_text(errors='replace')
    raw=re.sub(r'\x1b\][^\x07]*(?:\x07|\x1b\\)','',raw)
    raw=re.sub(r'\x1b\[[0-?]*[ -/]*[@-~]','',raw)
    raw=re.sub(r'\x1b[()][A-Z0-9]','',raw).replace('\r','')
    (ROOT/'video/terminal.txt').write_text(raw)
    metadata={'duration_seconds':written/12,'resolution':list(first.size),'fps':12,'capture':'Robot, VS Code avec terminal Bash intégré, feh et Swing ; habillage inspiré du TP1','chapters':len(chapters),
        'source_readings':(folder/'executions.log').read_text().count('[source éditeur :'),
        'invalid_input_cases':(folder/'executions.log').read_text().count('[code de sortie : 2]')}
    (ROOT/'video/metadata.json').write_text(json.dumps(metadata,ensure_ascii=False,indent=2)+'\n')
    (ROOT/'video/description.txt').write_text('Capture continue d’un vrai bureau : VS Code avec véritable terminal Bash intégré, feh et fenêtre Swing.\n'
        'Les caractères sont saisis dans le véritable terminal ; Bash exécute les commandes. Aucune console simulée.\n'
        'Habillage inspiré du TP1 : cartouche jaune, explication en bandeau inférieur et encadré jaune autour de la sélection réelle du code.\n'
        'Journal des commandes : executions.log ; transcription du véritable PTY : terminal.txt.\n'
        'Sous-titres intégrés : étapes A0 à B4, choix de CP, interprétation, conventions, bonus C1 à C4 et limites. Sans voix.\n'
        'Quatre lectures des sources réelles : couplage, lien moyen, départage et coupe ; coloration syntaxique et sélection dans VS Code.\n'
        'Trois erreurs provoquées : CP absent, CP mal formé et dossier inexistant, code 2 ; coupe rejetée, code 3.\n'
        'Repères jaunes sur les isolés du graphe et la branche du module principal ; données inchangées.\n'
        'La réextraction TP1 est exécutée pendant la capture. Les images du graphe et du dendrogramme sont régénérées.\n'
        'C4 conserve les appels résolus aux méthodes implicites de records/enum.\n'
        'C3 est inclus dans la vidéo principale, y compris le dialogue d’export. Les interactions sont pilotées.\n'
        'Les frappes et les mouvements de souris sont réellement injectés par Robot, à cadence variable.\n'
        'Les corrections de frappe sont scénarisées ; ce rendu ne prétend pas être une intervention humaine.\n'
        f'Durée : {written/12:.1f} secondes. Voir chapitres.txt et executions.log.\n'
        'Reproduction : python3 video/enregistrer.py --xvfb (Maven, JDK, Graphviz, Xvfb, VS Code, openbox, xdotool, feh et script dans PATH).\n')
    print(json.dumps(metadata,ensure_ascii=False))

def main():
    parser=argparse.ArgumentParser();group=parser.add_mutually_exclusive_group()
    group.add_argument('--display',help='écran X11 existant, de préférence privé')
    group.add_argument('--xvfb',action='store_true',help='créer un écran X11 privé')
    parser.add_argument('--encode-only',type=Path,help='encoder une capture continue déjà terminée')
    args=parser.parse_args()
    if args.encode_only:encode(args.encode_only);return
    env=os.environ.copy();server=None;manager=None
    with tempfile.TemporaryDirectory(prefix='tp2-video-') as tmp:
        work=Path(tmp)
        if args.display:env['DISPLAY']=args.display
        else:
            if not shutil.which('Xvfb'):raise SystemExit('Xvfb absent : installer Xvfb ou fournir --display :N pour un écran X11 disponible.')
            number=next(n for n in range(90,120) if not Path(f'/tmp/.X11-unix/X{n}').exists())
            env['DISPLAY']=f':{number}'
            server=subprocess.Popen(['Xvfb',env['DISPLAY'],'-screen','0','1920x1080x24','-ac','-nolisten','tcp'],env=env,stdout=subprocess.DEVNULL,stderr=subprocess.DEVNULL)
            time.sleep(1)
            if server.poll() is not None:raise RuntimeError('Échec du démarrage Xvfb')
        try:
            for executable in ['openbox','xdotool','feh','script']:
                if not shutil.which(executable):raise RuntimeError('Outil de capture absent : '+executable)
            if not shutil.which(env.get('TP2_CODE','code')):raise RuntimeError('VS Code absent : définir TP2_CODE vers le binaire natif')
            manager=subprocess.Popen(['openbox'],env=env,stdout=subprocess.DEVNULL,stderr=subprocess.DEVNULL)
            time.sleep(1)
            jar=ROOT/'target/hai913i-tp1-analyzer.jar'
            if not jar.exists():subprocess.run(['mvn','-q','-DskipTests','package'],cwd=ROOT,env=env,check=True)
            shutil.copy(jar,work/'engine.jar');classes=work/'classes';classes.mkdir()
            subprocess.run(['javac','--release','17','-encoding','UTF-8','-cp',str(work/'engine.jar'),'-d',str(classes),str(ROOT/'video/RealTerminalDemo.java'),str(ROOT/'video/NaturalInput.java')],env=env,check=True)
            subprocess.run(['java','-cp',str(classes)+os.pathsep+str(work/'engine.jar'),'hai913i.tp2.RealTerminalDemo',str(ROOT),str(work/'captures')],env=env,check=True)
            encode(work/'captures')
        finally:
            if manager is not None and manager.poll() is None:manager.terminate();manager.wait(timeout=10)
            if server is not None:server.terminate();server.wait(timeout=10)

if __name__=='__main__':main()
