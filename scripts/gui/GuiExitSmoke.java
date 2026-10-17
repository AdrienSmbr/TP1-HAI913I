package hai913i.tp2;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowEvent;

/** Processus de vérification du code CLI à la fermeture, après changement de CP. */
public final class GuiExitSmoke {
    private static Component find(Container parent) {
        for(Component c:parent.getComponents()){
            if("cp".equals(c.getName()))return c;
            if(c instanceof Container nested){var found=find(nested);if(found!=null)return found;}
        }return null;
    }
    public static void main(String[] args) {
        double finalCp=Double.parseDouble(args[0]);
        Timer close=new Timer(500,null);
        close.addActionListener(e->{
            for(Frame f:Frame.getFrames())if(f.isShowing() && f.getTitle().contains("Explorateur")){
                ((JSpinner)find(f)).setValue(finalCp);close.stop();
                f.dispatchEvent(new WindowEvent(f,WindowEvent.WINDOW_CLOSING));return;
            }
        });close.start();
        Main.main(new String[]{"resources","--cp","0.025","--gui","--out","target/gui-exit-check"});
    }
}
