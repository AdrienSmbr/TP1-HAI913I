package hai913i.tp2;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.*;
import javax.imageio.ImageIO;

/** Capture d'un vrai bureau : VS Code avec terminal Bash intégré, feh et l'application Swing.
 * Les commandes sont saisies par Robot et exécutées par le shell interactif.
 */
public final class RealTerminalDemo {
    private static NaturalInput hand;
    private static ExplorerWindow explorer;
    private static hai913i.tp1.ui.AnalyzerWindow analyzer;
    private static Path root,source,captures;
    private static PrintWriter timeline,notes,transcript;
    private static long start;
    private static Process terminal;
    private static String terminalWindow;
    private static int commandNumber;
    private static final AtomicBoolean recording=new AtomicBoolean(true);
    private static final AtomicReference<Throwable> failure=new AtomicReference<>();
    private static void edt(Runnable run) throws Exception {SwingUtilities.invokeAndWait(run);}
    private static Component find(Container parent,String name) {
        for(Component c:parent.getComponents()){
            if(name.equals(c.getName()))return c;
            if(c instanceof Container nested){var found=find(nested,name);if(found!=null)return found;}
        }return null;
    }
    private static <T> T findType(Container parent, Class<T> type) {
        for(Component c:parent.getComponents()){
            if(type.isInstance(c))return type.cast(c);
            if(c instanceof Container nested){T found=findType(nested,type);if(found!=null)return found;}
        }return null;
    }
    private static void exportDialog(Path path) throws Exception {
        Files.createDirectories(path);
        hand.click(find(explorer,"export"));hand.pause(650,1100);
        JFileChooser chooser=null;
        for(int retry=0;retry<60 && chooser==null;retry++){
            for(Window w:Window.getWindows())if(w instanceof JDialog d && d.isShowing()){
                JFileChooser found=findType(d,JFileChooser.class);if(found!=null){chooser=found;break;}
            }
            if(chooser==null)Thread.sleep(100);
        }
        if(chooser==null)throw new IllegalStateException("dialogue d'export absent");
        JTextField name=findType(chooser,JTextField.class);
        if(name==null)throw new IllegalStateException("champ du dossier d'export absent");
        hand.click(name);hand.selectAll();hand.type(path.toString(),false);
        String approve=UIManager.getString("FileChooser.saveButtonText",chooser.getLocale());
        JButton button=findButton(chooser,approve);
        if(button==null)throw new IllegalStateException("bouton d'export absent");
        hand.click(button);hand.pause(650,1200);
        for(int retry=0;retry<60;retry++){
            for(Window w:Window.getWindows())if(w instanceof JDialog d && d.isShowing() && findType(d,JOptionPane.class)!=null){
                JButton ok=findButton(d,"OK");if(ok==null)throw new IllegalStateException("bouton OK absent");
                hand.click(ok);return;
            }
            Thread.sleep(100);
        }
        throw new IllegalStateException("confirmation d'export absente");
    }
    private static JButton findButton(Container root,String text){
        for(Component c:root.getComponents()){
            if(c instanceof JButton b && text.equals(b.getText()))return b;
            if(c instanceof Container nested){var found=findButton(nested,text);if(found!=null)return found;}
        }return null;
    }
    private static void editSpinner(String name,String value,boolean correction) throws Exception {
        JSpinner spinner=(JSpinner)find(explorer,name);
        JTextField field=((JSpinner.DefaultEditor)spinner.getEditor()).getTextField();
        hand.click(field);hand.selectAll();hand.type(value,correction);hand.key(KeyEvent.VK_ENTER);hand.pause(300,650);
        if(Math.abs(((Number)spinner.getValue()).doubleValue()-Double.parseDouble(value))>1e-12)
            throw new IllegalStateException("valeur du contrôle non saisie : "+name);
    }
    private static void tab(int index) throws Exception {
        JTabbedPane tabs=(JTabbedPane)find(explorer,"views");Point[] p=new Point[1];
        edt(()->{Rectangle b=tabs.getBoundsAt(index);Point q=tabs.getLocationOnScreen();p[0]=new Point(q.x+b.x+b.width/2,q.y+b.y+b.height/2);});
        hand.click(p[0].x,p[0].y);
    }
    private static String nativeCommand(String... args) throws Exception {
        Process p=new ProcessBuilder(args).redirectErrorStream(true).start();
        String output=new String(p.getInputStream().readAllBytes(),java.nio.charset.StandardCharsets.UTF_8);
        if(p.waitFor()!=0)throw new IllegalStateException(Arrays.toString(args)+": "+output);
        return output.trim();
    }
    private static void chord(int modifier,int key) throws Exception {
        Robot robot=new Robot();robot.keyPress(modifier);hand.key(key);robot.keyRelease(modifier);
    }
    private static void terminalFocus() throws Exception {
        Robot robot=new Robot();robot.keyPress(KeyEvent.VK_CONTROL);robot.keyPress(KeyEvent.VK_ALT);
        hand.key(KeyEvent.VK_T);robot.keyRelease(KeyEvent.VK_ALT);robot.keyRelease(KeyEvent.VK_CONTROL);
    }
    private static void focusTerminal() throws Exception {
        nativeCommand("xdotool","windowactivate","--sync",terminalWindow);terminalFocus();hand.pause(650,950);
    }
    private static int[] status() {
        try{
            String[] values=Files.readString(captures.resolve("status")).trim().split(" ");
            return new int[]{Integer.parseInt(values[0]),Integer.parseInt(values[1])};
        }catch(Exception e){return new int[]{-1,-1};}
    }
    private static int waitPrompt(int previous) throws Exception {
        long limit=System.nanoTime()+240_000_000_000L;
        while(System.nanoTime()<limit){
            int[] state=status();if(state[0]>previous)return state[1];
            Thread.sleep(100);
        }
        throw new IllegalStateException("Délai de commande dépassé");
    }
    private static void chapter(String name,String text) throws Exception {
        double t=(System.nanoTime()-start)/1e9;
        System.out.println(name);timeline.printf(Locale.ROOT,"%.3f\t%s%n",t,name);timeline.flush();
        notes.printf(Locale.ROOT,"%.3f\t%s\t%s%n",t,name,text);notes.flush();
        if((explorer==null || !explorer.isShowing()) && (analyzer==null || !analyzer.isShowing())){
            focusTerminal();
            Robot key=new Robot();key.keyPress(KeyEvent.VK_CONTROL);hand.key(KeyEvent.VK_L);key.keyRelease(KeyEvent.VK_CONTROL);
        }
    }
    private static void begin(String cmd) throws Exception {
        focusTerminal();hand.pause(450,650);transcript.println("$ "+cmd);transcript.flush();
        hand.type(cmd,++commandNumber==3 || commandNumber==8 || commandNumber==12);
        hand.key(KeyEvent.VK_ENTER);
    }
    private static void complete(int before,int expected,String cmd) throws Exception {
        int code=waitPrompt(before);
        transcript.println("[code de sortie : "+code+"]");transcript.flush();
        if(code!=expected)throw new IllegalStateException("Code "+code+" au lieu de "+expected+" : "+cmd);
        hand.move(500+new Random(commandNumber).nextInt(650),530+new Random(commandNumber*3L).nextInt(240));
        hand.pause(3800,5400);
    }
    private static void command(String cmd,int expected) throws Exception {
        if(cmd.contains("exit $code"))cmd="("+cmd+")";
        int before=status()[0];begin(cmd);complete(before,expected,cmd);
    }
    private static void code(String filename,String range,String... focus) throws Exception {
        String[] lines=range.split(",");int first=Integer.parseInt(lines[0]),last=Integer.parseInt(lines[1]);
        focusTerminal();chord(KeyEvent.VK_CONTROL,KeyEvent.VK_J);hand.pause(300,500);
        chord(KeyEvent.VK_CONTROL,KeyEvent.VK_P);
        hand.type("src/main/java/hai913i/"+(filename.contains("/")?filename:"tp2/"+filename)+".java:"+first,false);hand.key(KeyEvent.VK_ENTER);hand.pause(700,1100);
        chord(KeyEvent.VK_CONTROL,KeyEvent.VK_G);hand.type(String.valueOf(first),false);hand.key(KeyEvent.VK_ENTER);hand.key(KeyEvent.VK_HOME);
        Robot robot=new Robot();robot.keyPress(KeyEvent.VK_SHIFT);for(int i=first;i<=last;i++)hand.key(KeyEvent.VK_DOWN);robot.keyRelease(KeyEvent.VK_SHIFT);
        transcript.println("[source éditeur : "+filename+".java lignes "+range+"]");transcript.flush();
        hand.move(1090,430);hand.pause(9000,11000);
        hand.key(KeyEvent.VK_ESCAPE);focusTerminal();hand.pause(800,1300);
    }
    private static void image(Path path) throws Exception {
        String cmd="feh --auto-zoom --scale-down --geometry 1860x900+30+30 "+source.relativize(path);
        int before=status()[0];begin(cmd);hand.pause(1700,2300);
        hand.move(780,490);hand.pause(2000,3000);hand.move(1140,730);hand.pause(5000,6500);
        hand.key(KeyEvent.VK_Q);complete(before,0,cmd);
    }
    public static void main(String[] args) throws Exception {
        root=Path.of(args[0]).toAbsolutePath();source=root;captures=Path.of(args[1]).toAbsolutePath();Files.createDirectories(captures);
        timeline=new PrintWriter(Files.newBufferedWriter(captures.resolve("chapitres.tsv")));
        notes=new PrintWriter(Files.newBufferedWriter(captures.resolve("notes.tsv")));
        transcript=new PrintWriter(Files.newBufferedWriter(captures.resolve("executions.log")));
        String rc="set -o pipefail\nHISTFILE="+captures.resolve("history")+"\nPS1='\\[\\e[32m\\]\\u@\\h\\[\\e[0m\\]:\\[\\e[34m\\]\\W\\[\\e[0m\\]\\$ '\n__tp2_n=0\n__tp2_prompt() { local s=$?; __tp2_n=$((__tp2_n+1)); printf '%s %s\\n' \\\"$__tp2_n\\\" \\\"$s\\\" > "+captures.resolve("status")+"; return \\\"$s\\\"; }\nPROMPT_COMMAND=__tp2_prompt\nalias grep='grep --color=auto'\n";
        rc=rc.replace("\\\"","\"");
        Files.writeString(captures.resolve("bashrc"),rc);
        Files.writeString(captures.resolve("vimrc"),"set nocompatible\nset number nowrap noswapfile nobackup nowritebackup\nset background=dark\nsyntax on\nset laststatus=2\nset ruler\nset scrolloff=3\nset t_Co=256\nhighlight Search ctermbg=178 ctermfg=0\n");
        Path profile=captures.resolve("editor-profile"),user=profile.resolve("User");Files.createDirectories(user);
        String settings="""
        {"workbench.startupEditor":"none","workbench.colorTheme":"Default Dark Modern",
        "window.menuBarVisibility":"compact","window.commandCenter":false,
        "workbench.activityBar.location":"hidden","editor.fontSize":20,"editor.minimap.enabled":false,
        "editor.scrollBeyondLastLine":false,"terminal.integrated.fontSize":18,
        "terminal.integrated.enablePersistentSessions":false,"terminal.integrated.shellIntegration.enabled":false,
        "security.workspace.trust.enabled":false,"telemetry.telemetryLevel":"off","chat.disableAIFeatures":true,
        "workbench.secondarySideBar.defaultVisibility":"hidden","update.mode":"none",
        "extensions.autoCheckUpdates":false,"extensions.autoUpdate":false,"extensions.ignoreRecommendations":true,
        "workbench.colorCustomizations":{"editor.selectionBackground":"#665018","editor.inactiveSelectionBackground":"#665018"},
        "terminal.integrated.defaultProfile.linux":"TP2","terminal.integrated.profiles.linux":{
        "TP2":{"path":"/usr/bin/script","args":["--quiet","--flush","--log-out","%s","--command","bash --noprofile --rcfile %s -i"]}}}
        """.formatted(captures.resolve("terminal.typescript"),captures.resolve("bashrc"));
        Files.writeString(user.resolve("settings.json"),settings);
        Files.writeString(user.resolve("keybindings.json"),"[{\"key\":\"ctrl+alt+t\",\"command\":\"workbench.action.terminal.focus\"},{\"key\":\"ctrl+alt+m\",\"command\":\"workbench.action.toggleMaximizedPanel\"},{\"key\":\"ctrl+alt+u\",\"command\":\"workbench.action.increaseViewHeight\"}]");
        String editor=System.getenv().getOrDefault("TP2_CODE","code");
        ProcessBuilder pb=new ProcessBuilder(editor,"--no-sandbox","--disable-gpu","--ozone-platform=x11","--extensions-dir="+captures.resolve("editor-extensions"),"--disable-workspace-trust","--skip-welcome","--user-data-dir="+profile,"--new-window",source.toString(),source.resolve("src/main/java/hai913i/tp2/CouplingCalculator.java").toString());
        pb.directory(source.toFile());terminal=pb.redirectErrorStream(true).redirectOutput(captures.resolve("editor.log").toFile()).start();
        hand=new NaturalInput();Robot reset=new Robot();for(int k:new int[]{KeyEvent.VK_Q,KeyEvent.VK_CONTROL,KeyEvent.VK_SHIFT,KeyEvent.VK_ALT,KeyEvent.VK_META})reset.keyRelease(k);
        for(int i=0;i<100 && terminalWindow==null;i++){
            try{terminalWindow=nativeCommand("xdotool","search","--onlyvisible","--name","CouplingCalculator.java.*Visual Studio Code").split("\\s+")[0];}
            catch(Exception ignored){Thread.sleep(200);}
        }
        if(terminalWindow==null)throw new IllegalStateException("Fenêtre de l’éditeur absente");
        nativeCommand("xdotool","windowmove",terminalWindow,"65","25");
        nativeCommand("xdotool","windowsize",terminalWindow,"1790","940");
        nativeCommand("xdotool","windowactivate","--sync",terminalWindow);hand.pause(2500,3500);
        hand.key(KeyEvent.VK_ESCAPE);hand.pause(250,400);hand.key(KeyEvent.VK_ESCAPE);
        chord(KeyEvent.VK_CONTROL,KeyEvent.VK_B);terminalFocus();
        for(int i=0;i<150 && status()[0]<1;i++)Thread.sleep(200);
        if(status()[0]<1)throw new IllegalStateException("Terminal intégré non prêt");
        // Agrandir le panneau par son séparateur réel ; l'éditeur reste visible au-dessus.
        hand.move(1000,640);reset.mousePress(InputEvent.BUTTON1_DOWN_MASK);hand.move(1000,275);reset.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
        focusTerminal();reset.keyPress(KeyEvent.VK_CONTROL);hand.key(KeyEvent.VK_U);reset.keyRelease(KeyEvent.VK_CONTROL);
        start=System.nanoTime();
        Thread capture=new Thread(()->{
            try{Robot robot=new Robot();int frame=0;PrintWriter times=new PrintWriter(Files.newBufferedWriter(captures.resolve("frames.tsv")));
                while(recording.get()){
                    double time=(System.nanoTime()-start)/1e9;String name=String.format("frame-%05d.png",frame++);
                    BufferedImage im=robot.createScreenCapture(new Rectangle(0,0,1920,1080));
                    Point c=MouseInfo.getPointerInfo().getLocation();Graphics2D g=im.createGraphics();
                    Polygon arrow=new Polygon(new int[]{c.x,c.x,c.x+6,c.x+10,c.x+13,c.x+9,c.x+16},new int[]{c.y,c.y+23,c.y+17,c.y+27,c.y+25,c.y+15,c.y+15},7);
                    g.setColor(Color.WHITE);g.fill(arrow);g.setColor(Color.BLACK);g.draw(arrow);g.dispose();
                    ImageIO.write(im,"png",captures.resolve(name).toFile());times.printf(Locale.ROOT,"%.6f\t%s%n",time,name);times.flush();Thread.sleep(80);
                }times.close();
            }catch(Throwable e){failure.set(e);}
        });capture.start();
        try {
            if(args.length>2 && args[2].equals("--preview")){
                chapter("A1 · Couplage — lecture du code", "Un arc interclasse ajoute une unité ; le dénominateur est le nombre d’arcs interclasses.");
                code("CouplingCalculator","12,24");
                chapter("A0 · Vérifier les faits", "Les compteurs affichés viennent du programme exécuté dans le terminal intégré.");
                command("pwd && java -version",0);
                return;
            }
            chapter("Construction du projet commun", "Suite du TP1 : un seul projet Maven et un seul JAR. Compilation avec -DskipTests ; cette capture ne relance pas la suite de tests.");
            command("java -version && mvn -B -Dstyle.color=never -DskipTests package | tail -18",0);
            chapter("Continuité · Retrouver l'analyse du TP1", "La bibliothèque du TP1 reste notre point de départ : mêmes sources Java, mêmes métriques et même analyseur JDT.");
            command("java -jar target/hai913i-tp1-analyzer.jar tp1 resources/validation --action metrics --x 5",0);
            chapter("Continuité · Lire le pont entre les deux TP", "L'adaptateur réutilise ProjectFacts. Il conserve les classes, les méthodes et les arcs internes résolus pour alimenter le moteur TP2.");
            code("integration/Tp1FactsSource","45,66");
            chapter("Continuité · Des sources aux modules en une commande", "--sources appelle directement l'extracteur du TP1. Les résultats suivants viennent des fichiers Java, sans CSV intermédiaire.");
            command("java -jar target/hai913i-tp1-analyzer.jar tp2 --sources resources/validation --cp 0.025 --out target/demo-direct > target/demo-direct.txt && head -12 target/demo-direct.txt && tail -11 target/demo-direct.txt",0);
            chapter("Continuité · Retrouver l'interface du TP1", "L'interface conserve la structure, les appels et les métriques. Le bouton Modules (TP2) sera activé après l'analyse des sources.");
            edt(()->{analyzer=new hai913i.tp1.ui.AnalyzerWindow("resources/validation");analyzer.setBounds(30,35,1860,930);analyzer.setVisible(true);analyzer.toFront();});
            hand.pause(1800,2500);
            JButton analyse=findButton(analyzer,"Analyser"), modules=findButton(analyzer,"Modules (TP2)");
            hand.click(analyse);
            for(int retry=0;retry<120 && !modules.isEnabled();retry++)Thread.sleep(100);
            if(!modules.isEnabled())throw new IllegalStateException("analyse TP1 non terminée");
            hand.pause(3000,4000);
            JTabbedPane originalTabs=findType(analyzer,JTabbedPane.class);Point[] metricTab=new Point[1];
            edt(()->{Rectangle b=originalTabs.getBoundsAt(2);Point q=originalTabs.getLocationOnScreen();metricTab[0]=new Point(q.x+b.x+b.width/2,q.y+b.y+b.height/2);});
            hand.click(metricTab[0].x,metricTab[0].y);hand.pause(4000,5500);
            chapter("Continuité · Ouvrir les modules depuis le TP1", "Un clic transmet les faits déjà en mémoire : pas de réextraction et pas de CSV. CP vaut ici 0,025, avec le lien moyen.");
            hand.click(modules);hand.pause(1300,1800);
            edt(()->{for(Window w:Window.getWindows())if(w instanceof ExplorerWindow e && e.isShowing())explorer=e;
                if(explorer==null)throw new IllegalStateException("explorateur intégré absent");
                explorer.setBounds(30,35,1860,930);explorer.toFront();});
            hand.pause(4000,5500);tab(1);hand.pause(3500,4500);tab(2);hand.pause(4000,5500);
            chapter("Continuité · Faire varier CP dans la même application", "À 0,045 la coupe est rejetée ; à 0,025 elle est acceptée. Le graphe reste identique : seul le parcours de coupe change.");
            editSpinner("cp","0.045",false);hand.pause(4500,5500);editSpinner("cp","0.025",false);hand.pause(3500,4500);
            edt(()->{explorer.dispose();analyzer.dispose();});
            if(args.length>2 && args[2].equals("--preview-integrated"))return;
            chapter("A0 · Revenir aux faits de référence du sujet", "On distingue l'extraction réelle précédente des CSV canoniques fournis : les chiffres de référence du TP2 se lisent sur ces CSV.");
            command("head -4 resources/facts-validation-classes.csv && head -4 resources/facts-validation-calls.csv",0);
            chapter("A1 · Du code à la formule du couplage", "Chaque arc interclasse ajoute 1 ; sites ne multiplie pas le poids. Le nombre d'arcs interclasses sert ensuite de dénominateur.");
            code("CouplingCalculator","12,24","if (a.equals(b))", "inter++", "1, Integer::sum", "count / (double) denominator");
            chapter("A0–A2 · Charger les faits et calculer le couplage", "15 classes, 59 méthodes et 44 arcs. On écarte 7 arcs internes : le dénominateur est 37, sans compter les sites d'appel.");
            command("java -jar target/hai913i-tp1-analyzer.jar tp2 resources --cp 0.025 --out target/demo-025 > target/demo-025.txt && head -25 target/demo-025.txt",0);
            chapter("A2 · Graphe pondéré et image produite", "19 arêtes ; Dvd et LoanException sont conservées comme nœuds isolés. Les images sont calculées à partir des exports de cette exécution.");
            command("python3 scripts/dessiner.py target/demo-025 && python3 video/annoter.py target/demo-025",0);image(source.resolve("target/demo-025/graphe-focus.png"));
            chapter("B1 · Lire le lien moyen dans le code", "La somme parcourt tous les couples, même nuls, puis divise par le produit des tailles. Le lien simple retourne uniquement le maximum.");
            code("AgglomerativeClusterer","25,33","sum += weight", "Linkage.SINGLE", "return sum /", "a.members().size() * b.members().size()");
            chapter("B1 · Départager les égalités sans dérive", "Les candidats suivent les identifiants croissants. On calcule le vrai maximum, puis on prend le premier couple situé à moins de 10⁻⁹.");
            code("AgglomerativeClusterer","43,57","maximum = Math.max", "maximum - c.similarity() <= EPSILON", "break;", "nextId++");
            chapter("B1–B2 · Fusions et dendrogramme", "Lien moyen, zéros compris. Les identifiants tranchent les égalités. Quatorze fusions, dont les deux dernières à zéro.");
            command("sed -n '/B1\\/B2/,/B3 |/p' target/demo-025.txt",0);image(source.resolve("target/demo-025/dendrogramme-focus.png"));
            chapter("B3 · La coupe est une lecture de branches", "mean vient de internalMean, pas de la similarité de fusion. Le test est strict : mean > cp ; sinon on descend dans les deux fils.");
            code("ModuleCutter","19,29","graph.classes().size() / 2", "graph.internalMean(node.members())", "node.isLeaf() || mean > cp", "visit(node.left()", "visit(node.right()");
            chapter("B3 · Coupe à CP = 0,025", "Sept modules : plafond respecté. Le module principal a C̄ = 0,033462, supérieur à CP ; sa similarité de fusion ne sert pas à la coupe.");
            command("sed -n '/B3 |/,$p' target/demo-025.txt",0);
            chapter("B3 · Coupe rejetée à CP = 0,045", "Dix modules dépassent le plafond floor(15/2) = 7. Le code 3 signale ce rejet et les exports restent disponibles.");
            command("java -jar target/hai913i-tp1-analyzer.jar tp2 resources --cp 0.045 --out target/demo-045 > target/demo-045.txt; code=$?; tail -13 target/demo-045.txt; exit $code",3);
            chapter("B4 · Pourquoi retenir CP = 0,025 ?", "0,015 donne quatre modules dont un bloc de onze classes ; 0,025 donne sept modules ; 0,045 dépasse le plafond. Le choix reste discutable.");
            command("java -jar target/hai913i-tp1-analyzer.jar tp2 resources --cp 0.015 --out target/demo-015 > target/demo-015.txt && grep 'B3 |' target/demo-015.txt && cat target/demo-015/modules.csv",0);
            chapter("B4 · Member–ConsoleNotifier : un module discutable", "Un appel au getter getName donne 1/37 > 0,025. Cette paire mélange domaine et affichage ; les appels ne suffisent pas à définir une responsabilité.");
            command("grep 'Member.*ConsoleNotifier' target/demo-025/modules.csv && grep 'Member' target/demo-025/couplages.csv",0);
            chapter("Déterminisme et rechargement", "Une seconde exécution donne les mêmes fichiers. Le graphe CSV rechargé redonne les mêmes modules, y compris les classes isolées.");
            command("java -jar target/hai913i-tp1-analyzer.jar tp2 resources --cp 0.025 --out target/demo-repeat > /dev/null && diff -qr target/demo-025 target/demo-repeat --exclude='*-image.dot' --exclude='*.png' && java -jar target/hai913i-tp1-analyzer.jar tp2 --graph target/demo-025/graphe.csv --cp 0.025 --out target/demo-recharge > /dev/null && cmp target/demo-025/modules.csv target/demo-recharge/modules.csv && printf 'Exports identiques apres rechargement.\\n'",0);
            chapter("Robustesse · Un seuil CP absent est refusé", "Le seuil doit être explicite : l'outil affiche le diagnostic et l'usage, puis rend le code 2. Aucun seuil n'est choisi silencieusement.");
            command("java -jar target/hai913i-tp1-analyzer.jar tp2 resources",2); command("echo $?",0);
            chapter("Robustesse · Un seuil CP mal formé est refusé", "Une chaîne à la place d'un nombre ne lance pas de calcul. Le message reste lisible, sans pile d'exception ; code de sortie 2.");
            command("java -jar target/hai913i-tp1-analyzer.jar tp2 resources --cp abc",2); command("echo $?",0);
            chapter("Robustesse · Un dossier inexistant est refusé", "Avec un CP valide, un chemin de données inexistant provoque un diagnostic et le code 2. Ce n'est pas le code 3 d'une coupe rejetée.");
            command("java -jar target/hai913i-tp1-analyzer.jar tp2 /chemin/inexistant --cp 0.025",2); command("echo $?",0);
            chapter("C1 · Lien simple : divergence dès la deuxième fusion", "Le maximum Loan–LoanService vaut 4/37. Leur moyenne avec Item vaut seulement 2/37 : le lien moyen choisit LibraryApp–Catalog à 3/37.");
            command("java -jar target/hai913i-tp1-analyzer.jar tp2 resources --cp 0.025 --linkage single --out target/demo-single > target/demo-single.txt && grep -E 'C1|  1 :|  2 :|B3' target/demo-single.txt && tail -9 target/demo-single.txt",0);
            chapter("C2 · Ajouter héritage et implémentation", "Quatre relations internes s'ajoutent aux 37 arcs : dénominateur 41, 22 arêtes. À seuil constant, huit modules : coupe rejetée.");
            command("java -jar target/hai913i-tp1-analyzer.jar tp2 resources --cp 0.025 --inheritance resources/facts-validation-inheritance.csv --out target/demo-enrichi > target/demo-enrichi.txt; code=$?; head -6 target/demo-enrichi.txt; tail -10 target/demo-enrichi.txt; exit $code",3);
            chapter("C2 · Recalibrer CP en déclarant le changement", "À CP = 0,020, la coupe enrichie donne sept modules acceptés. La comparaison à 0,025 reste fournie : les relations et l'échelle ont changé.");
            command("java -jar target/hai913i-tp1-analyzer.jar tp2 resources --cp 0.020 --inheritance resources/facts-validation-inheritance.csv --out target/demo-enrichi-020 | tail -10",0);
            chapter("C4 · Réextraction réelle du second projet par le TP1", "Les visiteurs JDT du TP1 analysent maintenant les 29 fichiers de l'analyseur. Le pont écrit les faits ; aucune table préexistante n'est utilisée ici.");
            command("sh scripts/extraire-tp1.sh resources/projects/analyseur-tp1 target/demo-extraction | tail -16",0);
            chapter("C4 · Comparer les modules du second système", "30 classes, 199 méthodes, 267 arcs ; 190 sont interclasses. À CP = 0,010, quinze modules respectent le plafond de quinze.");
            command("java -jar target/hai913i-tp1-analyzer.jar tp2 target/demo-extraction --cp 0.010 --out target/demo-second > target/demo-second.txt && head -5 target/demo-second.txt && tail -17 target/demo-second.txt",0);
            chapter("C4 · Cibles implicites réintégrées et effet du seuil", "Le pont conserve désormais 144 sites vers 69 méthodes implicites résolues : 121 arcs de plus. À 0,025, les 23 modules restent rejetés.");
            command("head -12 target/demo-extraction/cibles-reintegrees.csv; java -jar target/hai913i-tp1-analyzer.jar tp2 target/demo-extraction --cp 0.025 --out target/demo-second-rejet > target/demo-second-rejet.txt; code=$?; grep 'B3 |' target/demo-second-rejet.txt; exit $code",3);
            chapter("C3 · Explorer les mêmes calculs dans Swing", "La recherche et la sélection expliquent une classe ; changer CP recalcule la coupe. Les filtres d'affichage ne modifient pas les données.");
            var graph=new CouplingCalculator().compute(new CsvFactsSource(source.resolve("resources"),null).load()).graph();
            var model=new ExplorerModel(graph,.025,AgglomerativeClusterer.Linkage.AVERAGE,"calls","A0 | Faits canoniques de la bibliothèque\n");
            edt(()->{explorer=new ExplorerWindow(model);explorer.setBounds(30,35,1860,930);explorer.setVisible(true);explorer.toFront();});hand.pause(1500,2300);
            JTextField search=(JTextField)find(explorer,"search");hand.click(search);hand.type("Dvd",false);
            JList<?> classes=(JList<?>)find(explorer,"classes");Point[] item=new Point[1];
            edt(()->{Rectangle b=classes.getCellBounds(0,0);Point p=classes.getLocationOnScreen();item[0]=new Point(p.x+55,p.y+b.y+b.height/2);});
            hand.click(item[0].x,item[0].y);hand.pause(2000,3500);
            hand.click(search);hand.selectAll();hand.key(KeyEvent.VK_BACK_SPACE);
            editSpinner("cp","0.045",true);tab(2);hand.pause(2500,4000);
            editSpinner("cp","0.025",false);
            JComboBox<?> linkage=(JComboBox<?>)find(explorer,"linkage");hand.click(linkage);hand.key(KeyEvent.VK_HOME);hand.key(KeyEvent.VK_DOWN);hand.key(KeyEvent.VK_ENTER);hand.pause(350,650);
            if(linkage.getSelectedIndex()!=1)throw new IllegalStateException("lien simple non sélectionné");
            tab(1);JTree tree=(JTree)find(explorer,"dendrogram");Point[] branch=new Point[1];
            edt(()->{Rectangle b=tree.getRowBounds(1);Point p=tree.getLocationOnScreen();branch[0]=new Point(p.x+b.x+70,p.y+b.y+b.height/2);});
            hand.click(branch[0].x,branch[0].y);hand.pause(2300,3700);
            tab(0);editSpinner("edgeMinimum","0.050",false);hand.move(1020,620);hand.pause(1800,3000);
            chapter("C3 · Exporter la coupe courante", "Après changement de CP, le dialogue écrit un résumé et des paramètres à jour. La provenance de la relation est conservée.");
            editSpinner("cp","0.045",false);hand.pause(1700,2600);
            exportDialog(source.resolve("target/demo-export-gui"));hand.pause(1800,2700);edt(()->explorer.dispose());
            chapter("Export interactif vérifié et fin de démonstration", "Le résumé exporté est régénéré avec CP et la liaison courants. Les limites et la déclaration d'aide IA sont détaillées dans le rapport.");
            command("cat target/demo-export-gui/parametres.txt && grep 'B3 |' target/demo-export-gui/resultats.txt",0);
        } finally {
            recording.set(false);capture.join();timeline.close();notes.close();transcript.close();
            edt(()->{if(explorer!=null)explorer.dispose();if(analyzer!=null)analyzer.dispose();});nativeCommand("xdotool","windowclose",terminalWindow);terminal.destroy();
        }
        if(failure.get()!=null)throw new AssertionError("Échec de capture",failure.get());
        System.out.printf(Locale.ROOT,"Capture terminée : %.1f s%n",(System.nanoTime()-start)/1e9);
    }
}
