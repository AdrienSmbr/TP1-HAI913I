# Relecture comme correcteur, puis corrections

Audit du rendu précédent du TP2. Les remarques ci-dessous portent sur des défauts observés ou des garanties manquantes, sans promettre une note. Les points de contrôle canoniques étaient déjà justes ; ce fait ne suffisait pas à valider tout le rendu.

| Défaut initial | Conséquence possible en correction | Correction et preuve |
| --- | --- | --- |
| Vidéo principale reconstruite depuis des sorties de terminal ; graphe et arbre surtout textuels | Présentation discutable au regard de « outil en cours d'exécution, pas de diaporama » ; figures difficiles à apprécier | Nouvelle capture écran continue, avec vrais processus et images Graphviz régénérées pendant l'exécution. La dernière présentation utilise des frappes, mouvements et corrections scénarisés ; son automatisation reste déclarée. Voir `video/demonstration.mp4`, `chapitres.txt`, `executions.log`. |
| C4 montrait des diagnostics déjà sauvegardés ; C3 était dans un second fichier | La vidéo principale ne démontrait pas toute la chaîne des bonus | Réextraction TP1 exécutée pendant la capture ; interface et interactions intégrées à la même vidéo, y compris l'export. |
| Export graphique incomplet : pas de régénération de `resultats.txt`, provenance perdue | Un dossier pouvait annoncer une coupe acceptée alors que `modules.csv` contenait une coupe rejetée | `AnalysisReport` et `Exports.saveBundle` partagés entre CLI et interface. Régression `AuditTest.exportGraphiqueRemplaceAussiLeResumeEtConserveLaRelation`. |
| `--gui` terminait normalement même si la coupe finale était rejetée | Code de sortie 0 contraire à la convention documentée 0/3 | Le code final est décidé à la fermeture à partir de la coupe courante ; l'erreur d'ouverture de l'écran est remontée proprement. |
| Sans arc d'appel mais avec héritage, le diagnostic annonçait tous les poids nuls | Contradiction entre texte, graphe et métrique C2 | Le diagnostic porte sur l'absence de toute relation du graphe final ; test dédié à deux classes liées uniquement par héritage. |
| Rechargement acceptant un graphe non normalisé | CP était appliqué à des poids n'ayant plus l'échelle du couplage retenu | Rejet d'un graphe non vide dont la somme n'est pas 1 à 10⁻⁹ près ; le graphe sans arête reste accepté. |
| Noms contenant le séparateur des membres ou des guillemets acceptés | Exports ambigus ou non rechargeables | Validation explicite des noms/identifiants et test des caractères incompatibles avec le format simple annoncé. |
| Consultation d'une classe inconnue ou coupe d'un arbre incomplet silencieuses | Certaines erreurs de programmation pouvaient produire un résultat plausible mais faux | Validation des classes, membres, identifiants/similarités des nœuds et de la racine complète avant coupe. |
| Tests surtout centrés sur les jeux fournis | Généralité et absence d'erreur commune entre test et code insuffisamment établies | Oracle Python indépendant en fractions exactes : 112 exécutions, deux liaisons, 16 systèmes générés, fichiers mélangés, cas sans appel ; vérification des poids, fusions, membres, coupe et codes. |
| Sélections et détails anciens conservés lors du changement de liaison | Une ancienne branche pouvait encore être décrite alors que le nouvel arbre était affiché | Synchronisation des sélections classe/branche, effacement de l’inspection lors d’un changement de liaison ; assertions sur la fenêtre réelle. |
| Dialogue d'export de la fenêtre non testé | Un test du modèle ne prouvait pas que le bouton écrivait réellement le bon dossier | `GuiSmoke` ouvre le vrai JFileChooser, approuve un dossier, ferme le message et vérifie le résumé/export à CP=0,045. |
| Les 144 sites omis de C4 étaient tous présentés comme des méthodes implicites de records | Explication trop générale : le compteur ne prouvait pas cette catégorie | Complétion dans le pont : 69 méthodes cibles, 121 arcs et 144 sites réintégrés (143 RECORD, 1 ENUM). Détail `cibles-reintegrees.csv`, conservation contrôlée ; rapport, sorties et vidéo recalculés. |
| Manifeste SHA-256 présent mais non contrôlé par le protocole | La provenance du corpus pouvait dériver sans détection | Vérification effective des empreintes et de la liste exhaustive des 29 fichiers dans `verifier-extraction.py`. |
| Le pont écrivait les faits avant de signaler des erreurs de compilation | Des fichiers partiels pouvaient sembler réutilisables après un échec | Refus avant publication des CSV si JDT remonte des erreurs. |
| Sous-titres Markdown de troisième niveau non interprétés par le générateur PDF | Les sections de bonus du README pouvaient afficher des marqueurs bruts et manquer au sommaire | Prise en charge des titres de niveau 3, styles et signets PDF ; vérification des PDF régénérés. |
| Documents et journaux exposés à des écarts après les modifications | Décompte des tests, protocole vidéo ou statut d'export potentiellement contradictoires | Régénération des PDF, des sorties et de la vidéo ; reconstruction et protocole complet depuis l'archive décompressée. |

La première archive intermédiaire avait également comporté un commentaire XML illégal dans le POM ; ce défaut avait été corrigé lors de l'ajout des bonus. Le contrôle porte maintenant sur les POM réels et l'archive finale reconstruite, et pas uniquement sur un répertoire de travail antérieur.

## Ce que le correcteur peut toujours demander

L'aide de l'IA est déclarée dans le rapport avec son étendue réelle. Aucun test automatique, aucune vidéo pilotée et aucune déclaration ne peuvent prouver à la place des étudiants qu'ils maîtrisent l'algorithme. Le fichier `Guide-Soutenance.md` rassemble les calculs et les explications à savoir défendre. Le binôme doit les examiner ; le rendu ne prétend pas qu'une soutenance personnelle a déjà eu lieu.

Les limites méthodologiques sont maintenues explicitement : appels statiques seulement par défaut, perte de direction, gloutonnerie du regroupement, normalisation globale, seuil à recalibrer et accesseurs implicites absents du modèle TP1. Ce sont des limites du modèle étudiées dans B4/C4, pas des fonctionnalités secrètement complétées qui changeraient les conventions imposées.

## Compléments après relecture du sujet

- B1 : extraits ciblés du lien moyen et du départage, reliés au code livré.
- B4 : tableau exhaustif des sept modules face aux paquetages, et explication filmée du choix CP=0,025 et de Member–ConsoleNotifier.
- C4 : cibles implicites résolues conservées dans le pont ; 199 méthodes, 267 arcs, 190 interclasses, 52 arêtes et quatre isolés. À 0,025 : 23 modules rejetés. Les sources originales du TP1 restent inchangées.

## Vidéo : compléments pédagogiques après comparaison au TP1

- Quatre lectures d'extraits des fichiers source réellement livrés (numéros de ligne et expressions surlignées).
- Compteurs, couplages et statuts mis en évidence ; deux isolés du graphe et branche du module principal repérés en jaune sans changer les données.
- Trois erreurs exécutées et leurs diagnostics visibles : CP absent, CP mal formé, dossier inexistant ; codes 2 distingués de la coupe rejetée, code 3.
- Démonstrations B4 et quatre bonus conservés ; saisie et mouvements Robot toujours déclarés automatisés.

## Remplacement de la console filmée

La zone de texte Swing rendait l’exécution peu crédible visuellement. Elle a été retirée du pilote vidéo : xterm lance un vrai Bash interactif sur un PTY ; Robot saisit les commandes ; un hook d’invite transmet leurs véritables codes de sortie au pilote. Les sources sont ouvertes dans Vim, les images dans feh et C3 dans sa fenêtre Swing. Openbox gère les fenêtres. Les sous-titres sont ajoutés dans une bande basse ; le contenu du terminal n’est pas redessiné. La transcription réelle est fournie dans video/terminal.txt. Le pilote est désormais RealTerminalDemo.java.

## Style de la vidéo TP1

La présentation finale utilise VS Code avec terminal Bash intégré, en thème sombre, plutôt que des fenêtres séparées de terminal et d’éditeur. Le cartouche jaune d’étape, le texte explicatif dans le bandeau inférieur et les encadrés jaunes autour des sélections natives du code reprennent les repères du TP1. Les commandes, sources, images et interactions restent ceux d’une capture de programmes réels. Une courte capture de contrôle a précédé l’enregistrement complet.
