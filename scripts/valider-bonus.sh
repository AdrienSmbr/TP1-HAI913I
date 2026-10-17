#!/bin/sh
# Depuis la racine du dépôt. Vérifie aussi les obligatoires avant les variantes.
set -eu
sh scripts/valider.sh
run_expected() {
    expected=$1
    shift
    code=0
    "$@" || code=$?
    [ "$code" -eq "$expected" ] || { echo "Code inattendu $code / $expected" >&2; exit 1; }
}
jar=target/hai913i-tp1-analyzer.jar
run_expected 0 java -jar "$jar" resources --cp 0.025 --linkage single --out target/bonus/c1
run_expected 3 java -jar "$jar" resources --cp 0.025 --inheritance resources/facts-validation-inheritance.csv --out target/bonus/c2-025
run_expected 0 java -jar "$jar" resources --cp 0.020 --inheritance resources/facts-validation-inheritance.csv --out target/bonus/c2-020
run_expected 0 java -jar "$jar" datasets/analyseur-tp1 --cp 0.010 --out target/bonus/c4-010
run_expected 3 java -jar "$jar" datasets/analyseur-tp1 --cp 0.025 --out target/bonus/c4-025
# Rejouer réellement l'extraction, pas seulement recharger une table préexistante.
sh scripts/extraire-tp1.sh resources/projects/analyseur-tp1 target/reextraction
for kind in classes methods calls; do
    cmp "datasets/analyseur-tp1/facts-extracted-$kind.csv" "target/reextraction/facts-extracted-$kind.csv"
done
java -jar target/hai913i-tp1-analyzer.jar export-facts resources/validation target/extraction-validation
python3 scripts/verifier-extraction.py
python3 scripts/verifier-oracle.py
echo 'BONUS VALIDÉS : C1, C2, C4 et invariants du modèle graphique. Le test de fenêtre C3 exige un écran.'
