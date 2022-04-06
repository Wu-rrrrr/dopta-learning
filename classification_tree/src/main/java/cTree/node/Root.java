package cTree.node;

import automaton.Output;

import java.util.HashMap;
import java.util.Map;


public class Root extends Node {
    private Map<Output, Node> children;

    public Root() {
        children = new HashMap<>();
    }

    public void put(Output lastOutput, Node child) {
        children.put(lastOutput, child);
    }

    public Node get(Output key) {
        return children.get(key);
    }

    public Map<Output, Node> getChildren() {
        return children;
    }

    public String toString() {
        return "root";
    }
}
