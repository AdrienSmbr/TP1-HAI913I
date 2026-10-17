package hai913i.tp2;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.function.IntConsumer;
import java.lang.reflect.InvocationTargetException;

/** C3 : exploration interactive du même graphe, du même arbre et de la même coupe. */
public final class ExplorerWindow extends JFrame {
    private final ExplorerModel model;
    private final JTextField search = new JTextField(17);
    private final DefaultListModel<String> classItems = new DefaultListModel<>();
    private final JList<String> classes = new JList<>(classItems);
    private final JTextArea detail = new JTextArea();
    private final JTextArea moduleText = new JTextArea();
    private final JTree tree = new JTree();
    private final JLabel status = new JLabel();
    private final GraphCanvas graphCanvas = new GraphCanvas();
    private String selected;
    private Set<String> selectedGroup = Set.of();
    private double edgeMinimum;
    private static final Color[] COLORS = {
        new Color(0x12828b), new Color(0x647caa), new Color(0xbb7850), new Color(0x9371b8),
        new Color(0x539765), new Color(0xb55e69), new Color(0x3e83b6), new Color(0x8e8862)
    };
    private record TreeEntry(ClusterNode node) {
        @Override public String toString() {
            return node.isLeaf() ? Exports.shortName(node.members().get(0)) :
                "#" + node.id() + " · n=" + node.members().size() + " · S=" + String.format(Locale.ROOT, "%.5f", node.similarity());
        }
    }
    public static void open(ExplorerModel model, IntConsumer onClose) throws IOException {
        Runnable create = () -> {
            ExplorerWindow window = new ExplorerWindow(model);
            if (onClose != null) window.addWindowListener(new WindowAdapter() {
                @Override public void windowClosed(WindowEvent e) { onClose.accept(model.cut().accepted() ? 0 : 3); }
            });
            window.setVisible(true);
        };
        try {
            if (SwingUtilities.isEventDispatchThread()) create.run();
            else SwingUtilities.invokeAndWait(create);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); throw new IOException("ouverture graphique interrompue", e);
        } catch (InvocationTargetException | AWTError e) {
            throw new IOException("impossible d'ouvrir l'écran graphique : " + e.getMessage(), e);
        }
    }
    ExplorerWindow(ExplorerModel model) {
        super("HAI913I · TP2 — Explorateur de modules"); this.model = model;
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setSize(1360, 880); setMinimumSize(new Dimension(1000, 650)); setLocationByPlatform(true);
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        controls.add(new JLabel("CP :"));
        JSpinner cp = new JSpinner(new SpinnerNumberModel(model.cp(), 0.0, 1.0, 0.001));
        cp.setName("cp"); cp.setEditor(new JSpinner.NumberEditor(cp, "0.000")); controls.add(cp);
        cp.addChangeListener(e -> { model.setCp(((Number)cp.getValue()).doubleValue()); refreshCut(); });
        controls.add(new JLabel("Liaison :"));
        JComboBox<String> linkage = new JComboBox<>(new String[]{"Lien moyen", "Lien simple"});
        linkage.setName("linkage"); linkage.setSelectedIndex(model.linkage() == AgglomerativeClusterer.Linkage.AVERAGE ? 0 : 1);
        controls.add(linkage);
        linkage.addActionListener(e -> {
            model.setLinkage(linkage.getSelectedIndex() == 0 ? AgglomerativeClusterer.Linkage.AVERAGE : AgglomerativeClusterer.Linkage.SINGLE);
            selected = null; selectedGroup = Set.of(); classes.clearSelection();
            detail.setText("Liaison modifiée. Sélectionner une classe ou une branche du nouvel arbre.");
            refreshTree(); refreshCut();
        });
        controls.add(new JLabel("Arêtes ≥"));
        JSpinner minimum = new JSpinner(new SpinnerNumberModel(0.0, 0.0, 1.0, 0.005));
        minimum.setName("edgeMinimum"); minimum.setEditor(new JSpinner.NumberEditor(minimum, "0.000"));
        controls.add(minimum); minimum.addChangeListener(e -> { edgeMinimum=((Number)minimum.getValue()).doubleValue(); graphCanvas.repaint(); });
        JButton export = new JButton("Exporter la vue calculée…"); export.setName("export"); controls.add(export);
        export.addActionListener(e -> export());
        add(controls, BorderLayout.NORTH);
        JPanel left = new JPanel(new BorderLayout(5, 8)); left.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        JPanel searchBox = new JPanel(new BorderLayout(5, 5)); searchBox.add(new JLabel("Rechercher une classe :"),BorderLayout.NORTH);
        search.setName("search"); searchBox.add(search);left.add(searchBox,BorderLayout.NORTH);
        classes.setName("classes");classes.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        classes.setCellRenderer(new DefaultListCellRenderer() {
            @Override public Component getListCellRendererComponent(JList<?> list,Object value,int index,boolean selected,boolean focus) {
                JLabel label=(JLabel)super.getListCellRendererComponent(list,value,index,selected,focus);
                label.setText(Exports.shortName(value.toString()));label.setToolTipText(value.toString());return label;
            }
        });
        left.add(new JScrollPane(classes), BorderLayout.CENTER);
        search.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { filter(); }
            public void removeUpdate(DocumentEvent e) { filter(); }
            public void changedUpdate(DocumentEvent e) { filter(); }
        });
        classes.addListSelectionListener(e -> { if (!e.getValueIsAdjusting() && classes.getSelectedValue()!=null) selectClass(classes.getSelectedValue()); });
        JTabbedPane tabs = new JTabbedPane();tabs.setName("views");
        tabs.addTab("Graphe pondéré", new JScrollPane(graphCanvas));
        tree.setRootVisible(true);tree.setName("dendrogram");
        tree.addTreeSelectionListener(e -> {
            Object item = tree.getLastSelectedPathComponent();
            if (item instanceof DefaultMutableTreeNode t && t.getUserObject() instanceof TreeEntry entry) {
                ClusterNode n=entry.node();classes.clearSelection();selected=null;selectedGroup=Set.copyOf(n.members());
                detail.setText("Groupe #"+n.id()+"\nTaille : "+n.members().size()+"\nSimilarité de fusion : "+
                    (n.isLeaf()?"NA (feuille)":Exports.number(n.similarity()))+"\nMoyenne interne : "+
                    (n.isLeaf()?"NA (feuille)":Exports.number(model.graph().internalMean(n.members())))+"\n\n"+String.join("\n",n.members()));
                detail.setCaretPosition(0);graphCanvas.repaint();
            }
        });
        tabs.addTab("Dendrogramme",new JScrollPane(tree));
        moduleText.setEditable(false);moduleText.setFont(new Font(Font.MONOSPACED,Font.PLAIN,13));
        tabs.addTab("Modules",new JScrollPane(moduleText));
        detail.setName("detail");detail.setEditable(false);detail.setFont(new Font(Font.MONOSPACED,Font.PLAIN,12));
        detail.setText("Cliquer une classe du graphe ou de la liste.\nDans l'arbre, sélectionner une branche pour l'inspecter.\nLes couleurs correspondent à la coupe courante.\nLe filtre d'arêtes modifie seulement l'affichage.");
        JSplitPane vertical = new JSplitPane(JSplitPane.VERTICAL_SPLIT,tabs,new JScrollPane(detail));vertical.setResizeWeight(.78);
        JSplitPane body = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,left,vertical);body.setDividerLocation(220);add(body);
        status.setName("status");status.setBorder(BorderFactory.createEmptyBorder(8,12,8,12));add(status,BorderLayout.SOUTH);
        filter();refreshTree();refreshCut();
    }
    private void filter() {
        String keep=selected; classItems.clear();model.search(search.getText()).forEach(classItems::addElement);
        if(keep!=null && classItems.contains(keep))classes.setSelectedValue(keep,true);
        else if(keep!=null) { selected=null; detail.setText("Sélection masquée par la recherche. Sélectionner une classe visible."); }
        graphCanvas.repaint();
    }
    private void selectClass(String name) {
        tree.clearSelection();selected=name;selectedGroup=Set.of();
        StringBuilder b=new StringBuilder(name+"\nModule : "+model.assignment().get(name)+"\n\nCouplages non nuls :\n");
        int degree=0;
        for(var edge:model.graph().rankedEdges()) {
            var p=edge.getKey();
            if(p.a().equals(name)||p.b().equals(name)) {
                degree++;b.append("  ").append(p.a().equals(name)?p.b():p.a()).append(" : ").append(Exports.number(edge.getValue())).append('\n');
            }
        }
        if(degree==0)b.append("  Classe isolée pour la relation utilisée.\n");
        detail.setText(b.toString());detail.setCaretPosition(0);graphCanvas.repaint();
    }
    private DefaultMutableTreeNode branch(ClusterNode n) {
        DefaultMutableTreeNode item=new DefaultMutableTreeNode(new TreeEntry(n));
        if(!n.isLeaf()){item.add(branch(n.left()));item.add(branch(n.right()));}return item;
    }
    private void refreshTree() {
        tree.setModel(new DefaultTreeModel(branch(model.tree().root())));
        for(int row=0;row<tree.getRowCount();row++)tree.expandRow(row);
    }
    private void refreshCut() {
        var cut=model.cut();
        status.setText("CP="+model.cp()+"  |  "+cut.modules().size()+" modules / maximum "+cut.maximum()+"  |  "+
                (cut.accepted()?"COUPE ACCEPTÉE":"COUPE REJETÉE : contrainte M/2 violée")+
                "  |  "+model.graph().classes().size()+" classes, "+model.graph().edges().size()+" arêtes");
        status.setForeground(cut.accepted()?new Color(0x1c7040):new Color(0xb33826));
        StringBuilder b=new StringBuilder();int i=0;
        for(var m:cut.modules()) b.append("Module ").append(++i).append(" (#").append(m.node().id()).append(") n=")
            .append(m.node().members().size()).append(" C=").append(m.node().isLeaf()?"NA":Exports.number(m.internalMean()))
            .append('\n').append(String.join("\n",m.node().members())).append("\n\n");
        moduleText.setText(b.toString());moduleText.setCaretPosition(0);graphCanvas.repaint();
        if(selected!=null)selectClass(selected);
    }
    private void export() {
        JFileChooser chooser=new JFileChooser();chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if(chooser.showSaveDialog(this)!=JFileChooser.APPROVE_OPTION)return;
        try {
            var path=chooser.getSelectedFile().toPath();model.export(path);
            JOptionPane.showMessageDialog(this,"Exports écrits dans "+path);
        }catch(IOException e){JOptionPane.showMessageDialog(this,e.getMessage(),"Erreur d'export",JOptionPane.ERROR_MESSAGE);}
    }
    private final class GraphCanvas extends JPanel {
        private final Map<String,Point2D.Double> positions=new TreeMap<>();
        GraphCanvas() {
            setPreferredSize(new Dimension(980,540));setBackground(Color.WHITE);setToolTipText("");
            addMouseListener(new MouseAdapter(){@Override public void mouseClicked(MouseEvent e){
                String c=hit(e.getPoint());if(c!=null){if(!classItems.contains(c))search.setText("");selectClass(c);classes.setSelectedValue(c,true);}
            }});
        }
        private String hit(Point p) {
            return positions.entrySet().stream().filter(e->e.getValue().distance(p)<22)
                .min(Comparator.comparingDouble(e->e.getValue().distance(p))).map(Map.Entry::getKey).orElse(null);
        }
        @Override public String getToolTipText(MouseEvent e) { return hit(e.getPoint()); }
        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);if(model==null)return;
            Graphics2D d=(Graphics2D)g.create();d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            var names=model.graph().classes();positions.clear();double cx=getWidth()/2.0,cy=getHeight()/2.0;
            double rx=Math.max(100,cx-125),ry=Math.max(100,cy-65);
            for(int i=0;i<names.size();i++){double a=2*Math.PI*i/names.size()-Math.PI/2;positions.put(names.get(i),new Point2D.Double(cx+rx*Math.cos(a),cy+ry*Math.sin(a)));}
            Set<String> visible=new HashSet<>(model.search(search.getText()));
            for(var e:model.graph().edges().entrySet()) {
                if(e.getValue()<edgeMinimum)continue;var p=e.getKey();
                boolean focus=selected!=null&&(p.a().equals(selected)||p.b().equals(selected));
                d.setColor(focus?new Color(0x245d7c):new Color(0xd8e2ea));d.setStroke(new BasicStroke(focus?3:1));
                var a=positions.get(p.a());var b=positions.get(p.b());d.draw(new Line2D.Double(a,b));
                if(focus){d.setColor(new Color(0x17384a));d.drawString(String.format(Locale.ROOT,"%.4f",e.getValue()),(float)((a.x+b.x)/2),(float)((a.y+b.y)/2));}
            }
            var assignment=model.assignment();d.setFont(new Font(Font.SANS_SERIF,Font.PLAIN,12));
            for(String c:names){var p=positions.get(c);boolean focus=c.equals(selected)||selectedGroup.contains(c);
                d.setColor(visible.contains(c)?COLORS[(assignment.get(c)-1)%COLORS.length]:new Color(0xdde2e5));
                d.fill(new Ellipse2D.Double(p.x-11,p.y-11,22,22));
                if(focus){d.setColor(new Color(0x172d3e));d.setStroke(new BasicStroke(3));d.draw(new Ellipse2D.Double(p.x-15,p.y-15,30,30));}
                String label=Exports.shortName(c);int w=d.getFontMetrics().stringWidth(label);
                d.setColor(new Color(0x263f51));d.drawString(label,(float)(p.x-w/2.0),(float)(p.y+29));
            }
            d.setColor(new Color(0x506777));d.drawString("Couleurs : modules courants · cliquer un nœud pour lire ses poids · isolés conservés",18,22);d.dispose();
        }
    }
}
