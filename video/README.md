# Démonstration TP1 → TP2

[Vidéo principale](demonstration.mp4) · [Aperçu](apercu-style-tp1.png) · [Chapitres](chapitres.txt) · [Sous-titres](sous-titres.vtt)

Nouvelle capture du projet intégré, réalisée le 17 octobre 2026 : 10 min 24, 1920 × 1080, 12 images/s, sans voix. Le MP4 contient 32 chapitres navigables. Les explications sont inscrites dans les bandeaux ; le fichier WebVTT permet aussi de les consulter séparément.

Le cadre reprend la [vidéo du TP1](https://youtu.be/UB8gPc2GF-g) : éditeur sombre, véritable terminal intégré, lecture du code sélectionné et bandeaux jaunes. La continuité est montrée dans l'application :

| Repère | Démonstration |
| --- | --- |
| 00:15 | Métriques TP1 sur la bibliothèque d'origine |
| 00:28 | Lecture de l'adaptateur partagé |
| 00:46 | Extraction directe par `tp2 --sources` |
| 01:07 | Analyse dans l'interface TP1 |
| 01:21 | Bouton « Modules (TP2) », dendrogramme et coupe |
| 01:53 | Faits canoniques, couplage et graphe |
| 03:20 | Clustering et départage des égalités |
| 04:33 | Coupe, choix de CP et interprétation |
| 06:25 | Trois diagnostics d'entrée invalide |
| 07:24 | Bonus C1 : lien simple |
| 07:45 | Bonus C2 : relations enrichies |
| 08:25 | Bonus C4 : extraction d'un second projet |
| 09:24 | Bonus C3 : exploration et dialogue d'export |

## Provenance et reproduction

Le bureau X11 est capturé en continu par Robot. Bash exécute les commandes dans le terminal de VS Code ; feh affiche les graphes produits et les fenêtres Swing exécutent le code réel de l'application. Les fenêtres Swing sont ouvertes dans la JVM du pilote. Les gestes de souris, frappes et corrections sont automatisés ; cette vidéo ne représente pas une manipulation humaine.

La construction utilise `-DskipTests`. Aucun nouveau résultat de suite de tests n'est annoncé. Les codes de sortie des commandes réellement exécutées figurent dans [executions.log](executions.log), et la sortie du terminal dans [terminal.txt](terminal.txt). Les anciennes validations du rendu autonome restent identifiées comme historiques.

Avec JDK 17, Maven, Graphviz, Python/Pillow/imageio-ffmpeg, Xvfb, Openbox, xdotool, feh, script et VS Code natif :

```bash
TP2_CODE=/chemin/VSCode-linux-x64/code python3 video/enregistrer.py --xvfb --work-dir /chemin/capture
# Réencoder une capture terminée :
python3 video/enregistrer.py --encode-only /chemin/capture/captures
```

Le film précédent reste accessible dans l'historique Git au commit `6222b9f`. `interface-bonus.mp4` est une ancienne capture complémentaire ; la démonstration actuelle complète est `demonstration.mp4`.
