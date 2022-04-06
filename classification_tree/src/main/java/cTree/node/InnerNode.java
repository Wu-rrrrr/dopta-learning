package cTree.node;

import base.learner.Answer;
import lombok.Data;
import lombok.EqualsAndHashCode;
import trace.TimedSuffixTrace;

import java.util.*;

@EqualsAndHashCode(callSuper = true)
@Data
public class InnerNode extends Node<TimedSuffixTrace>{

    private Node preNode;
    private Map<Answer, Node> keyChildMap;

    public InnerNode(TimedSuffixTrace sequence) {
        super(sequence);
        keyChildMap = new HashMap<>();
    }

    public void add(Answer key, Node node) {
        if (keyChildMap == null) {
            keyChildMap = new HashMap<>();
        }
        keyChildMap.put(key, node);
    }

    public Node getChild(Answer key) {
        Node node = keyChildMap.get(key);
        return node;
    }

    public void removeLeafNode(LeafNode leafNode) {
        Answer key = null;
        for (Map.Entry<Answer, Node> child : keyChildMap.entrySet()) {
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
        LinkedList<InnerNode> queue = new LinkedList<>();
        queue.add(this);
        while (!queue.isEmpty()) {
            InnerNode current = queue.removeFirst();
            for (Node child : current.keyChildMap.values()) {
                if (child.isLeaf())
                    offspring.add((LeafNode) child);
                else
                    queue.add((InnerNode) child);
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
