package cTree;

import cTree.node.LeafNode;
import lombok.AllArgsConstructor;
import lombok.Data;
import trace.TimedInput;
import utils.FastImmPair;

import java.util.Map;

@Data
@AllArgsConstructor
public class Track {

    private LeafNode source;
    private TimedInput input;
    private LeafNode target;
    private Map<FastImmPair<LeafNode, Boolean>, Integer> edges;

    public int hashCode() {
        return source.hashCode() + input.hashCode() + (target == null ? edges.hashCode() : target.hashCode());
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Track)) return false;

        Track other = (Track) o;
        if ((edges == null ^ other.edges == null) || (target == null ^ other.target == null)) {
            return false;
        }
        if (edges != null && !edges.equals(other.edges))
            return false;
        return source.equals(other.source) && input.equals(other.input);
    }

    public String toString() {
        StringBuilder str = new StringBuilder("d[");
        if (edges != null) {
            for (Map.Entry<FastImmPair<LeafNode, Boolean>, Integer> map : edges.entrySet()) {
                str.append(String.format("%s->%d,", map.getKey(), map.getValue()));
            }
            str.deleteCharAt(str.length() - 1).append("]");
        }
        return String.format("Track{source=%s, input=%s, edges=%s}", source, input, target == null ? str : "chaos");
    }
}
