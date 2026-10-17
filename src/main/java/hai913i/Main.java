package hai913i;

import java.util.Arrays;

/** Entrée commune ; les anciennes commandes du TP1 restent utilisables. */
public final class Main {
    private Main() {}
    public static void main(String[] args) {
        if (args.length > 0 && (args[0].equals("--help") || args[0].equals("-h"))) {
            System.out.println("HAI913I — TP1 + TP2 : sources Java → faits → métriques / couplage → modules");
            System.out.println("  java -jar target/hai913i-tp1-analyzer.jar tp1 PROJET --action all --x 5");
            System.out.println("  java -jar target/hai913i-tp1-analyzer.jar tp2 resources --cp 0.025");
            System.out.println("  java -jar target/hai913i-tp1-analyzer.jar tp2 --sources PROJET --cp 0.025 [--gui]");
            System.out.println("  java -jar target/hai913i-tp1-analyzer.jar export-facts PROJET DOSSIER");
            System.out.println("  java -jar target/hai913i-tp1-analyzer.jar --gui");
            System.out.println("Aide détaillée : tp1 --help / tp2 --help. Les commandes historiques TP1 restent valides.");
            return;
        }
        if (args.length > 0 && args[0].equals("tp2")) {
            hai913i.tp2.Main.main(Arrays.copyOfRange(args, 1, args.length)); return;
        }
        if (args.length > 0 && args[0].equals("export-facts")) {
            hai913i.integration.ExportFacts.main(Arrays.copyOfRange(args, 1, args.length)); return;
        }
        if (args.length > 0 && args[0].equals("tp1")) {
            if (args.length == 2 && (args[1].equals("--help") || args[1].equals("-h")))
                System.out.print(hai913i.tp1.cli.CommandLine.USAGE);
            else hai913i.tp1.Main.main(Arrays.copyOfRange(args, 1, args.length));
            return;
        }
        if (Arrays.asList(args).contains("--cp") || Arrays.asList(args).contains("--sources")) {
            hai913i.tp2.Main.main(args); return;
        }
        hai913i.tp1.Main.main(args);
    }
}
