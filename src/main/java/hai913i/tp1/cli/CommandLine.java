package hai913i.tp1.cli;

import java.nio.file.Path;
import java.util.Locale;
import java.util.Optional;

/** Lecture et validation des arguments. Ne calcule rien. */
public final class CommandLine {

    public static final String USAGE = """
            Usage : java -jar target/hai913i-tp1-analyzer.jar PROJET [OPTIONS]

              PROJET                 dossier du projet Java a analyser
                                     facultatif avec --gui (bouton « Parcourir »)

            Options :
              --action ACTION        ast | structure | calls | metrics | graph | spoon | all  (defaut : all)
              --x N                  seuil X, entier positif ou nul
                                     obligatoire pour les actions metrics et all
              --callees ID           methodes appelees par ID
              --callers ID           methodes appelantes de ID
                                     ID a la forme paquetage.Classe#methode(Type,Type)
              --dot FICHIER          exporte le graphe d'appel au format Graphviz
              --dot-package PREFIXE  limite l'export DOT a un paquetage
              --file NOM             avec --action ast : n'affiche que ce fichier (ex. Dvd.java)
              --gui                  ouvre l'interface graphique

            Exemples :
              java -jar target/hai913i-tp1-analyzer.jar resources/validation --x 5
              java -jar target/hai913i-tp1-analyzer.jar resources/validation --action ast --file Dvd.java
              java -jar target/hai913i-tp1-analyzer.jar resources/validation --action graph \\
                  --callers 'library.model.Loanable#checkOut(library.model.Member)'
              java -jar target/hai913i-tp1-analyzer.jar resources/validation --action graph --dot graphe.dot
              java -jar target/hai913i-tp1-analyzer.jar resources/validation --gui
            """;

    private CommandLine() {
    }

    public static Options parse(String[] args) throws UsageException {
        // Le chemin est le premier argument s'il ne commence pas par un tiret.
        // Avec --gui il devient facultatif : la fenetre a un bouton « Parcourir ».
        boolean hasProject = args.length > 0 && !args[0].startsWith("--");
        Path project = hasProject ? Path.of(args[0]) : null;
        Action action = Action.ALL;
        Integer threshold = null;
        Optional<String> callees = Optional.empty();
        Optional<String> callers = Optional.empty();
        Optional<Path> dotFile = Optional.empty();
        Optional<String> dotPackage = Optional.empty();
        Optional<String> astFile = Optional.empty();
        boolean gui = false;

        for (int i = hasProject ? 1 : 0; i < args.length; i++) {
            String option = args[i];
            switch (option) {
                case "--action" -> action = readAction(value(args, ++i, "--action"));
                case "--x" -> threshold = readThreshold(value(args, ++i, "--x"));
                case "--callees" -> callees = Optional.of(value(args, ++i, "--callees"));
                case "--callers" -> callers = Optional.of(value(args, ++i, "--callers"));
                case "--dot" -> dotFile = Optional.of(Path.of(value(args, ++i, "--dot")));
                case "--dot-package" -> dotPackage = Optional.of(value(args, ++i, "--dot-package"));
                case "--file" -> astFile = Optional.of(value(args, ++i, "--file"));
                case "--gui" -> gui = true;
                default -> throw new UsageException("option inconnue : " + option);
            }
        }

        if (gui) {
            return new Options(project == null ? Path.of("") : project,
                    action, threshold == null ? 0 : threshold,
                    callees, callers, dotFile, dotPackage, astFile, true);
        }
        if (project == null) {
            throw new UsageException("chemin du projet manquant");
        }
        // L'absence de seuil n'est pas refusee ici : un chemin invalide doit etre
        // diagnostique avant lui. C'est Analyzer qui exige X, apres verification
        // du projet. Voir le point de controle B4.
        return new Options(project, action, threshold,
                callees, callers, dotFile, dotPackage, astFile, false);
    }

    private static String value(String[] args, int index, String option) throws UsageException {
        if (index >= args.length) {
            throw new UsageException("valeur manquante apres " + option);
        }
        return args[index];
    }

    private static Action readAction(String raw) throws UsageException {
        try {
            return Action.valueOf(raw.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new UsageException("action inconnue : " + raw
                    + " (attendu : ast, structure, calls, metrics, graph, spoon, all)");
        }
    }

    /** X doit etre un entier positif ou nul ; toute autre valeur est refusee. */
    private static int readThreshold(String raw) throws UsageException {
        int value;
        try {
            value = Integer.parseInt(raw);
        } catch (NumberFormatException e) {
            throw new UsageException("seuil X mal forme : « " + raw
                    + " » n'est pas un entier");
        }
        if (value < 0) {
            throw new UsageException("seuil X mal forme : « " + raw
                    + " » doit etre positif ou nul");
        }
        return value;
    }
}
