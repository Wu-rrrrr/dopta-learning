package cTree.node;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public abstract class Node<T> {

    private T sequence;

    public boolean isLeaf() {
        return this instanceof LeafNode;
    }

    public boolean isInnerNode() {
        return this instanceof InnerNode;
    }

    @Override
    public String toString() {
        return sequence.toString();
    }
}
