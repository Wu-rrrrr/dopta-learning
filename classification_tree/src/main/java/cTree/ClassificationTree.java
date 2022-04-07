package cTree;

import automaton.*;
import base.Compatibility;
import base.learner.Answer;
import base.learner.Learner;
import base.learner.LearningSetting;
import base.teacher.Teacher;
import base.teacher.oracle.PACEquivalence;
import cTree.node.*;
import lombok.Data;
import org.apache.commons.lang3.tuple.Triple;
import trace.*;
import util.export.DotExporter;
import utils.FastImmPair;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Data
public class ClassificationTree implements Learner {

    private Set<Input> inputs;
    private final Teacher teacher;
    private final Compatibility compChecker;
    private Output initOutput;

    private Root root;
    private LeafNode chaosNode;
    private LeafNode invalidNode;
    private Location chaosLocation;
    private Location invalidLocation;
    private Set<Track> tracks;
    private PTA hypothesis;

    private Map<Location, LeafNode> locationNodeMap;
    private Map<LeafNode, Location> nodeMdpStateMap;

    // store incomplete trace
    private Set<TimedIncompleteTrace> incompleteTraces;
    private Set<ResetTimedIncompleteTrace> ctxes;

    private double unambiguous;
    private int nrUnambiguousBreakPointAnalysis;

    public ClassificationTree(Set<Input> inputs, Compatibility compChecker,Teacher teacher) {
        this.inputs = inputs;
        this.teacher = teacher;
        this.compChecker = compChecker;
    }

    public void init(Output initOutput) {
        this.initOutput = initOutput;
        chaosNode = new LeafNode(ResetTimedTrace.empty(Output.chaos()));
        invalidNode = new LeafNode(ResetTimedTrace.empty(Output.invalid()));
        chaosLocation = Location.chaos(inputs);
        invalidLocation = Location.invalid(inputs);
        ctxes = new HashSet<>();
        unambiguous = 1.0;
        nrUnambiguousBreakPointAnalysis = 0;
        cTreeInit(this.initOutput);
    }

    public void writeTreeToFile(String fileName) throws IOException {
        Exporter exporter = new Exporter();
        exporter.writeToFile(this, fileName);
    }

    @Override
    public void learn(LearningSetting setting) throws IOException {
        init(teacher.getInitialOutput());
        refineCTree();

        PTA hypo = null;
        int rounds = 0;
        int count = 1;
        DotExporter dotExp = new DotExporter();
        do {
            System.out.println("Round " + rounds);
            if (rounds > 0 && rounds % (setting.getPrintFrequency()) == 0) {
                writeTreeToFile("hypotheses/tree_" + rounds + ".dot");
            }

            hypo = buildHypothesis();
            if (rounds > 0 && rounds % setting.getPrintFrequency() == 0) {
                dotExp.writeToFile(hypo, "hypotheses/hyp" + rounds + ".dot");
                if (setting.getRmlExp() != null)
                    setting.getRmlExp().toFile(hypo, "hypotheses/hyp" + rounds + ".prism");
            }
            rounds++;
            // strictly viewed, this check should come at the end of the loop, but this is more
            // efficient
            double cexProcessCompletelyRatio = ctxes.isEmpty() ? 1 : (double) nrUnambiguousBreakPointAnalysis / ctxes.size();
            if (cexProcessCompletelyRatio >= 1) {
                FastImmPair<Boolean, ResetTimedIncompleteTrace> eqResult = teacher.equivalenceQuery(count, hypo, portionOfUnambiguousRows());
                if (eqResult.left) {
                    break;
                }
                if (eqResult.right != null) {
                    count++;
                    ctxes.add(eqResult.right);
                }
            }
            processCtxUnderIncomplete();
            refineCTree();
        } while (true);

        dotExp.writeToFile(hypo, "hypotheses/hyp_final.dot");
        writeTreeToFile("hypotheses/tree_final.dot");
        if (setting.getRmlExp() != null)
            setting.getRmlExp().toFile(hypo, "hypotheses/hyp_final.prism");
        setting.setRounds(rounds);
        setting.setHypothesis(hypo);
    }

    @Override
    public void processCounterexample(ResetTimedIncompleteTrace counterExample) {
        ctxes.add(counterExample);
    }

    @Override
    public void show() {

    }

    @Override
    public boolean check(ResetTimedIncompleteTrace counterExample) {
        return false;
    }

    private void cTreeInit(Output initOutput) {
        incompleteTraces = new HashSet<>();
        tracks = new HashSet<>();
        root = new Root();
        LeafNode initLeaf = new LeafNode(ResetTimedTrace.empty(initOutput));
        root.put(initOutput, initLeaf);
        initLeaf.setInit(true);
        initLeaf.setPreNode(root);
        refineSymbolTrack(initLeaf);
    }

    public void refineSymbolTrack(LeafNode leafNode) {
        for (Input input : inputs) {
            buildTrack(leafNode, TimedInput.create(input));
        }
    }

    // 重筛分整棵分类树
    public void ensureConsistentWithObsTree() {
        int nrTrace = 0;
        int nrUnambiguousTraces = 0;

        // ① 复制旧的叶节点迁移
        List<Track> oldTracks = new ArrayList<>(tracks);
        oldTracks.sort(new Comparator<Track>() {
            @Override
            public int compare(Track o1, Track o2) {
                int sourceLengthComp = Integer.compare(o1.getSource().getSequence().length(), o2.getSource().getSequence().length());
                if (sourceLengthComp != 0)
                    return sourceLengthComp;
                else {
                    int sourceString = o1.getSource().getSequence().toString().compareTo(o2.getSource().getSequence().toString());
                    if (sourceString != 0)
                        return sourceString;
                    else
                        return o1.getInput().getInput().getSymbol().compareTo(o2.getInput().getInput().getSymbol());
                }
            }
        });

        // ② 重置叶节点迁移集合
        tracks = new HashSet<>();

        // 更新所有叶节点
        Map<LeafNode, LeafNode> leafNodeMap = new HashMap<>();
        for (LeafNode old : deleteAllLeaves()) {
            SiftResult siftResult = sift(old.getSequence());
            LeafNode latest = siftResult.getLeafNode();
            leafNodeMap.put(old, latest);
            if (siftResult.isUnambiguous()) {
                nrUnambiguousTraces++;
            }
            nrTrace++;
        }

        // ③ 重筛分所有叶节点迁移
        for (Track track : oldTracks) {
            LeafNode source = leafNodeMap.get(track.getSource());
            FastImmPair<Integer, Integer> ratio = buildTrack(source, track.getInput());
            nrUnambiguousTraces += ratio.left;
            nrTrace += ratio.right;
        }

        unambiguous = (double) nrUnambiguousTraces / nrTrace;
    }

    private List<LeafNode> deleteAllLeaves() {
        List<LeafNode> leaves = new ArrayList<>();
        for (LeafNode leaf : getLeaves()) {
            leaves.add(leaf);
            if (leaf.getPreNode() != root) {
                InnerNode parent = (InnerNode) leaf.getPreNode();
                parent.removeLeafNode(leaf);
            }
        }
        leaves.sort(new Comparator<LeafNode>() {
            @Override
            public int compare(LeafNode o1, LeafNode o2) {
                return Integer.compare(o1.getSequence().length(), o2.getSequence().length());
            }
        });
        return leaves;
    }

    public FastImmPair<Integer, Integer> buildTrack(LeafNode source, TimedInput timedInput) {
        int nrTrace = 0;
        int nrUnambiguousTraces = 0;
        Track track = new Track(source, timedInput, null, null);
        ResetTimedTrace prefix = source.getSequence();
        Answer answer = teacher.query(prefix, TimedSuffixTrace.empty(timedInput));
        if (!answer.isValid()) {
            track.setTarget(invalidNode);
        } else {
            if (answer.getFrequencies().isEmpty()) {
                track.setTarget(chaosNode);
            } else {
                Map<FastImmPair<LeafNode, Boolean>, Integer> edges = new HashMap<>();
                for (Map.Entry<TimedOutput, Integer> entry : answer.getFrequencies().entrySet()) {
                    ResetTimedTrace extension = prefix.append(FastImmPair.of(timedInput, entry.getKey()));
                    SiftResult siftResult = sift(extension);
                    LeafNode target = siftResult.getLeafNode();
                    edges.put(FastImmPair.of(target, entry.getKey().isReset()), entry.getValue());
                    if (siftResult.isUnambiguous()) {
                        nrUnambiguousTraces++;
                    }
                    nrTrace++;
                }
                track.setEdges(edges);
            }
            if (!answer.isComplete()) {
                incompleteTraces.add(new TimedIncompleteTrace(track.getSource().getSequence().convert(),
                        TimedSuffixTrace.empty(timedInput)));
            }
        }

        addTrack(track);
        return FastImmPair.of(nrUnambiguousTraces, nrTrace);
    }

    public void refineCTree() throws IOException {
        refineCTree(Optional.empty());
    }

    public List<TimedIncompleteTrace> findIncomplete() {
        Set<TimedIncompleteTrace> incompleteTraces = new HashSet<>(this.incompleteTraces);
        System.out.println("Incomplete traces: " + incompleteTraces.size());
        return new ArrayList<>(incompleteTraces);
    }

    public void refineCTree(Optional<List<TimedIncompleteTrace>> tracesToRefine) throws IOException {
        ensureConsistentWithObsTree();
//        System.out.println("update before");
//        buildHypothesis();

        List<TimedIncompleteTrace> traces = null;
        if (tracesToRefine.isEmpty()) {
            // 2. find incomplete entries
            traces = findIncomplete();
            if (traces.isEmpty())
                return;
            // System.out.println("Traces to incomplete " );
            // traces.forEach(t -> System.out.println(t));
        } else {
            traces = tracesToRefine.get();
        }
        teacher.refine(traces);

        ensureConsistentWithObsTree();
//        System.out.println("update after");
//        buildHypothesis();
    }

    public SiftResult sift (ResetTimedTrace trace) {
        Node current = root.get(trace.lastOutput());
        if (current == null) {
            LeafNode leafNode = new LeafNode(trace);
            root.put(trace.lastOutput(), leafNode);
            leafNode.setPreNode(root);
            refineSymbolTrack(leafNode);
            return new SiftResult(leafNode, true);
        }
        boolean unambiguous = true;
        while (current.isInnerNode()) {
            InnerNode node = (InnerNode) current;
            TimedSuffixTrace suffix = node.getSequence();
            Answer key = teacher.query(trace, suffix);
            FastImmPair<Node, Boolean> nextEdge = getNode(node, key);
            if (!nextEdge.right) {
                incompleteTraces.add(new TimedIncompleteTrace(trace.convert(), suffix));
                unambiguous = false;
            }
            Node next = nextEdge.left;
            if (next == null) {
                LeafNode leafNode = new LeafNode(trace);
                node.add(key, leafNode);
                leafNode.setPreNode(node);
                if (trace.equals(ResetTimedTrace.empty(initOutput))) {
                    leafNode.setInit(true);
                }
                refineSymbolTrack(leafNode);
                return new SiftResult(leafNode, unambiguous);
            }
            current = next;
        }

        return new SiftResult((LeafNode) current, unambiguous);
    }

    private FastImmPair<Node, Boolean> getNode(InnerNode node, Answer answer) {
        List<Answer> similarKeys = new ArrayList<>();
        boolean unambiguous = true;
        for (Answer child : node.getKeyChildMap().keySet()) {
            // 考虑逻辑时间的合法性
            if (answer.isValid() == child.isValid() && answer.answerEqual(child, compChecker)) {
                similarKeys.add(child);
            }
        }
        if (similarKeys.isEmpty()) {
            return FastImmPair.of(null, true);
        } else {
            if (similarKeys.size() > 1) {
                unambiguous = false;
                similarKeys.sort(new Comparator<Answer>() {
                    @Override
                    public int compare(Answer o1, Answer o2) {
                        int sum1 = o1.getFrequencies().values().stream().mapToInt(Integer::intValue).sum();
                        int sum2 = o2.getFrequencies().values().stream().mapToInt(Integer::intValue).sum();
                        return sum2 - sum1;
                    }
                });
            }
            return FastImmPair.of(node.getKeyChildMap().get(similarKeys.get(0)), unambiguous);
        }
    }

    private void addTrack (Track track) {
        for (Track t : tracks) {
            if (track.getSource().equals(t.getSource()) && track.getInput().equals(t.getInput()) && !track.equals(t)) {
                System.out.printf("track repeat: %s, %s\n", track.getSource(), track.getInput());
                Exception e = new Exception("track repeat");
                e.printStackTrace();
                System.exit(0);
            }
        }
        tracks.add(track);
    }

    public PTA buildHypothesis() {
        locationNodeMap = new HashMap<>();
        nodeMdpStateMap = new HashMap<>();
        nodeMdpStateMap.put(chaosNode, chaosLocation);
        nodeMdpStateMap.put(invalidNode, invalidLocation);

        PTA hypothesis = new PTA();
        hypothesis.setInputs(inputs);
        Location init = buildStateList();
        buildTransitions();
        hypothesis.setInitial(init);
        for (Location location : nodeMdpStateMap.values()) {
            hypothesis.addLocation(location);
        }
        this.hypothesis = hypothesis;
        return hypothesis;
    }

    @Override
    public PTA getFinalHypothesis() {
        return hypothesis;
    }

    private Location buildStateList() {
        Location init = null;
        int index = 1;
        for (LeafNode node : getLeaves()) {
            Location location = new Location(index++, node.getLast(), node.getSequence());
            if (node.isInit()) {
                init = location;
            }
            nodeMdpStateMap.put(node, location);
        }
        return init;
    }

    private void buildTransitions() {
        Map<Location, Set<Track>> successors = new HashMap<>();
        for (Track track : tracks) {
            Location source = nodeMdpStateMap.get(track.getSource());
            successors.putIfAbsent(source, new HashSet<>());
            successors.get(source).add(track);
        }

        for (Map.Entry<Location, Set<Track>> successor : successors.entrySet()) {
            Location source = successor.getKey();
            Map<Input, List<Double>> chaosClockValuations = new HashMap<>();
            Map<Input, List<Double>> invalidClockValuations = new HashMap<>();
            Map<Input, Set<FastImmPair<Double, Map<Edge, Integer>>>> discreteTransitions = new HashMap<>();
            Map<Input, Set<Map<Edge, Integer>>> frequencies = new HashMap<>();
            for (Input input : inputs) {
                chaosClockValuations.put(input, new ArrayList<>());
                invalidClockValuations.put(input, new ArrayList<>());
                discreteTransitions.put(input, new HashSet<>());
                frequencies.put(input, new HashSet<>());
            }

            for (Track track : successor.getValue()) {
                TimedInput timedInput = track.getInput();
                if (track.getTarget() == invalidNode) {
                    invalidClockValuations.get(timedInput.getInput()).add(timedInput.getClockVal());
                } else {
                    if (track.getTarget() == chaosNode) {
                        chaosClockValuations.get(timedInput.getInput()).add(timedInput.getClockVal());
                    } else {
                        Map<Edge, Integer> frequency = new HashMap<>();
                        for (Map.Entry<FastImmPair<LeafNode, Boolean>, Integer> edge : track.getEdges().entrySet()) {
                            frequency.put(new Edge(edge.getKey().right, nodeMdpStateMap.get(edge.getKey().left)), edge.getValue());
                        }
                        frequencies.get(timedInput.getInput()).add(frequency);
                        discreteTransitions.get(timedInput.getInput()).add(FastImmPair.of(timedInput.getClockVal(), frequency));
                    }
                }
            }
            constructTransitions(chaosLocation, invalidLocation, compChecker, source, chaosClockValuations, invalidClockValuations, discreteTransitions, frequencies);
        }

        for (Map.Entry<LeafNode, Location> entry : nodeMdpStateMap.entrySet()) {
            locationNodeMap.put(entry.getValue(), entry.getKey());
        }
    }

    public Set<LeafNode> getLeaves() {
        Set<LeafNode> leaves = new HashSet<>();
        LinkedList<Node> queue = new LinkedList<>();
        queue.addAll(root.getChildren().values());
        while (!queue.isEmpty()) {
            Node node = queue.remove();
            if (node.isLeaf()) {
                LeafNode leaf = (LeafNode) node;
                leaves.add(leaf);
            } else {
                InnerNode innerNode = (InnerNode) node;
                queue.addAll(innerNode.getChildList());
            }
        }
        return leaves;
    }

    public void processCtxUnderIncomplete() {
        nrUnambiguousBreakPointAnalysis = 0;
        incompleteTraces.clear();
        ensureConsistentWithObsTree();
        hypothesis = buildHypothesis();
        for (ResetTimedIncompleteTrace ctx : ctxes) {
            ctx = longestReachablePrefix(ctx);
            if (refineUnderIncomplete(ctx)) {
                hypothesis = buildHypothesis();
            }

        }
    }

    public boolean refineUnderIncomplete(ResetTimedIncompleteTrace ctx) {
        ResetTimedTrace trace = ctx.getTrace();
        TimedInput lastInput = ctx.get(ctx.length()-1).right;
        Location location = hypothesis.getStateReachedByResetLogicalTimedTrace(trace).left;
        LeafNode leafNode = locationNodeMap.get(location);
        ResetTimedIncompleteTrace lastTs = new ResetTimedIncompleteTrace(leafNode.getSequence(), ResetTimedSuffixTrace.empty(lastInput));
        Answer lastAnswer = teacher.query(leafNode.getSequence(), TimedSuffixTrace.empty(lastInput));
        if (!lastAnswer.isComplete()) {
            incompleteTraces.add(lastTs.convert());
        }
        Map<TimedOutput, Integer> hypFreq = PACEquivalence.getHypoFrequencies(hypothesis, lastTs);
        boolean hypFreqValid = !hypFreq.containsKey(TimedOutput.create(true, "Invalid"));
        if (lastAnswer.isValid() != hypFreqValid || !compChecker.compatible(lastAnswer.getFrequencies(), hypFreq)) {
            buildTrack(leafNode, lastInput);
            return true;
        }

        ErrorIndexResult result = errorIndexAnalyse(ctx);
        if (result == null) {
            return false;
        }
        if (result.getError() == ErrorEnum.RefineGuard) {
            buildTrack(result.getSource(), result.getPair().left);
        }
        if (result.getError() == ErrorEnum.ConsistentError) {
            splitNode(result.getTarget(), result.getSource().getSequence().append(result.getPair()), result.getSuffix());
            buildTrack(result.getSource(), result.getPair().left);
        }
        return true;
    }

    private ResetTimedIncompleteTrace longestReachablePrefix(ResetTimedIncompleteTrace incompleteTrace){

        ResetTimedIncompleteTrace copied = incompleteTrace;
        for (int i = incompleteTrace.length(); i >= 1; i--) {
            ResetTimedTrace trace = copied.getTrace();
            FastImmPair<Location, Double> state = hypothesis.getStateReachedByResetLogicalTimedTrace(trace);
            if (state != null) {
                return copied;
            }
            copied = copied.prefix(copied.length()-1);
        }
        return copied;
    }

    private void splitNode(LeafNode original, ResetTimedTrace added, TimedSuffixTrace suffix) {
        Answer originalKey = teacher.query(original.getSequence(), suffix);
        Answer inConsistentKey = teacher.query(added, suffix);
        InnerNode innerNode = new InnerNode(suffix);
        // change targetNode in its parent childMap
        List<LeafNode> refineNodes = new ArrayList<>();
        refineNodes.add(original);
        Node preNode = original.getPreNode();
        if (preNode == root) {
            root.put(original.getLast(), innerNode);
        } else {
            InnerNode father = (InnerNode) preNode;
            Answer dist = null;
            for (Answer edge : father.getKeyChildMap().keySet()) {
                if (father.getChild(edge).equals(original)) {
                    dist = edge;
                    break;
                }
            }
            father.getKeyChildMap().put(dist, innerNode);
            refineNodes.addAll(father.getAllOffspring());
        }
        innerNode.setPreNode(preNode);
        // set innerNode childMap
        original.setPreNode(innerNode);
        LeafNode newLeafNode = new LeafNode(added);
        newLeafNode.setPreNode(innerNode);
        innerNode.add(originalKey, original);
        innerNode.add(inConsistentKey, newLeafNode);

        refineNode(refineNodes);
        refineSymbolTrack(newLeafNode);
    }

    private void refineNode(List<LeafNode> relatedLeaves) {
        Set<Track> unambiguousTracks = tracks.stream().filter(track -> track.getTarget() == null).collect(Collectors.toSet());
        for (Track track : unambiguousTracks) {
            for (FastImmPair<LeafNode, Boolean> edge : track.getEdges().keySet()) {
                if (relatedLeaves.contains(edge.left)) {
                    LeafNode latest = sift(track.getSource().getSequence()
                            .append(FastImmPair.of(track.getInput(), TimedOutput.create(edge.right, edge.left.getLast().getSymbol()))))
                            .getLeafNode();
                    if (latest != edge.left) {
                        Map<FastImmPair<LeafNode, Boolean>, Integer> copiedEdges = new HashMap<>(track.getEdges());
                        copiedEdges.put(FastImmPair.of(latest, edge.right),
                                copiedEdges.remove(edge));
                        tracks.remove(track);
                        addTrack(new Track(track.getSource(), track.getInput(), track.getTarget(), copiedEdges));
                    }
                }
            }
        }
    }

    private ErrorIndexResult errorIndexAnalyse(ResetTimedIncompleteTrace ctx) {

        for (int i = 0; i < ctx.length()-1; i++) {
            Triple<ResetTimedTrace, FastImmPair<TimedInput, TimedOutput>, ResetTimedSuffixTrace> triple = ctx.splitAt(i);
            ResetTimedTrace prefix = triple.getLeft();
            TimedSuffixTrace suffix = triple.getRight().convert();
            Location u = hypothesis.getStateReachedByResetLogicalTimedTrace(prefix).left;
            LeafNode lu = locationNodeMap.get(u);
            Location v = hypothesis.getStateReachedByResetLogicalTimedTrace(prefix.append(triple.getMiddle())).left;
            LeafNode lv = locationNodeMap.get(v);
            Answer tarDist = teacher.query(lu.getSequence().append(triple.getMiddle()), suffix);
            Answer hypDist = teacher.query(lv.getSequence(), suffix);
            if (!tarDist.isComplete()) {
                incompleteTraces.add(new TimedIncompleteTrace(lu.getSequence().append(triple.getMiddle()).convert(), suffix));
            }
            if (!hypDist.isComplete()) {
                incompleteTraces.add(new TimedIncompleteTrace(lv.getSequence().convert(), suffix));
            }
            if ((tarDist.isComplete() && hypDist.isComplete() && tarDist.isValid() != hypDist.isValid())
                    || !hypDist.answerEqual(tarDist, compChecker)) {
                SiftResult siftResult = sift(lu.getSequence().append(triple.getMiddle()));
                if (siftResult.getLeafNode() != lv) {
                    InnerNode discriminator = (InnerNode) lowestCommonAncestor(lv, siftResult.getLeafNode());
                    tarDist = teacher.query(lu.getSequence().append(triple.getMiddle()), discriminator.getSequence());
                    hypDist = teacher.query(lv.getSequence(), discriminator.getSequence());
                    if ((tarDist.isComplete() && hypDist.isComplete() && tarDist.isValid() != hypDist.isValid())
                            || !tarDist.answerEqual(hypDist, compChecker)) {
                        return new ErrorIndexResult(lu, triple.getMiddle(), lv, suffix, ErrorEnum.RefineGuard);
                    }
                } else {
                    if (siftResult.isUnambiguous()) {
                        return new ErrorIndexResult(lu, triple.getMiddle(), lv, suffix, ErrorEnum.ConsistentError);
                    }
                }
            }
        }
        nrUnambiguousBreakPointAnalysis++;
        return null;
    }

    private Node lowestCommonAncestor(LeafNode l1, LeafNode l2) {
        List<Node> ancestor1 = new ArrayList<>();
        List<Node> ancestor2 = new ArrayList<>();
        Node preNode = l1.getPreNode();
        while (preNode != root) {
            ancestor1.add(preNode);
            InnerNode father = (InnerNode) preNode;
            preNode = father.getPreNode();
        }
        preNode = l2.getPreNode();
        while (preNode != root) {
            ancestor2.add(preNode);
            InnerNode father = (InnerNode) preNode;
            preNode = father.getPreNode();
        }
        Node ancestor = root;
        for (int i = 1; i <= Math.min(ancestor1.size(), ancestor2.size()) && ancestor1.get(ancestor1.size()-i) == ancestor2.get(ancestor2.size()-i); i++) {
            ancestor = ancestor1.get(ancestor1.size()-i);
        }
        return ancestor;

    }

    public double portionOfUnambiguousRows() {
        return unambiguous;
    }

}
