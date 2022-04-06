package cTree;

import base.learner.Answer;
import cTree.node.InnerNode;
import cTree.node.Node;
import org.apache.commons.lang3.tuple.Pair;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.Map;

public class Exporter {

    public String stateString(int index, Object trace) {
        return String.format("%s [shape=\"circle\" margin=0 label=%s];", index, trace);
    }

    public String toDot(ClassificationTree tree) {
        int index = 1;
        StringBuilder sb = new StringBuilder();
        appendLine(sb, "digraph g {");
        LinkedList<Pair<Node, Integer>> queue = new LinkedList<>();
        queue.add(Pair.of(tree.getRoot(), index));
        while (!queue.isEmpty()) {
            Pair<Node, Integer> cur = queue.removeFirst();
            Node curNode = cur.getKey();
            int curIndex = cur.getValue();
            appendLine(sb, stateString(curIndex, curNode.getSequence()));
            if (curNode.isInnerNode()) {
                InnerNode innerNode = (InnerNode) curNode;
                for (Map.Entry<Answer, Node> child : innerNode.getKeyChildMap().entrySet()) {
                    index++;
                    appendLine(sb, transitionString(curIndex, child.getKey(), index));
                    queue.add(Pair.of(child.getValue(), index));
                }
            }
        }

        appendLine(sb, "}");
        return sb.toString();
    }

    private String transitionString(int source, Answer key, int target) {
        return String.format("%s -> %s [label=\"%s\"];", source,
                target, key);
    }

    public static void appendLine(StringBuilder sb, String line) {
        sb.append(line);
        sb.append(System.lineSeparator());
    }

    public void writeToFile(ClassificationTree tree, String fileName) throws IOException {
        File f = new File(fileName);
        f.getParentFile().mkdirs();
        try (FileWriter fw = new FileWriter(f)) {
            BufferedWriter bw = new BufferedWriter(fw);
            bw.write(toDot(tree));
            bw.flush();
        }
        generateGraph(fileName);
    }

    public void generateGraph(String fileName) throws IOException {
        Runtime run = Runtime.getRuntime();
        run.exec(String.format("dot %s -T pdf -o %s", fileName, fileName.replace(".dot", ".pdf")));
    }
}
