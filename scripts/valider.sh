#!/bin/sh
# Depuis la racine du dépôt ; un rejet de coupe attendu (code 3) est un succès du protocole.
set -eu
mvn -B -Dstyle.color=never clean verify
run_expected() {
    expected=$1
    shift
    code=0
    "$@" || code=$?
    if [ "$code" -ne "$expected" ]; then
        echo "Code inattendu : $code au lieu de $expected" >&2
        exit 1
    fi
}
jar=target/hai913i-tp1-analyzer.jar
run_expected 0 java -jar "$jar" examples --cp 0.20 --out target/validation/mini
run_expected 3 java -jar "$jar" examples --cp 0.50 --out target/validation/mini-rejet
run_expected 0 java -jar "$jar" resources --cp 0.015 --out target/validation/cp-0.015
run_expected 0 java -jar "$jar" resources --cp 0.025 --out target/validation/cp-0.025
run_expected 3 java -jar "$jar" resources --cp 0.045 --out target/validation/cp-0.045
run_expected 0 java -jar "$jar" resources --cp 0.025 --out target/validation/repetition
for file in target/validation/cp-0.025/*; do
    cmp "$file" "target/validation/repetition/$(basename "$file")"
done
run_expected 0 java -jar "$jar" --graph target/validation/cp-0.025/graphe.csv --cp 0.025 --out target/validation/recharge
cmp target/validation/cp-0.025/modules.csv target/validation/recharge/modules.csv
run_expected 2 java -jar "$jar" inexistant --cp 0.025
run_expected 2 java -jar "$jar" resources --cp NaN
echo 'VALIDATION TERMINÉE : contrôles, rejets attendus, rechargement et déterminisme conformes.'
