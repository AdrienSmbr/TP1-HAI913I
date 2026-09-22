package hai913i.tp1;

import hai913i.tp1.cli.Analyzer;
import hai913i.tp1.cli.CommandLine;
import hai913i.tp1.cli.Options;
import hai913i.tp1.cli.UsageException;

/**
 * Point d'entree. Lit les arguments, delegue, rend un code de sortie.
 *
 * Usage : java -jar target/hai913i-tp1-analyzer.jar PROJET [OPTIONS]
 */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        Options options;
        try {
            options = CommandLine.parse(args);
        } catch (UsageException e) {
            System.err.println("Erreur : " + e.getMessage());
            System.err.println();
            System.err.print(CommandLine.USAGE);
            System.exit(Analyzer.BAD_USAGE);
            return;
        }
        if (options.gui()) {
            hai913i.tp1.ui.AnalyzerWindow.launch(options.projectDirectory().toString());
            return;
        }
        System.exit(Analyzer.run(options, System.out, System.err));
    }
}
