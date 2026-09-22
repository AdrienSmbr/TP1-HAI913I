package hai913i.tp1.cli;

import java.nio.file.Path;
import java.util.Locale;
import java.util.Optional;

/** Lecture et validation des arguments. Ne calcule rien. */
public final class CommandLine {

    public static final String USAGE = """
            Usage : java -jar target/hai913i-tp1-analyzer.jar PROJET [OPTIONS]

              PROJET                 dossier du projet Java a analyser (bouton « Parcourir »)

            Options :
              --action ACTION        structure | calls | metrics | graph | all   (defaut : all)
              --x N                  seuil X, entier positif ou nul
                                     obligatoire pour les actions metrics et all
              --callees ID           methodes appelees par ID
              --callers ID           methodes appelantes de ID
                                     ID a la forme paquetage.Classe#methode(Type,Type)

            Exemples :
              java -jar target/hai913i-tp1-analyzer.jar ../validation --x 5
              java -jar target/hai913i-tp1-analyzer.jar ../validation --action graph \\
                  --callers 'library.model.Loanable#checkOut(library.model.Member)'
            """;

    private CommandLine() {
    }

    public static Options parse(String[] args) throws UsageException {
        if (args.length < 1) {
            throw new UsageException("chemin du projet manquant");
        }
        Path project = Path.of(args[0]);
        Action action = Action.ALL;
        Integer threshold = null;
        Optional<String> callees = Optional.empty();
        Optional<String> callers = Optional.empty();

        for (int i = 1; i < args.length; i++) {
            String option = args[i];
            switch (option) {
                case "--action" -> action = readAction(value(args, ++i, "--action"));
                case "--x" -> threshold = readThreshold(value(args, ++i, "--x"));
                case "--callees" -> callees = Optional.of(value(args, ++i, "--callees"));
                case "--callers" -> callers = Optional.of(value(args, ++i, "--callers"));
                default -> throw new UsageException("option inconnue : " + option);
            }
        }

        if (action.needsThreshold() && threshold == null) {
            throw new UsageException("le seuil X est obligatoire pour l'action "
                    + action.name().toLowerCase(Locale.ROOT) + " : ajoutez --x N");
        }
        return new Options(project, action, threshold == null ? 0 : threshold,
                callees, callers);
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
                    + " (attendu : structure, calls, metrics, graph, all)");
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
