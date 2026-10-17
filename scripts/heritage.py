#!/usr/bin/env python3
"""Convertit le bloc extra.inheritance fourni, sans inventer de relation."""
import json,sys
from pathlib import Path
src=Path(sys.argv[1] if len(sys.argv)>1 else 'resources/facts-validation.json')
dst=Path(sys.argv[2] if len(sys.argv)>2 else 'resources/facts-validation-inheritance.csv')
lines=['type;target;kind']
for r in json.loads(src.read_text())['extra']['inheritance']:
    for key,kind in [('superclasses','extends'),('interfaces','implements')]:
        for target in r[key]:lines.append(';'.join([r['type'],target,kind]))
dst.write_text('\n'.join(lines)+'\n')
