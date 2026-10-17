package hai913i.tp2;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Point2D;
import java.util.Random;
import javax.swing.*;

/** Gestes automatisés à cadence variable ; corrections de saisie scénarisées.
 * Aucun de ces gestes ne constitue une preuve d'intervention humaine.
 */
public final class NaturalInput {
    private final Robot robot = new Robot();
    private final Random random = new Random(20261016L);
    public NaturalInput() throws AWTException { robot.setAutoDelay(2); }
    public void pause(int min,int max) throws InterruptedException {Thread.sleep(min+random.nextInt(max-min+1));}
    public void key(int key) {robot.keyPress(key);robot.keyRelease(key);}
    public void selectAll() {robot.keyPress(KeyEvent.VK_CONTROL);key(KeyEvent.VK_A);robot.keyRelease(KeyEvent.VK_CONTROL);}
    public void move(int x,int y) throws InterruptedException {
        Point start=MouseInfo.getPointerInfo().getLocation();double dx=x-start.x,dy=y-start.y;
        double bend=(random.nextDouble()-.5)*Math.min(180,Math.hypot(dx,dy)/3);
        Point2D.Double control=new Point2D.Double((start.x+x)/2.0-dy*.08+bend,(start.y+y)/2.0+dx*.06);
        int steps=18+random.nextInt(16);
        for(int i=1;i<=steps;i++){
            double t=(double)i/steps;t=t*t*(3-2*t);double u=1-t;
            int xx=(int)Math.round(u*u*start.x+2*u*t*control.x+t*t*x);
            int yy=(int)Math.round(u*u*start.y+2*u*t*control.y+t*t*y);
            robot.mouseMove(xx,yy);pause(9,23);
            if(i==steps/2 && random.nextInt(4)==0)pause(90,230);
        }
        if(random.nextInt(4)==0){robot.mouseMove(x+random.nextInt(7)-3,y+random.nextInt(7)-3);pause(50,110);}
        robot.mouseMove(x,y);pause(100,230);
    }
    public void click(Component component) throws Exception {
        Point[] p=new Point[1];SwingUtilities.invokeAndWait(()->{Point q=component.getLocationOnScreen();p[0]=new Point(q.x+Math.min(component.getWidth()/2,120),q.y+component.getHeight()/2);});
        click(p[0].x,p[0].y);
    }
    public void click(int x,int y) throws Exception {
        move(x,y);robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);pause(55,130);robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);pause(100,260);
    }
    public void character(char c) {
        boolean shift=false;int code;
        String unshifted="`1234567890-=[]\\;',./";
        String shifted="~!@#$%^&*()_+{}|:\"<>?";
        int index=shifted.indexOf(c);
        if(index>=0){c=unshifted.charAt(index);shift=true;}
        else if(Character.isUpperCase(c)){c=Character.toLowerCase(c);shift=true;}
        code=KeyEvent.getExtendedKeyCodeForChar(c);
        if(code==KeyEvent.VK_UNDEFINED)throw new IllegalArgumentException("caractère non pris en charge par le scénario clavier : "+c);
        if(shift)robot.keyPress(KeyEvent.VK_SHIFT);key(code);if(shift)robot.keyRelease(KeyEvent.VK_SHIFT);
    }
    public void type(String text,boolean correction) throws Exception {
        int mistake=correction?Math.min(text.length()-2,Math.max(8,text.length()/3)):-1;
        for(int i=0;i<text.length();i++){
            if(i==mistake){character('x');pause(280,570);key(KeyEvent.VK_BACK_SPACE);pause(100,240);}
            character(text.charAt(i));pause(23,65);
            if(text.charAt(i)==' ' && random.nextInt(7)==0)pause(130,430);
            if(i>0 && i%95==0)pause(260,650);
        }
        pause(250,650);
    }
}
