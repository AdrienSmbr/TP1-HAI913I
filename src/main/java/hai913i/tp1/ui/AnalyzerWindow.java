package hai913i.tp1.ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;

import hai913i.tp1.callgraph.CallGraph;
import hai913i.tp1.callgraph.CallGraphBuilder;
import hai913i.tp1.extract.FactExtractor;
import hai913i.tp1.metrics.MetricsService;
import hai913i.tp1.model.CallFact;
import hai913i.tp1.model.MethodFact;
import hai913i.tp1.model.ProjectFacts;
import hai913i.tp1.model.TypeFact;
import hai913i.tp1.parse.JdtParser;
import hai913i.tp1.parse.ProjectSources;
import hai913i.tp1.report.TextReport;

/**
 * Interface graphique (bonus C2).
 *
 * <p>Ce n'est qu'une seconde facon d'afficher les resultats du <b>meme</b>
 * modele de faits : la fenetre appelle exactement les memes composants que la
 * ligne de commande ({@code FactExtractor}, {@code MetricsService},
 * {@code CallGraphBuilder}) et ne calcule rien elle-meme.</p>
 */
public class AnalyzerWindow extends JFrame {

    private static final long serialVersionUID = 1L;
    private static final Font MONO = new Font(Font.MONOSPACED, Font.PLAIN, 12);

    private final JTextField projectField = new JTextField(34);
    private final JSpinner thresholdField = new JSpinner(new SpinnerNumberModel(5, 0, 999, 1));
    private final JLabel status = new JLabel("Choisissez un projet puis lancez l'analyse.");

    private final DefaultTableModel structureModel = readOnlyModel(
            "Type", "Genre", "Paquetage", "Superclasses", "Interfaces", "Attributs", "Methodes");
    private final DefaultTableModel callsModel = readOnlyModel(
            "Methode appelante", "Ligne", "Appel", "Receveur (type statique)", "Cible", "Portee");
    private final DefaultTableModel arcsModel = readOnlyModel("Source", "Cible", "Poids");
    private final JTextArea metricsArea = new JTextArea();
    private final JTextArea neighboursArea = new JTextArea();
    private final JComboBox<String> methodChooser = new JComboBox<>();

    private CallGraph graph;

    public AnalyzerWindow(String initialProject) {
        super("HAI913I TP1 - Analyseur statique");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        projectField.setText(initialProject == null ? "" : initialProject);
        if (projectField.getText().isBlank()) {
            status.setText("Cliquez sur « Parcourir... » pour choisir un projet Java a analyser.");
        }

        add(toolbar(), BorderLayout.NORTH);
        add(tabs(), BorderLayout.CENTER);
        add(status, BorderLayout.SOUTH);

        setSize(1150, 700);
        setLocationRelativeTo(null);
    }

    private JPanel toolbar() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        panel.add(new JLabel("Projet :"));
        panel.add(projectField);

        JButton browse = new JButton("Parcourir...");
        browse.addActionListener(event -> chooseDirectory());
        panel.add(browse);

        panel.add(new JLabel("   Seuil X :"));
        panel.add(thresholdField);

        JButton analyse = new JButton("Analyser");
        analyse.addActionListener(event -> analyse());
        panel.add(analyse);
        return panel;
    }

    private JTabbedPane tabs() {
        metricsArea.setFont(MONO);
        metricsArea.setEditable(false);
        neighboursArea.setFont(MONO);
        neighboursArea.setEditable(false);

        JPanel graphPanel = new JPanel(new BorderLayout());
        JPanel query = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        query.add(new JLabel("Methode :"));
        methodChooser.setPreferredSize(new java.awt.Dimension(560, 26));
        query.add(methodChooser);
        JButton callees = new JButton("Appelees");
        callees.addActionListener(event -> showNeighbours(false));
        JButton callers = new JButton("Appelantes");
        callers.addActionListener(event -> showNeighbours(true));
        query.add(callees);
        query.add(callers);
        graphPanel.add(query, BorderLayout.NORTH);
        graphPanel.add(new JScrollPane(new JTable(arcsModel)), BorderLayout.CENTER);
        graphPanel.add(new JScrollPane(neighboursArea), BorderLayout.SOUTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Structure (A2)", new JScrollPane(new JTable(structureModel)));
        tabs.addTab("Appels (A3)", new JScrollPane(new JTable(callsModel)));
        tabs.addTab("Metriques (B2)", new JScrollPane(metricsArea));
        tabs.addTab("Graphe d'appel (B3)", graphPanel);
        return tabs;
    }

    private void chooseDirectory() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            projectField.setText(chooser.getSelectedFile().getAbsolutePath());
        }
    }

    private void analyse() {
        try {
            ProjectSources sources = ProjectSources.of(Path.of(projectField.getText().trim()));
            if (sources.isEmpty()) {
                fail("Aucun fichier .java sous " + sources.sourceRoot());
                return;
            }
            ProjectFacts facts = FactExtractor.extract(JdtParser.parse(sources, List.of()));
            graph = CallGraphBuilder.build(facts);

            fillStructure(facts);
            fillCalls(facts);
            fillMetrics(facts);
            fillGraph(facts);

            status.setText(facts.types().size() + " types  |  " + facts.methodCount()
                    + " methodes  |  " + facts.fieldCount() + " attributs  |  "
                    + facts.callCount() + " appels (" + facts.internalCallCount() + " internes, "
                    + facts.externalCallCount() + " externes, "
                    + facts.unresolvedCallCount() + " non resolus)  |  graphe : "
                    + graph.nodeCount() + " noeuds, " + graph.arcCount() + " arcs, "
                    + graph.siteCount() + " sites");
        } catch (IllegalArgumentException | IOException e) {
            fail(e.getMessage());
        }
    }

    private void fillStructure(ProjectFacts facts) {
        structureModel.setRowCount(0);
        for (TypeFact type : facts.types()) {
            structureModel.addRow(new Object[] {
                    type.qualifiedName(), type.kind(), type.packageName(),
                    String.join(", ", type.superclasses()),
                    String.join(", ", type.interfaces()),
                    type.fields().size(), type.methods().size()});
        }
    }

    private void fillCalls(ProjectFacts facts) {
        callsModel.setRowCount(0);
        for (CallFact call : facts.calls()) {
            callsModel.addRow(new Object[] {
                    call.callerId(), call.sourceLine(), call.calledName(),
                    call.staticReceiverType(),
                    call.targetId().orElse("NON RESOLUE"),
                    call.resolved() ? (call.internal() ? "interne" : "externe") : "non resolu"});
        }
    }

    private void fillMetrics(ProjectFacts facts) throws IOException {
        int threshold = (Integer) thresholdField.getValue();
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try (PrintStream out = new PrintStream(buffer, true, StandardCharsets.UTF_8)) {
            TextReport.metrics(out, MetricsService.compute(facts, threshold));
        }
        metricsArea.setText(buffer.toString(StandardCharsets.UTF_8));
        metricsArea.setCaretPosition(0);
    }

    private void fillGraph(ProjectFacts facts) {
        arcsModel.setRowCount(0);
        for (Map.Entry<String, Map<String, Integer>> source : graph.outgoing().entrySet()) {
            for (Map.Entry<String, Integer> target : source.getValue().entrySet()) {
                arcsModel.addRow(new Object[] {source.getKey(), target.getKey(), target.getValue()});
            }
        }
        methodChooser.setModel(new DefaultComboBoxModel<>(
                facts.types().stream()
                        .flatMap(type -> type.methods().stream())
                        .map(MethodFact::id)
                        .sorted()
                        .toArray(String[]::new)));
        neighboursArea.setText("");
    }

    private void showNeighbours(boolean callers) {
        if (graph == null || methodChooser.getSelectedItem() == null) {
            return;
        }
        String id = (String) methodChooser.getSelectedItem();
        Map<String, Integer> neighbours = callers ? graph.callers(id) : graph.callees(id);
        StringBuilder text = new StringBuilder(callers ? "Appelantes de " : "Appelees par ")
                .append(id).append("\n");
        if (neighbours.isEmpty()) {
            text.append("    (aucune)\n");
        } else {
            neighbours.forEach((neighbour, weight) ->
                    text.append("    ").append(neighbour).append("  (poids ").append(weight).append(")\n"));
        }
        neighboursArea.setText(text.toString());
        neighboursArea.setCaretPosition(0);
    }

    private void fail(String message) {
        status.setText("Erreur : " + message);
        JOptionPane.showMessageDialog(this, message, "Erreur", JOptionPane.ERROR_MESSAGE);
    }

    private static DefaultTableModel readOnlyModel(String... columns) {
        return new DefaultTableModel(columns, 0) {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    }

    public static void launch(String initialProject) {
        SwingUtilities.invokeLater(() -> new AnalyzerWindow(initialProject).setVisible(true));
    }
}
