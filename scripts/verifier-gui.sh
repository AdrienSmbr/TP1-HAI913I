#!/bin/sh
# Depuis la racine du dépôt. Un écran X11 est nécessaire ; ne fait pas partie des tests sans écran.
set -eu
mkdir -p target/smoke
javac --release 17 -encoding UTF-8 -cp target/hai913i-tp1-analyzer.jar -d target/smoke scripts/gui/GuiSmoke.java scripts/gui/GuiExitSmoke.java
java -cp target/smoke:target/hai913i-tp1-analyzer.jar hai913i.tp2.GuiSmoke target/gui-captures-x11
for cp in 0.025 0.045; do
    expected=0
    [ "$cp" != 0.045 ] || expected=3
    code=0
    java -cp target/smoke:target/hai913i-tp1-analyzer.jar hai913i.tp2.GuiExitSmoke "$cp" > "target/gui-exit-$cp.log" 2>&1 || code=$?
    [ "$code" -eq "$expected" ] || { echo "Code GUI inattendu : $code au lieu de $expected" >&2; exit 1; }
    echo "Fermeture GUI après CP=$cp : code $code conforme."
done
code=0
java -Djava.awt.headless=true -jar target/hai913i-tp1-analyzer.jar resources --cp 0.025 --gui > target/gui-headless.log 2>&1 || code=$?
[ "$code" -eq 2 ] || exit 1
echo 'Mode sans écran : diagnostic et code 2 conformes.'
