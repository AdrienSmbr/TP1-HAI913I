# Intégration du TP2 au projet TP1 — 17 octobre 2026

## Une chaîne et un seul projet

Le moteur Java TP2 et ses 24 tests sont intégrés aux arbres Maven existants. Le manifeste lance hai913i.Main ; les sous-commandes tp1, tp2 et export-facts donnent une entrée commune, tandis que les anciennes commandes TP1 restent routées vers l’analyseur initial. Les dépendances JDT/Spoon du TP1 sont réutilisées.

Tp1FactsSource implémente FactsSource. La CLI --sources extrait les faits via JdtParser et FactExtractor, refuse les erreurs de compilation, puis adapte directement ProjectFacts. Le bouton Modules (TP2) partage exactement cet adaptateur sur les faits déjà calculés par l’interface. ExportFacts utilise le même code : la complétion des cibles implicites n’est donc pas réécrite trois fois.

L’adaptateur ajoute les cibles de records/enum réellement résolues mais absentes des méthodes déclarées, agrège les sites par couple de méthodes et transmet des arcs uniques au calcul du couplage. Le modèle TP1 historique et ses métriques ne sont pas modifiés : les compteurs TP2 peuvent donc inclure davantage de nœuds sur un projet utilisant des records. Les interfaces ne constituent pas une expansion polymorphe et aucun corps synthétique n’est inventé.

## Données, documents et vidéo

Les CSV et résultats du TP2 sont conservés ; resources/projects/analyseur-tp1 est le corpus figé de 29 fichiers, nécessaire pour reproduire les chiffres C4 du rapport. Analyser tout le dépôt désormais agrandi est possible avec --sources ., mais produit légitimement d’autres chiffres.

Les rapports et la vidéo décrivent la version autonome antérieure. Une note de migration est ajoutée aux documents ; le README principal est la référence d’exécution actuelle. Les journaux historiques mentionnant 24 tests et 112 comparaisons portent sur ce moteur autonome, pas sur une campagne exécutée après l’intégration. La vidéo conserve ses commandes et son contexte ; elle n’est pas présentée comme une nouvelle capture de l’application commune.

Les scripts de validation et de production sont adaptés à la racine du dépôt et au JAR unique. Aucun JAR, target, cache, archive dupliquée ou deuxième pom.xml n’est ajouté au dépôt.

## Contrôles de cette intégration

La construction Maven est effectuée avec `mvn -B -DskipTests package` : compilation des sources et des tests existants, assemblage du JAR commun. La suite de tests et les scripts de validation ne sont pas relancés dans cette opération de publication. Les commandes permettant de le faire figurent dans le README ; les traces antérieures restent identifiables.

## Reproduire les documents et la vidéo

```bash
python3 -m pip install -r rapport/tp2/requirements.txt
python3 rapport/tp2/generer.py
# Avec JDK, Maven, Graphviz, Python/Pillow/imageio-ffmpeg, Xvfb,
# Openbox, xdotool, feh, script et le binaire natif de VS Code :
TP2_CODE=/chemin/VSCode-linux-x64/code python3 video/enregistrer.py --xvfb
```

Le pilote est adapté à l’organisation commune ; une nouvelle capture montrera donc le JAR unique et l’ensemble des tests exécutés par Maven. L’ancienne vidéo livrée n’est pas remplacée silencieusement. Le commentaire annonce une suite complète plutôt qu’un nombre figé de tests.

Les auteurs et l’usage de Codex sont déclarés dans les rapports. Cette intégration n’atteste pas à elle seule que les étudiants maîtrisent les algorithmes : le guide de soutenance et les sources sont fournis pour leur appropriation.
