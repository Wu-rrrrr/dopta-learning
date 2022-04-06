package cTree.node;

import automaton.Output;
import lombok.AllArgsConstructor;
import lombok.Data;
import trace.ResetTimedTrace;

@Data
@AllArgsConstructor
public class LeafNode extends Node<ResetTimedTrace> {

    private boolean init;
    private Output last;
    private Node preNode;
//    private List<Pair<ColumnTrace, TableCell>> path;

    public LeafNode(ResetTimedTrace sequence) {
        super(sequence);
        this.last = sequence.lastOutput();
    }

    public LeafNode(ResetTimedTrace sequence, boolean init) {
        super(sequence);
        this.last = sequence.lastOutput();
        this.init = init;
    }

    public LeafNode copy() {
        LeafNode copied = new LeafNode(this.getSequence(), this.init);
        copied.setPreNode(this.preNode);
        return copied;
    }

    public String toString() {
        return getSequence().toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LeafNode)) return false;
        if (!super.equals(o)) return false;

        LeafNode leafNode = (LeafNode) o;

        if (init != leafNode.init) return false;
        return last.equals(leafNode.last);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (init ? 1 : 0);
        result = 31 * result + last.hashCode();
        return result;
    }
}
