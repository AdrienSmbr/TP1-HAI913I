package hai913i.tp2;

import javax.swing.*;
import java.awt.*;
import java.nio.file.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import javax.imageio.ImageIO;

/** Vérification de la fenêtre réelle et capture écran continue via Robot. */
public final class GuiSmoke {
    private static ExplorerWindow window;
    private static Component find(Container parent,String name) {
        for(Component c:parent.getComponents()) {
            if(name.equals(c.getName()))return c;
            if(c instanceof Container nested){Component found=find(nested,name);if(found!=null)return found;}
        }
        return null;
    }
    private static <T> T findType(Container parent, Class<T> type) {
        for(Component c:parent.getComponents()) {
            if(type.isInstance(c))return type.cast(c);
            if(c instanceof Container nested){T found=findType(nested,type);if(found!=null)return found;}
        }
        return null;
    }
    private static void exportThroughDialog(Path path) throws Exception {
        action(() -> {
            javax.swing.Timer timer=new javax.swing.Timer(200,null);
            timer.addActionListener(e -> {
                for(Window w:Window.getWindows())if(w instanceof JDialog d && d.isShowing()) {
                    JFileChooser chooser=findType(d,JFileChooser.class);
                    if(chooser!=null){chooser.setSelectedFile(path.toAbsolutePath().toFile());chooser.approveSelection();return;}
                    if(findType(d,JOptionPane.class)!=null){d.dispose();timer.stop();return;}
                }
            });
            timer.start();((JButton)find(window,"export")).doClick();
        });
        action(()->{});
    }
    private static void action(Runnable run) throws Exception { SwingUtilities.invokeAndWait(run); }
    private static void check(boolean value,String message) { if(!value)throw new AssertionError(message); }
    public static void main(String[] args) throws Exception {
        if(GraphicsEnvironment.isHeadless())throw new IllegalStateException("Un écran est nécessaire");
        Path captures=Path.of(args.length==0?"target/gui-captures":args[0]);Files.createDirectories(captures);
        var graph=new CouplingCalculator().compute(new CsvFactsSource(Path.of("resources"),null).load()).graph();
        action(()->{window=new ExplorerWindow(new ExplorerModel(graph,.025,AgglomerativeClusterer.Linkage.AVERAGE));window.setLocation(60,60);window.setVisible(true);window.toFront();});
        AtomicBoolean recording=new AtomicBoolean(true);
        AtomicReference<Throwable> captureFailure=new AtomicReference<>();
        Thread capture=new Thread(()->{
            try {
                Robot robot=new Robot();int frame=0;
                while(recording.get()){
                    Rectangle[] bounds=new Rectangle[1];action(()->bounds[0]=window.getBounds());
                    ImageIO.write(robot.createScreenCapture(bounds[0]),"png",captures.resolve(String.format("frame-%04d.png",frame++)).toFile());
                    Thread.sleep(200);
                }
            } catch(Exception e){captureFailure.set(e);}
        });capture.start();
        try {
            Thread.sleep(2500);
            action(()->check(((JLabel)find(window,"status")).getText().contains("7 modules"),"coupe initiale"));
            ImageIO.write(new Robot().createScreenCapture(window.getBounds()),"png",captures.resolve("initial.png").toFile());
            action(()->((JTextField)find(window,"search")).setText("Dvd"));Thread.sleep(1800);
            action(()->{
                JList<?> list=(JList<?>)find(window,"classes");check(list.getModel().getSize()==1,"filtre de recherche");
                list.setSelectedIndex(0);check(list.getSelectedValue().toString().equals("library.model.Dvd"),"sélection Dvd");
            });Thread.sleep(2000);
            ImageIO.write(new Robot().createScreenCapture(window.getBounds()),"png",captures.resolve("recherche.png").toFile());
            action(()->{((JTextField)find(window,"search")).setText("");((JSpinner)find(window,"cp")).setValue(.045);});Thread.sleep(2300);
            action(()->check(((JLabel)find(window,"status")).getText().contains("REJETÉE"),"rejet interactif"));
            ImageIO.write(new Robot().createScreenCapture(window.getBounds()),"png",captures.resolve("rejet.png").toFile());
            Path export=captures.resolve("export-rejet");exportThroughDialog(export);
            String summary=Files.readString(export.resolve("resultats.txt"));
            check(summary.contains("COUPE REJETÉE") && summary.contains("CP=0.045"),"export du résumé à jour");
            check(Files.readString(export.resolve("parametres.txt")).contains("cp=0.045"),"export des paramètres");
            action(()->((JTabbedPane)find(window,"views")).setSelectedIndex(2));Thread.sleep(2300);
            action(()->{((JSpinner)find(window,"cp")).setValue(.025);((JComboBox<?>)find(window,"linkage")).setSelectedIndex(1);});Thread.sleep(2300);
            action(()->{((JTabbedPane)find(window,"views")).setSelectedIndex(1);((JTree)find(window,"dendrogram")).setSelectionRow(1);});Thread.sleep(2500);
            action(()->check(((JList<?>)find(window,"classes")).getSelectedIndex()==-1,"sélection de classe effacée pour la branche"));
            ImageIO.write(new Robot().createScreenCapture(window.getBounds()),"png",captures.resolve("arbre.png").toFile());
            action(()->{((JComboBox<?>)find(window,"linkage")).setSelectedIndex(0);((JTabbedPane)find(window,"views")).setSelectedIndex(0);((JSpinner)find(window,"edgeMinimum")).setValue(.05);});Thread.sleep(2300);
            action(()->check(((JSpinner)find(window,"edgeMinimum")).getValue().equals(.05),"filtre d'arêtes"));
            action(()->check(((JTextArea)find(window,"detail")).getText().contains("Liaison modifiée"),"inspection ancienne effacée après changement de liaison"));
            System.out.println("GUI CONFORME : recherche, sélection, changement de CP, rejet, liaison, arbre, filtre d'arêtes et dialogue réel d'export avec résumé à jour.");
        } finally {recording.set(false);capture.join();action(()->window.dispose());}
        if(captureFailure.get()!=null)throw new AssertionError("échec capture écran",captureFailure.get());
    }
}
