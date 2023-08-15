package cTree.node;

import automaton.OutputDistribution;
import base.learner.Answer;
import lombok.Data;
import trace.TimedSuffixTrace;

import java.util.*;

@Data
public class ExactInnerNode extends Node<TimedSuffixTrace> {
    private Node preNode;
    private Map<OutputDistribution, Node> keyChildMap;

    public ExactInnerNode(TimedSuffixTrace sequence) {
        super(sequence);
        keyChildMap = new HashMap<>();
    }

    public void add(OutputDistribution key, Node node) {
        if (keyChildMap == null) {
            keyChildMap = new HashMap<>();
        }
        keyChildMap.put(key, node);
    }

    public Node getChild(OutputDistribution key) {
        return keyChildMap.get(key);
    }

    public void removeLeafNode(LeafNode leafNode) {
        OutputDistribution key = null;
        for (Map.Entry<OutputDistribution, Node> child : keyChildMap.entrySet()) {
            if (child.getValue() == leafNode) {
                key = child.getKey();
                break;
            }
        }
        if (key == null) {
            System.out.println("child not find");
            System.exit(0);
        }
        if (keyChildMap.get(key) == null) {
            System.exit(0);
        }
        keyChildMap.remove(key);
    }

    public List<LeafNode> getAllOffspring() {
        List<LeafNode> offspring = new ArrayList<>();
        LinkedList<ExactInnerNode> queue = new LinkedList<>();
        queue.add(this);
        while (!queue.isEmpty()) {
            ExactInnerNode current = queue.removeFirst();
            for (Node child : current.getKeyChildMap().values()) {
                if (child.isLeaf())
                    offspring.add((LeafNode) child);
                else
                    queue.add((ExactInnerNode) child);
            }
        }
        return offspring;
    }

    public List<Node> getChildList() {
        return new ArrayList<>(keyChildMap.values());
    }

    public String toString() {
        return super.toString();
    }
}
