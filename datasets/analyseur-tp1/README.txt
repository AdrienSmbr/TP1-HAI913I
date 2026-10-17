Second projet C4 : analyseur Java du TP1 fourni par le binôme.
Corpus : bonus/tp1-extracteur/src/main/java ; empreintes : bonus/tp1-sources.sha256.
Faits reconstruits réellement par JdtParser, FactExtractor et CallGraphBuilder du TP1.
Pont : bonus/ExportTp2.java ; commande : sh scripts/extraire-tp1.sh bonus/tp1-extracteur target/reextraction
Format CSV canonique, préfixe facts-extracted ; aucun poids ou regroupement précalculé dans ces faits.
30 types, 199 méthodes, 267 arcs, 326 sites internes. Le pont complète 69 méthodes cibles implicites résolues (records/enum), soit 121 arcs et 144 sites réintégrés.
Détail : cibles-reintegrees.csv. Aucun corps synthétique ni constructeur n’est reconstruit.
Voir extraction.txt pour les diagnostics et le rapport pour leur interprétation.
