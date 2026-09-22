# HAI913I — TP 1 : Analyseur statique Java

Analyseur statique en ligne de commande. À partir du chemin d'un projet Java, il extrait
la structure et les appels de méthodes avec Eclipse JDT Core, construit un **modèle de faits
indépendant de JDT**, puis calcule 13 métriques et un graphe d'appel interrogeable dans les
deux sens.

SAMBARREY Adrien et PREVOST-AVENEL Pierre — 2026/2027 — enseignant : Bachar Rima.

## Environnement

| Élément | Version |
|---|---|
| JDK | 17 minimum (validé sur OpenJDK 17.0.16) |
| Maven | 3.9.11 |
| Eclipse JDT Core | 3.46.0 |
| Spoon (bonus C1) | 11.5.0 |
| JUnit Jupiter | 5.14.4 |
| Graphviz (bonus C2) | 14.1.0 |

Aucun JAR à installer à la main. Projet sur le classpath, pas de `module-info.java`.

## Construction

```bash
mvn clean package
```

## Exécution

```bash
java -jar target/hai913i-tp1-analyzer.jar ../validation --x 5
```

| Option | Rôle |
|---|---|
| `--action` | `structure` \| `calls` \| `metrics` \| `graph` \| `spoon` \| `all` (défaut : `all`) |
| `--x N` | seuil X, entier positif ou nul ; obligatoire pour `metrics` et `all` |
| `--callees ID` | méthodes appelées par `ID` |
| `--callers ID` | méthodes appelantes de `ID` |
| `--dot FICHIER` | export Graphviz |
| `--dot-package P` | limite l'export DOT à un paquetage |
| `--gui` | interface graphique (le chemin devient facultatif) |

`ID` a la forme `paquetage.Classe#methode(Type,Type)`.

Codes de sortie : `0` succès, `2` usage incorrect, `3` chemin invalide, `4` aucun `.java`.

## Validation

Sur le projet de validation fourni :

```
Unites de compilation  : 14        Erreurs de compilation : 0
Types : 15   Methodes : 59 (9 constructeurs)   Attributs : 21   Paquetages : 4
Appels : 94 (46 internes, 48 externes, 0 non resolus)
Graphe : 59 noeuds, 44 arcs, 46 sites
Metriques (X=5) : 493 lignes, 3.93 meth/classe, 4.64 lignes/meth, 1.40 attr/classe
```

Vérification automatique de tous les points de contrôle A0 à B4 :

```bash
mvn clean test     # 34 tests, 0 echec
```

## Organisation

```
hai913i.tp1
├── Main          lit les arguments, délègue
├── cli           Action, Options, CommandLine, Analyzer
├── parse         ProjectSources, JdtParser              ← connaît JDT
├── extract       StructureVisitor, CallCollector, …     ← connaît JDT
├── model         TypeFact, FieldFact, MethodFact, …     ← ne dépend de rien
├── metrics       Ranking, MetricsService
├── callgraph     CallGraph, CallGraphBuilder
├── report        TextReport, DotReport
├── ui            AnalyzerWindow                  (bonus C2)
└── spoon         SpoonExtractor, SpoonRawCounts  (bonus C1)
```

`metrics`, `callgraph`, `report` et `ui` ne contiennent aucun `import org.eclipse`.

## Dépannage

| Symptôme | Remède |
|---|---|
| `UnsupportedClassVersionError` | `java -version` et `JAVA_HOME` doivent pointer le même JDK 17 |
| `SecurityException: Invalid signature file digest` | reconstruire avec `mvn clean package` (le pom exclut les signatures Eclipse) |
| Moitié des appels non résolus, sans erreur | racine des sources mal détectée : vérifier la ligne « Racine des sources » |
| Image DOT illisible | filtrer avec `--dot-package library.service` |
