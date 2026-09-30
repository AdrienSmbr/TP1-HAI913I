package hai913i.tp1.report;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import hai913i.tp1.callgraph.CallGraph;

public final class DotReport {

    private DotReport() {
    }

    public static void write(PrintStream out, CallGraph graph, String packageFilter) {
        out.println("digraph callgraph {");
        out.println("  rankdir=LR;");
        out.println("  node [shape=box, style=rounded, fontname=\"Helvetica\", fontsize=9];");
        out.println("  edge [fontname=\"Helvetica\", fontsize=8];");

        Map<String, List<String>> byOwner = new LinkedHashMap<>();
        for (String node : graph.nodes()) {
            if (matches(node, packageFilter)) {
                byOwner.computeIfAbsent(owner(node), key -> new ArrayList<>()).add(node);
            }
        }

        int cluster = 0;
        for (Map.Entry<String, List<String>> group : byOwner.entrySet()) {
            out.println("  subgraph cluster_" + cluster++ + " {");
            out.println("    label=\"" + group.getKey() + "\";");
            out.println("    style=dashed; color=gray60; fontsize=10;");
            for (String node : group.getValue()) {
                out.println("    \"" + node + "\" [label=\"" + shortLabel(node) + "\"];");
            }
            out.println("  }");
        }

        for (Map.Entry<String, Map<String, Integer>> source : graph.outgoing().entrySet()) {
            if (!matches(source.getKey(), packageFilter)) {
                continue;
            }
            for (Map.Entry<String, Integer> target : source.getValue().entrySet()) {
                if (!matches(target.getKey(), packageFilter)) {
                    continue;
                }
                String weight = target.getValue() > 1 ? " [label=\"" + target.getValue() + "\"]" : "";
                out.println("  \"" + source.getKey() + "\" -> \"" + target.getKey() + "\"" + weight + ";");
            }
        }
        out.println("}");
    }

    private static boolean matches(String nodeId, String packageFilter) {
        return packageFilter == null || nodeId.startsWith(packageFilter);
    }

    private static String owner(String nodeId) {
        int hash = nodeId.indexOf('#');
        return hash < 0 ? nodeId : nodeId.substring(0, hash);
    }

    private static String shortLabel(String nodeId) {
        int hash = nodeId.indexOf('#');
        String signature = hash < 0 ? nodeId : nodeId.substring(hash + 1);
        int open = signature.indexOf('(');
        if (open < 0) {
            return signature;
        }
        String name = signature.substring(0, open);
        String parameters = signature.substring(open + 1, signature.length() - 1);
        int arity = parameters.isEmpty() ? 0 : parameters.split(",").length;
        return name + "/" + arity;
    }
}
