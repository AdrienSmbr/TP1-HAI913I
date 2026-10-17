#!/usr/bin/env python3
"""Génère les deux PDF depuis les Markdown et les sorties calculées."""
from pathlib import Path
import csv, re, html
from reportlab.pdfgen.canvas import Canvas
from reportlab.platypus import (BaseDocTemplate, PageTemplate, Frame, Paragraph, Spacer,
    PageBreak, Table, TableStyle, Image, Preformatted, KeepTogether)
from reportlab.platypus.tableofcontents import TableOfContents
from reportlab.lib.styles import getSampleStyleSheet, ParagraphStyle
from reportlab.lib.enums import TA_CENTER
from reportlab.lib import colors
from reportlab.lib.pagesizes import A4
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont

DOCS=Path(__file__).resolve().parent
ROOT=DOCS.parents[1]
FONT=Path('/usr/share/fonts/truetype/dejavu')
for name,file in [('DV','DejaVuSans.ttf'),('DV-Bold','DejaVuSans-Bold.ttf'),('DVM','DejaVuSansMono.ttf')]:
    pdfmetrics.registerFont(TTFont(name,str(FONT/file)))
pdfmetrics.registerFontFamily('DV',normal='DV',bold='DV-Bold',italic='DV',boldItalic='DV-Bold')
S=getSampleStyleSheet()
S.add(ParagraphStyle(name='Body',fontName='DV',fontSize=9.2,leading=14,spaceAfter=8))
S.add(ParagraphStyle(name='H1tp',fontName='DV-Bold',fontSize=15,leading=20,spaceBefore=17,spaceAfter=10,keepWithNext=True,textColor=colors.HexColor('#17374d')))
S.add(ParagraphStyle(name='H2tp',fontName='DV-Bold',fontSize=11,leading=16,spaceBefore=12,spaceAfter=7,keepWithNext=True,textColor=colors.HexColor('#235a70')))
S.add(ParagraphStyle(name='H3tp',fontName='DV-Bold',fontSize=10.5,leading=15,spaceBefore=10,spaceAfter=6,keepWithNext=True,textColor=colors.HexColor('#235a70')))
S.add(ParagraphStyle(name='CodeTp',fontName='DVM',fontSize=7,leading=10,spaceBefore=6,spaceAfter=10,backColor=colors.HexColor('#f0f4f7'),borderPadding=7))
S.add(ParagraphStyle(name='CellTp',fontName='DV',fontSize=8,leading=11,spaceAfter=0))
S.add(ParagraphStyle(name='CaptionTp',fontName='DV',fontSize=8,leading=11,spaceAfter=12,alignment=TA_CENTER,textColor=colors.HexColor('#415a6b')))
S.add(ParagraphStyle(name='CoverTp',fontName='DV-Bold',fontSize=25,leading=34,alignment=TA_CENTER,textColor=colors.HexColor('#17374d'),spaceAfter=14))
S.add(ParagraphStyle(name='CenterTp',fontName='DV',fontSize=11,leading=18,alignment=TA_CENTER,spaceAfter=10))
WIDTH=A4[0]-94

def esc(s):return html.escape(s,quote=False)
def para(s,sty='Body'):
    return Paragraph(esc(s).replace('\n',' '),S[sty])
def table(rows, widths=None):
    t=Table([[para(str(v),'CellTp') for v in row] for row in rows],colWidths=widths,repeatRows=1,hAlign='LEFT')
    t.setStyle(TableStyle([
      ('BACKGROUND',(0,0),(-1,0),colors.HexColor('#dce9f1')),
      ('VALIGN',(0,0),(-1,-1),'TOP'),('LEFTPADDING',(0,0),(-1,-1),7),
      ('RIGHTPADDING',(0,0),(-1,-1),7),('TOPPADDING',(0,0),(-1,-1),6),('BOTTOMPADDING',(0,0),(-1,-1),6),
      ('ROWBACKGROUNDS',(0,1),(-1,-1),[colors.white,colors.HexColor('#f4f7f9')]),
      ('LINEBELOW',(0,0),(-1,0),.6,colors.HexColor('#80a1b5'))]))
    return [t,Spacer(1,12)]

def rows(path):return list(csv.DictReader(path.open(),delimiter=';'))
def computed(marker):
    if marker=='FUSIONS':
      rs=[['Étape','Couple → groupe','Similarité','Moyenne interne']]
      for r in rows(ROOT/'sorties/cp-0.025/fusions.csv'):
       rs.append([r['step'],f"({r['left']}, {r['right']}) → {r['id']}",f"{float(r['similarity']):.10f}",f"{float(r['internalMean']):.10f}"])
      return table(rs,[45,130,160,WIDTH-335])
    if marker=='SEUILS':
      rs=[['CP','Tailles des modules','Nombre / maximum','Statut']]
      for cp in ['0.015','0.025','0.045']:
       ms=rows(ROOT/f'sorties/cp-{cp}/modules.csv')
       rs.append([cp,', '.join(m['size'] for m in ms),str(len(ms))+' / 7','Acceptée' if ms[0]['accepted']=='true' else 'Rejetée'])
      return table(rs,[50,210,120,WIDTH-380])
    if marker=='MODULES':
      rs=[['Module / branche','Classes (noms courts)','Taille','Cbar']]
      for m in rows(ROOT/'sorties/cp-0.025/modules.csv'):
       rs.append([m['module']+' / #'+m['node'],', '.join(c.rsplit('.',1)[-1] for c in m['classes'].split('|')),m['size'],'NA' if m['internalMean']=='NA' else f"{float(m['internalMean']):.10f}"])
      return table(rs,[85,220,40,WIDTH-345])
    if marker=='CONTROLES':
      return table([
        ['Étape','Attendu et obtenu','Vérification'],
        ['A0','15/59/44 et micro 3/5/4','Compte des faits chargés'],
        ['A1','7 internes, 37 interclasses, somme 1','Formules et invariants de matrice'],
        ['A2','15 nœuds, 19 arêtes, 2 isolés','Export puis rechargement sans perte'],
        ['B1','14 fusions ; 4/37, 3/37, 9/148 au départ','Partition active, décroissance, répétition'],
        ['B2','15 feuilles + 14 nœuds internes','Unions disjointes, racine complète'],
        ['B3','4 / 7 / 10 modules ; accepté / accepté / rejeté','Branches, partition, seuil strict et plafond']
      ],[45,210,WIDTH-255])
    raise ValueError(marker)

class Doc(BaseDocTemplate):
    def __init__(self,path,title):
        super().__init__(str(path),pagesize=A4,leftMargin=47,rightMargin=47,topMargin=48,bottomMargin=47,
            title=title,author='Adrien Sambarrey ; Pierre Prevost-Avenel',pageCompression=1)
        self.doc_title=title;self.heading_count=0
        frame=Frame(self.leftMargin,self.bottomMargin,self.width,self.height,id='normal',leftPadding=0,rightPadding=0,topPadding=0,bottomPadding=0)
        self.addPageTemplates(PageTemplate(id='main',frames=frame,onPage=self.page))
    def beforeDocument(self):self.heading_count=0
    def page(self,canvas,doc):
        if doc.page==1:return
        canvas.saveState();canvas.setFont('DV',8);canvas.setFillColor(colors.HexColor('#536f81'))
        canvas.drawString(47,A4[1]-28,'HAI913I · TP2 — '+self.doc_title)
        canvas.drawRightString(A4[0]-47,27,str(doc.page));canvas.restoreState()
    def afterFlowable(self,f):
        if isinstance(f,Paragraph) and f.style.name in ['H1tp','H2tp','H3tp'] and f.getPlainText() != 'Table des matières':
            text=f.getPlainText();key='h'+str(self.heading_count);self.heading_count+=1
            self.canv.bookmarkPage(key);level={'H1tp':0,'H2tp':1,'H3tp':2}[f.style.name]
            self.notify('TOCEntry',(level,text,self.page,key))
            self.canv.addOutlineEntry(text,key,level,False)

def cover(kind):
    return [Spacer(1,35),para('Université de Montpellier','CenterTp'),para('Faculté des Sciences','CenterTp'),Spacer(1,32),
     para('TP 2 — Compréhension et architectures','CoverTp'),para(kind,'CenterTp'),
     para('HAI913I — Évolution et restructuration des logiciels','CenterTp'),Spacer(1,38),
     para('Adrien SAMBARREY','CenterTp'),para('Numéro étudiant : 22510228 — Parcours GL','CenterTp'),Spacer(1,12),
     para('Pierre PREVOST-AVENEL','CenterTp'),para('Numéro étudiant : 22205821 — Parcours GL','CenterTp'),Spacer(1,38),
     para('Enseignant responsable : Bachar Rima','CenterTp'),para('Année universitaire 2026–2027','CenterTp'),
     para('16 octobre 2026','CenterTp'),PageBreak()]

def parse(path):
    text=path.read_text();lines=text.splitlines();result=[];i=0
    while i<len(lines):
        line=lines[i].strip()
        if not line:i+=1;continue
        if line.startswith('```'):
            block=[];i+=1
            while i<len(lines) and not lines[i].startswith('```'):block.append(lines[i]);i+=1
            import textwrap
            wrapped=[]
            for x in block:wrapped+=textwrap.wrap(x,width=103,expand_tabs=False,replace_whitespace=False,drop_whitespace=False,subsequent_indent='    ') or ['']
            result.append(Preformatted('\n'.join(wrapped),S['CodeTp']));i+=1;continue
        if line.startswith('{{'):
            result+=computed(line.strip('{}'));i+=1;continue
        if line.startswith('!['):
            m=re.match(r'!\[(.*?)\]\((.*?)\)',line)
            target=(path.parent/m[2]).resolve()
            image=Image(str(target));ratio=min(WIDTH/image.imageWidth,340/image.imageHeight)
            image.drawWidth=image.imageWidth*ratio;image.drawHeight=image.imageHeight*ratio
            result.append(KeepTogether([Spacer(1,8),image,para(m[1],'CaptionTp')]));i+=1;continue
        if line.startswith('|'):
            rs=[]
            while i<len(lines) and lines[i].strip().startswith('|'):
                v=[s.strip() for s in lines[i].strip().strip('|').split('|')]
                if not all(re.fullmatch(r'[-: ]+',s) for s in v):rs.append(v)
                i+=1
            cols=len(rs[0]);result+=table(rs,[WIDTH/cols]*cols);continue
        if line.startswith('### '):result.append(para(line[4:],'H3tp'));i+=1;continue
        if line.startswith('# '):result.append(para(line[2:],'H1tp'));i+=1;continue
        if line.startswith('## '):result.append(para(line[3:],'H2tp'));i+=1;continue
        block=[line];i+=1
        while i<len(lines) and lines[i].strip() and not lines[i].startswith(('#','```','|','![','{{')):
            block.append(lines[i].strip());i+=1
        result.append(para(' '.join(block)))
    return result

for md,pdf,title in [('Compte-Rendu.md','Compte-Rendu.pdf',"Rapport d'ingénierie"),('README.md','README.pdf','Construction, exécution et validation')]:
    doc=Doc(DOCS/pdf,title)
    toc=TableOfContents();toc.levelStyles=[ParagraphStyle('Toc0',fontName='DV',fontSize=9,leading=14,spaceBefore=5),ParagraphStyle('Toc1',fontName='DV',fontSize=8,leading=12,leftIndent=12),ParagraphStyle('Toc2',fontName='DV',fontSize=8,leading=11,leftIndent=24)]
    story=cover(title)+[para('Table des matières','H1tp'),toc,PageBreak()]+parse(DOCS/md)
    doc.multiBuild(story)
    print(DOCS/pdf)
