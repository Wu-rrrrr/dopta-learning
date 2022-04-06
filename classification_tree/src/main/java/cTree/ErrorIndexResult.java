package cTree;

import cTree.node.LeafNode;
import lombok.AllArgsConstructor;
import lombok.Data;
import trace.ResetTimedSuffixTrace;
import trace.TimedInput;
import trace.TimedOutput;
import trace.TimedSuffixTrace;
import utils.FastImmPair;

@AllArgsConstructor
@Data
public class ErrorIndexResult {

//    private int index;
    private LeafNode source;
    private FastImmPair<TimedInput, TimedOutput> pair;
    private LeafNode target;
    private TimedSuffixTrace suffix;
    private ErrorEnum error;
}
