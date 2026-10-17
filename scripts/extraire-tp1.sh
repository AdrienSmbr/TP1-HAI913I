#!/bin/sh
# Adaptateur partagé de l'unique projet Maven.
set -eu
if [ "$#" -ne 2 ]; then
    echo 'Usage : sh scripts/extraire-tp1.sh PROJET_JAVA DOSSIER_SORTIE' >&2
    exit 2
fi
mvn -B -Dstyle.color=never -DskipTests package
java -jar target/hai913i-tp1-analyzer.jar export-facts "$1" "$2"
