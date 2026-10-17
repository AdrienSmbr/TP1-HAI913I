#!/usr/bin/env python3
"""Encode la capture Robot de la fenêtre Swing ; aucune reconstruction de l'interface."""
import sys
from pathlib import Path
from PIL import Image
import imageio_ffmpeg
root=Path(__file__).resolve().parents[1]
folder=Path(sys.argv[1]) if len(sys.argv)>1 else root/'target/gui-captures-x11'
images=sorted(folder.glob('frame-*.png'))
if not images:raise SystemExit('Aucune capture Robot ; lancer bonus/GuiSmoke.java avec un écran.')
with Image.open(images[0]) as im:size=im.size
writer=imageio_ffmpeg.write_frames(str(root/'video/interface-bonus.mp4'),size,fps=4,codec='libx264',output_params=['-crf','20','-preset','fast','-movflags','+faststart'])
writer.send(None)
for path in images:
    with Image.open(path) as im:writer.send(im.convert('RGB').tobytes())
writer.close()
print('Capture réelle de la fenêtre Swing :',len(images)/4,'s')
