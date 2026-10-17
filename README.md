# HAI913I — Analyse statique et architectures (TP1 + TP2)

Adrien SAMBARREY et Pierre PREVOST-AVENEL — Université de Montpellier, 2026–2027.

Un seul projet Maven transforme les **sources Java → faits → métriques / graphe d’appel → couplage → dendrogramme → modules candidats**. L’analyseur TP1 demeure utilisable ; le TP2 prolonge son modèle sans dupliquer l’extracteur ni imposer un aller-retour CSV.

## Construire

Java **17 minimum**, Maven **3.9.x**. Depuis la racine :

```bash
mvn clean package
```

Un seul JAR exécutable avec ses dépendances : `target/hai913i-tp1-analyzer.jar` (nom historique conservé). JDT 3.46.0, Spoon 11.5.0, SLF4J 2.0.18 ; JUnit 5.14.4 pour les tests. Graphviz sert uniquement aux images ; Python et les outils de bureau servent à la documentation et aux vidéos.

## Ligne de commande commune

```bash
# TP1 : faits, métriques et graphe d’appel (les anciennes commandes sans « tp1 » fonctionnent aussi)
java -jar target/hai913i-tp1-analyzer.jar tp1 resources/validation --action all --x 5

# TP2 : jeu canonique fourni, seuil obligatoire
java -jar target/hai913i-tp1-analyzer.jar tp2 resources --cp 0.025 --out target/modules

# Chaîne intégrée : extraction JDT du TP1 puis analyse TP2, sans CSV intermédiaire
java -jar target/hai913i-tp1-analyzer.jar tp2 --sources resources/validation --cp 0.025 --out target/modules-sources

# Lien simple (C1), héritage/implémentation (C2), second projet figé (C4)
java -jar target/hai913i-tp1-analyzer.jar tp2 resources --cp 0.025 --linkage single --out target/single
java -jar target/hai913i-tp1-analyzer.jar tp2 resources --cp 0.020 --inheritance resources/facts-validation-inheritance.csv --out target/enrichi
java -jar target/hai913i-tp1-analyzer.jar tp2 --sources resources/projects/analyseur-tp1 --cp 0.010 --out target/second-projet

# Export de faits réutilisable et rechargement du graphe pondéré
java -jar target/hai913i-tp1-analyzer.jar export-facts resources/validation target/faits
java -jar target/hai913i-tp1-analyzer.jar tp2 target/faits --cp 0.025 --out target/relecture
java -jar target/hai913i-tp1-analyzer.jar tp2 --graph target/modules/graphe.csv --cp 0.025 --out target/graphe-recharge
```

Pour TP2 : **0** coupe acceptée, **2** entrée/extraction invalide, **3** coupe rejetée par le plafond M/2 (exports conservés). Les anciens codes TP1 sont décrits dans [sa documentation](doc/README-TP1.md). Ne pas confondre le seuil entier X des métriques et le seuil décimal CP de la coupe.

Le jeu canonique contient 15 classes, 59 méthodes, 44 arcs, dont 37 interclasses : 19 arêtes et 14 fusions. CP=0,015 / 0,025 / 0,045 donnent respectivement 4 / 7 / 10 modules ; la dernière coupe est rejetée. Les poids comptent les arcs uniques, pas les sites. Le lien moyen inclut les zéros ; la coupe impose une moyenne interne **strictement supérieure** à CP.

## Interface graphique commune

```bash
java -jar target/hai913i-tp1-analyzer.jar --gui
# Ou ouvrir directement l’explorateur TP2 à partir des sources ou des CSV :
java -jar target/hai913i-tp1-analyzer.jar tp2 --sources resources/validation --cp 0.025 --gui
```

Dans l’analyseur, choisir un projet et cliquer sur **Analyser**, régler **CP**, puis **Modules (TP2)**. Le bouton partage les faits déjà extraits, puis ouvre l’explorateur de couplage, d’arbre et de modules. Modifier le chemin invalide ces faits et désactive le bouton jusqu’à une nouvelle analyse. L’explorateur permet recherche, sélection, changement de CP ou de liaison, filtrage visuel et export.

## Architecture et fichiers

- `src/main/java/hai913i/tp1/` : extraction JDT/Spoon, modèle de faits, métriques, graphe d’appel et interface initiale.
- `src/main/java/hai913i/tp2/` : calcul du couplage, regroupement, coupe, exports et explorateur.
- `src/main/java/hai913i/integration/` : adaptateur direct et export CSV partagés par la CLI et l’interface.
- `src/test/` : tests TP1 existants et 24 tests TP2 repris du rendu autonome.
- `resources/`, `examples/`, `datasets/` : faits canoniques, micro jeu, corpus et extraction C4.
- `sorties/` : résultats et journaux du rendu TP2 autonome ; leur date et leur contexte ne constituent pas une validation nouvelle de l’intégration.
- `rapport/` : rapport TP1 ; [rapport TP2](rapport/tp2/Compte-Rendu.pdf), [README TP2 historique](rapport/tp2/README.pdf), audit et guide de soutenance.
- [Vidéo TP2](video/demonstration.mp4), [aperçu](video/apercu-style-tp1.png), chapitres et transcription : démonstration du rendu autonome du 16 octobre. Le moteur est intégré ici, mais cette vidéo ne prétend pas montrer les nouvelles commandes ou le bouton de liaison ajouté le 17 octobre.

## Validation et reproduction

```bash
mvn test
sh scripts/valider-bonus.sh
# Écran X11 disponible pour le contrôle de l’explorateur :
sh scripts/verifier-gui.sh
```

Les scripts sont adaptés à la racine du projet commun et au JAR unique. Le corpus C4 conserve les 29 fichiers d’origine comme données d’entrée (pas comme second projet Maven), avec un manifeste SHA-256. L’adaptateur conserve les méthodes implicites internes effectivement appelées et résolues : pour ce corpus, 199 méthodes, 267 arcs et 144 sites réintégrés. Il ne reconstruit ni corps synthétiques ni appels de constructeurs.

Voir [la note d’intégration](doc/INTEGRATION-TP2.md) pour les différences avec le rendu autonome, la portée des contrôles effectués et la reproduction des documents/vidéos. Usage de l’IA : l’implémentation et l’intégration TP2 ont été produites avec Codex ; la déclaration détaillée est conservée dans le rapport. Les deux étudiants doivent pouvoir expliquer l’ensemble.
