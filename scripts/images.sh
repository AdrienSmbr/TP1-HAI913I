#!/bin/sh
# Graphviz optionnel : le programme Java n'en dépend pas.
set -eu
folder=${1:-sorties/cp-0.025}
for name in graphe dendrogramme modules; do
    dot -Tsvg "$folder/$name.dot" -o "$folder/$name.svg"
    dot -Tpng "$folder/$name.dot" -o "$folder/$name.png"
done
