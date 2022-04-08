package observationTable;

import automaton.*;
import base.Compatibility;
import base.learner.Answer;
import base.learner.Learner;
import base.learner.LearningSetting;
import base.teacher.Teacher;
import org.apache.commons.lang3.tuple.Triple;
import trace.*;
import util.export.DotExporter;
import utils.FastImmPair;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;

public class ObservationTable implements Learner {

    private Set<ResetTimedTrace> Srows;
    private Set<TimedSuffixTrace> Ecols;
    private HashMap<ResetTimedTrace, Row> shortRows;
    private HashMap<ResetTimedTrace, Row> longRows;

    private final Set<Input> inputs;
    private final Set<TimedSuffixTrace> singleInputCols;
    private final Teacher teacher;
    private final Compatibility compChecker;

    private TreeMap<ResetTimedTrace, FastImmPair<Integer, Set<ResetTimedTrace>>> groupedAccSequences;
    private ResetTimedTrace firstShortTrace;
    private Location chaosLocation;
    private Location invalidLocation;
    private boolean bfsSort = false;

    // new
    private Map<ResetTimedTrace, Set<TimedInput>> timeInputMap;
    private double unambiguousRows = 0;
    private Set<TimedIncompleteTrace> consistentCheckIncomplete;

    public ObservationTable(Set<Input> inputs, Compatibility compChecker, Teacher teacher) {
        super();
        this.singleInputCols = new HashSet<>();
        this.compChecker = compChecker;
        this.teacher = teacher;
        this.inputs = inputs;
        for (Input input : inputs) {
            singleInputCols.add(TimedSuffixTrace.empty(TimedInput.create(input)));
        }
    }

    public void writeTableToFile(String fileName) throws IOException {
        groupShortRows();
        StringBuilder fileContent = new StringBuilder();
        fileContent.append(";;;").append(Ecols.stream().map(Object::toString).collect(Collectors.joining(";")));
        fileContent.append(System.lineSeparator());
        List<Map.Entry<ResetTimedTrace, Row>> sortedSRows = new ArrayList<>(shortRows.entrySet());
        sortedSRows.sort(Comparator.comparing(e -> e.getKey().toString()));

        for (Map.Entry<ResetTimedTrace, Row> shortRow : sortedSRows) {
            fileContent.append(findGroup(shortRow.getKey())).append(";");
            fileContent.append("S;").append(shortRow.getKey().toString()).append(";");
            String cellContents = Ecols.stream().map(eCol -> shortRow.getValue().get(eCol).toString())
                    .collect(Collectors.joining(";"));
            fileContent.append(cellContents);
            fileContent.append(System.lineSeparator());
        }

        List<Map.Entry<ResetTimedTrace, Row>> sortedLRows = new ArrayList<>(longRows.entrySet());
        sortedLRows.sort(Comparator.comparing(e -> e.getKey().toString()));

        for (Map.Entry<ResetTimedTrace, Row> longRow : sortedLRows) {
            fileContent.append(findGroup(longRow.getKey())).append(";");
            fileContent.append("L;").append(longRow.getKey().toString()).append(";");
            String cellContents = Ecols.stream().map(eCol -> longRow.getValue().get(eCol).toString())
                    .collect(Collectors.joining(";"));
            fileContent.append(cellContents);
            fileContent.append(System.lineSeparator());
        }
        File f = new File(fileName);
        f.getParentFile().mkdirs();
        try (FileWriter fw = new FileWriter(f)) {
            BufferedWriter bw = new BufferedWriter(fw);
            bw.write(fileContent.toString());
            bw.flush();
        }
    }

    /**
     * Computes the relative number of unambiguous rows, i.e. the rows
     * compatible to exactly one compatibility class representative.
     *
     * @return relative number of unambiguous rows in [0,1]
     */
    public double portionOfUnambiguousRows() {
        if (unambiguousRows == 1.0)
            return 1.0;
        int nrUnambig = 0;
        groupShortRows();
        for (ResetTimedTrace shortTrace : shortRows.keySet()) {
            if (!findGroup(shortTrace).trim().contains(" "))
                nrUnambig++;
        }
        for (ResetTimedTrace longTrace : longRows.keySet()) {
            if (!findGroup(longTrace).trim().contains(" "))
                nrUnambig++;
        }
        return (double) nrUnambig / (longRows.size() + shortRows.size());
    }

    private String findGroup(ResetTimedTrace trace) {

        if (shortRows.containsKey(trace) && groupedAccSequences.containsKey(trace)) {
            return "[" + groupedAccSequences.get(trace).getLeft() + "]";
        }
        StringBuilder allCompat = new StringBuilder();
        Row rowForTrace = shortRows.get(trace);
        if (rowForTrace == null)
            rowForTrace = longRows.get(trace);
        for (Map.Entry<ResetTimedTrace, FastImmPair<Integer, Set<ResetTimedTrace>>> entry :
                groupedAccSequences.entrySet()) {
            Row shortRow = shortRows.get(entry.getKey());
            if (rowForTrace.statRowEquivalence(shortRow, compChecker)
                    && trace.lastOutput().equals(entry.getKey().lastOutput()))
                allCompat.append(entry.getValue().getLeft()).append(" ");
        }
        return allCompat.toString();

    }

    /**
     * Create compatibility classes.
     */
    private void groupShortRows() {
        if (groupedAccSequences != null)
            return;
        List<ResetTimedTrace> nonGrouped = new ArrayList<>(shortRows.keySet());
        // sort by "completeness", otherwise we might simply, we want to have
        // complete entries as
        // representatives and add non-complete to their groups and not the
        // opposite
        // i.e. we do not want incomplete entries and add more complete entries
        // to their groups
        // this may give rise to degenerate cases where we have only one group
        if (bfsSort)
            nonGrouped = sortShortByBFS(nonGrouped);
        else
            nonGrouped.sort(this::compareCompletenessOfShort);

        TreeMap<ResetTimedTrace, FastImmPair<Integer, Set<ResetTimedTrace>>> grouped = new TreeMap<>(ObservationTable::rowTraceOrder);
        // TreeMap<RowTrace, FastImmPair<Integer,Set<RowTrace>>> grouped = new
        // TreeMap<>(
        // this::compareCompletenessOfShort);

        int groupID = 0;
        while (!nonGrouped.isEmpty()) {
            ResetTimedTrace current = nonGrouped.iterator().next();
            Set<ResetTimedTrace> currentGroup = new HashSet<>();
            for (ResetTimedTrace potentiallyEquiv : nonGrouped) {
                // if
                // (shortRows.get(current).statRowEquivalence(shortRows.get(potentiallyEquiv),
                // compChecker))
                if (shortRows.get(current).statRowEquivalence(shortRows.get(potentiallyEquiv), compChecker)
                        && current.lastOutput().equals(potentiallyEquiv.lastOutput()))
                    currentGroup.add(potentiallyEquiv);
            }
            nonGrouped.removeAll(currentGroup);
            grouped.put(current, FastImmPair.of(groupID++, currentGroup));
            // grouped.put(chooseMostCompleteShort(currentGroup), currentGroup);
        }
        groupedAccSequences = grouped;
    }

    private List<ResetTimedTrace> sortShortByBFS(List<ResetTimedTrace> shortRowList) {
        List<ResetTimedTrace> result = new ArrayList<>();
        LinkedList<ResetTimedTrace> schedule = new LinkedList<>();
        schedule.add(firstShortTrace);
        while (!schedule.isEmpty()) {
            ResetTimedTrace current = schedule.removeFirst();
            result.add(current);
            List<ResetTimedTrace> next = shortRowList.stream().
                    filter(other -> other.isExtensionOf(current)).sorted(ObservationTable::rowTraceOrder).
                    collect(Collectors.toList());
            // next.sort(this::compareCompletenessOfShort);

            schedule.addAll(next);
        }
        return result;
    }

    private int compareCompletenessOfShort(ResetTimedTrace t1, ResetTimedTrace t2) {

        int nrEntries1 = 0;
        int nrEntries2 = 0;
        int nrComplete1 = 0;
        int nrComplete2 = 0;

        Set<TimedInput> timedInputs = new HashSet<>(timeInputMap.get(t1));
        timedInputs.addAll(timeInputMap.get(t2));

        for (TimedInput timedInput : timedInputs) {
            TimedSuffixTrace Etrace = TimedSuffixTrace.empty(timedInput);
            Answer answer1 = teacher.query(t1, Etrace);
            Answer answer2 = teacher.query(t2, Etrace);

//            if (answer1.isComplete() && !answer1.getFrequencies().isEmpty())
//                nrComplete1++;
//            if (answer2.isComplete() && !answer2.getFrequencies().isEmpty())
//                nrComplete2++;
            for (Integer outputCount : answer1.getFrequencies().values())
                nrEntries1 += outputCount;
            for (Integer outputCount : answer2.getFrequencies().values())
                nrEntries2 += outputCount;
        }
//        int completenessCompare = -Integer.compare(nrComplete1, nrComplete2);
//        if (completenessCompare != 0)
//            return completenessCompare;
        // sort descending
        return -Integer.compare(nrEntries1, nrEntries2);
    }

    private static int rowTraceOrder(ResetTimedTrace rt1, ResetTimedTrace rt2) {
        String rt1String = rt1.toString();
        String rt2String = rt2.toString();
        if (rt1String.equals(rt2String))
            return 0;
        if (rt1String.startsWith(rt2String)) { // rt1 longer -> rt1 > rt2 ->
            // positive integer
            return 1;
        } else if (!rt1String.startsWith(rt2String)) {
            return -1;
        } else {
            // aside from prefix relation we do not care and simply use string
            // comparison
            return rt1String.compareTo(rt2String);
        }
    }

    @Override
    public void init(Output initialOutput) {
        Srows = new HashSet<>();
        Ecols = new HashSet<>();
        shortRows = new HashMap<>();
        longRows = new HashMap<>();
        timeInputMap = new HashMap<>();

        firstShortTrace = ResetTimedTrace.empty(initialOutput);
        Srows.add(firstShortTrace);
        Ecols.addAll(singleInputCols);
        Row firstRow = new Row(Ecols);
        shortRows.put(firstShortTrace, firstRow);

        timeInputMap.put(firstShortTrace, new HashSet<>());
        for (TimedSuffixTrace singleInputEcol : singleInputCols) {
            timeInputMap.get(firstShortTrace).add(singleInputEcol.getFirstInput());
        }
        chaosLocation = Location.chaos(inputs);
        invalidLocation = Location.invalid(inputs);
        consistentCheckIncomplete = new HashSet<>();

        List<TimedIncompleteTrace> initialTraces = new ArrayList<>();
        for (TimedSuffixTrace colTrace : Ecols)
            initialTraces.add(new TimedIncompleteTrace(firstShortTrace.convert(), colTrace));
        fillTable(Optional.of(initialTraces));
        addLongRows(firstShortTrace);

    }

    private void addLongRows(ResetTimedTrace shortRowTrace) {
        for (TimedInput i : timeInputMap.get(shortRowTrace)) {
            Set<TimedOutput> outputsAfterShortAndI = teacher.query(shortRowTrace, TimedSuffixTrace.empty(i)).getFrequencies().keySet();
            for (TimedOutput o : outputsAfterShortAndI) {
                ResetTimedTrace longTrace = shortRowTrace.append(FastImmPair.of(i, o));
                if (!longRows.containsKey(longTrace) && !shortRows.containsKey(longTrace)) {
                    Row longRow = new Row(Ecols);
                    longRows.put(longTrace, longRow);
                    ensureConsistencyWithTree(longTrace, longRow);
                }
            }
        }
    }

    private void ensureConsistencyWithTree(ResetTimedTrace rowTrace, Row row) {
        for (TimedSuffixTrace colTrace : Ecols) {
            row.put(colTrace, teacher.query(rowTrace, colTrace));
        }
    }

    public void ensureConsistencyWithTree() {
        for (ResetTimedTrace shortRowTrace : shortRows.keySet()) {
            Row row = shortRows.get(shortRowTrace);
            ensureConsistencyWithTree(shortRowTrace, row);
            addLongRows(shortRowTrace);
        }

        for (ResetTimedTrace longRowTrace : longRows.keySet()) {
            Row row = longRows.get(longRowTrace);
            ensureConsistencyWithTree(longRowTrace, row);
        }
        groupedAccSequences = null;
    }

    private List<TimedIncompleteTrace> findIncomplete() {
        return findIncomplete(false);
    }

    private List<TimedIncompleteTrace> findIncomplete(boolean allLongCols) {
        List<TimedIncompleteTrace> incompleteTraces = new ArrayList<>();
        for (ResetTimedTrace shortTrace : shortRows.keySet()) {
            Set<TimedSuffixTrace> cols = new HashSet<>(Ecols);
            for (TimedInput timedInput : timeInputMap.get(shortTrace)) {
                cols.add(TimedSuffixTrace.empty(timedInput));
            }
            for (TimedSuffixTrace colTrace : cols) {
                Answer answer = teacher.query(shortTrace, colTrace);
                if (answer.isValid() && !answer.isComplete())
                    incompleteTraces.add(new TimedIncompleteTrace(shortTrace.convert(), colTrace));
            }
        }
        for (ResetTimedTrace longTrace : longRows.keySet()) {
            for (TimedSuffixTrace colTrace : allLongCols ? Ecols : singleInputCols) {
                if (!longRows.get(longTrace).get(colTrace).isComplete())
                    incompleteTraces.add(new TimedIncompleteTrace(longTrace.convert(), colTrace));
            }
        }
        incompleteTraces.addAll(consistentCheckIncomplete);
        System.out.println("Incomplete traces: " + incompleteTraces.size());
        return incompleteTraces;
    }

    /**
     * Perform a refine query as described in "L*-Based Learning of MDPs".
     */
    public void fillTable() {
        fillTable(Optional.empty());
    }

    /**
     * Perform a refine query restricted to the sequences given as parameter. If
     * no sequences are given, the refine query is performed as discussed in the
     * paper, i.e. incomplete sequences stored in the observation table are
     * resampled.
     *
     * @param tracesToFill
     *            traces that should be resampled
     */
    public void fillTable(Optional<List<TimedIncompleteTrace>> tracesToFill) {
        // 1. make information between observation tree and table consistent
        ensureConsistencyWithTree();
        List<TimedIncompleteTrace> traces = null;
        if (tracesToFill.isEmpty()) {
            // 2. find incomplete entries
            traces = findIncomplete();
            if (traces.isEmpty()) {
                // initially collect only single input columns for long traces,
                // but if all of them
                // are complete then try all
                traces = findIncomplete(true);
                if (traces.isEmpty()) {
                    unambiguousRows = 1.0;
                    return;
                }
            }
            // System.out.println("Traces to incomplete " );
            // traces.forEach(t -> System.out.println(t));
        } else {
            traces = tracesToFill.get();
        }

        unambiguousRows = 0;
        // 3. try making those complete through queries
        teacher.refine(traces);
        // 5. make tree and table consistent again
        ensureConsistencyWithTree();
        groupedAccSequences = null;
    }

    @Override
    public void learn(LearningSetting setting) throws IOException {
        init(teacher.getInitialOutput());
        fillTable();

        PTA hypo = null;
        int rounds = 0;
        int count = 1;
        DotExporter dotExp = new DotExporter();
        do {
            stabilise();
            System.out.println("Round " + rounds);
            if (rounds > 0 && rounds % (setting.getPrintFrequency()) == 0) {
                writeTableToFile("hypotheses/table_" + rounds + ".csv");
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
            FastImmPair<Boolean, ResetTimedIncompleteTrace> eqResult = teacher.equivalenceQuery(count, hypo, portionOfUnambiguousRows());
            if (eqResult.left) {
                break;
            }
            if (eqResult.right != null) {
                count++;
                processCounterexample(eqResult.right);
            }
            fillTable();
        } while (true);

        dotExp.writeToFile(hypo, "hypotheses/hyp_final.dot");
        writeTableToFile("hypotheses/table_final.csv");
        if (setting.getRmlExp() != null)
            setting.getRmlExp().toFile(hypo, "hypotheses/hyp_final.prism");
        setting.setRounds(rounds);
        setting.setHypothesis(hypo);
    }

    @Override
    public void processCounterexample(ResetTimedIncompleteTrace counterExample) {
        for (int prefixLength = 1; prefixLength <= counterExample.length(); prefixLength++) {
            ResetTimedIncompleteTrace prefix = (ResetTimedIncompleteTrace) counterExample.prefix(prefixLength);
            ResetTimedTrace trace = (ResetTimedTrace) prefix.getTrace();
            updateS(trace);
            timeInputMap.get(trace).add(prefix.get(prefixLength-1).right);
        }
    }

    @Override
    public void show() {
        List<List<String>> table = new ArrayList<>();
        int maxLenPrefix = 0;

        // 添加表头
        List<String> row = new ArrayList<>();
        row.add("");
        for (TimedSuffixTrace col : Ecols) {
            row.add(col.toString());
            maxLenPrefix = Math.max(maxLenPrefix, row.get(row.size() - 1).length());
        }
        table.add(row);

        // 添加S区
        for (Map.Entry<ResetTimedTrace, Row> tableRow : shortRows.entrySet()) {
            row = new ArrayList<>();
            row.add(tableRow.getKey().toString());
            maxLenPrefix = Math.max(maxLenPrefix, row.get(row.size() - 1).length());
            for (TimedSuffixTrace col : Ecols) {
                Answer cell = tableRow.getValue().get(col);
                if (cell == null) {
                    row.add("");
                } else {
                    row.add(cell.toString());
                    maxLenPrefix = Math.max(maxLenPrefix, row.get(row.size() - 1).length());
                }
            }
            table.add(row);
        }

        // 添加分隔符
        row = new ArrayList<>();
        for(int i = 0; i <= Ecols.size(); i++) {
            row.add("-");
        }
        table.add(row);

        // 添加R区
        for(Map.Entry<ResetTimedTrace, Row> tableRow : longRows.entrySet()){
            row = new ArrayList<>();
            row.add(tableRow.getKey().toString());
            maxLenPrefix = Math.max(maxLenPrefix, row.get(row.size() - 1).length());
            for (TimedSuffixTrace col : Ecols) {
                Answer cell = tableRow.getValue().get(col);
                if (cell == null) {
                    row.add("");
                } else {
                    row.add(cell.toString());
                    maxLenPrefix = Math.max(maxLenPrefix, row.get(row.size() - 1).length());
                }
            }
            table.add(row);
        }

        // 输出
        int maxRowLen = maxLenPrefix * (Ecols.size() + 1);
        StringBuilder sb = new StringBuilder();
        sb.append("-".repeat(Math.max(0, maxRowLen)));
        sb.append("\n");
        for (List<String> strings : table) {
            sb.append("|");
            for (int j = 0; j < Ecols.size() + 1; j++) {
                sb.append(strings.get(j));
                for (int z = strings.get(j).length(); z < maxLenPrefix; z++) {
                    if ("-".equals(strings.get(j))) {
                        sb.append("-");
                    } else {
                        sb.append(" ");
                    }
                }
                sb.append("|");
            }
            sb.append("\n");
        }
        sb.append("-".repeat(Math.max(0, maxRowLen)));
        sb.append("\n");
        System.out.print(sb);
    }

    @Override
    public boolean check(ResetTimedIncompleteTrace counterExample) {
        return false;
    }

    private Optional<ResetTimedTrace> closenessCheck() {
        groupShortRows();

        // precondition longRows stores only defined rows
        for (ResetTimedTrace longRowTrace : longRows.keySet()) {
            Row longRow = longRows.get(longRowTrace);
            boolean foundMatch = false;
            for (ResetTimedTrace shortRowTrace : groupedAccSequences.keySet()) {
                Row shortRow = shortRows.get(shortRowTrace);
                if (longRowTrace.lastOutput().equals(shortRowTrace.lastOutput())
                        && longRow.statRowEquivalence(shortRow, compChecker)) {
                    foundMatch = true;
                }
            }
            if (!foundMatch)
                return Optional.of(longRowTrace);
        }
        return Optional.empty();
    }

    private Optional<TimedSuffixTrace> consistencyCheck() {
        consistentCheckIncomplete = new HashSet<>();
        for (ResetTimedTrace s1 : Srows) {
            Row s1Row = shortRows.get(s1);
            for (ResetTimedTrace s2 : Srows) {
                Row s2Row = shortRows.get(s2);
                if (s1 != s2) {
                    if (s1.lastOutput().equals(s2.lastOutput()) && s1Row.statRowEquivalence(s2Row, compChecker)) {
                        // 检查相同timed input下的frequencies是否compatible
                        Set<TimedInput> timedInputs = new HashSet<>(timeInputMap.get(s1));
                        timedInputs.addAll(timeInputMap.get(s2));
                        for (TimedInput timedInput : timedInputs) {
                            Answer answer1 = teacher.query(s1, TimedSuffixTrace.empty(timedInput));
                            Answer answer2 = teacher.query(s2, TimedSuffixTrace.empty(timedInput));
                            if (!answer1.isComplete() && !timeInputMap.get(s1).contains(timedInput)) {
                                consistentCheckIncomplete.add(new TimedIncompleteTrace(s1.convert(), TimedSuffixTrace.empty(timedInput)));
                            }
                            if (!answer2.isComplete() && !timeInputMap.get(s2).contains(timedInput)) {
                                consistentCheckIncomplete.add(new TimedIncompleteTrace(s2.convert(), TimedSuffixTrace.empty(timedInput)));
                            }
                            if (!answer1.answerEqual(answer2, compChecker)) {
                                return Optional.of(TimedSuffixTrace.empty(timedInput));
                            }
                            Set<TimedOutput> timedOutputs = new HashSet<>(answer1.getFrequencies().keySet());
                            timedOutputs.retainAll(answer2.getFrequencies().keySet());
                            // 检查相同的 (i,o) 扩展的 trace 是否 compatible
                            for (TimedOutput timedOutput : timedOutputs) {
                                FastImmPair<TimedInput, TimedOutput> symbol = FastImmPair.of(timedInput, timedOutput);
                                ResetTimedTrace s1Ext = s1.append(symbol);
                                ResetTimedTrace s2Ext = s2.append(symbol);

                                Row s1ExtRow = shortRows.get(s1Ext);
                                if (s1ExtRow == null)
                                    s1ExtRow = longRows.get(s1Ext);
                                Row s2ExtRow = shortRows.get(s2Ext);
                                if (s2ExtRow == null)
                                    s2ExtRow = longRows.get(s2Ext);

                                if (s1ExtRow == null || s2ExtRow == null)
                                    continue;

                                for (TimedSuffixTrace suffix : Ecols) {
                                    if (!s1ExtRow.get(suffix).answerEqual(s2ExtRow.get(suffix), compChecker)) {
                                        TimedSuffixTrace newSuffix = suffix.
                                                prepend(FastImmPair.of(symbol.left, symbol.right.getOutput()));
                                        return Optional.of(newSuffix);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return Optional.empty();
    }

    public void stabilise() {
        do {
            // maybe I need to fill the table here
            Optional<ResetTimedTrace> closedCounterEx = closenessCheck();
            if (closedCounterEx.isPresent()) {
                updateS(closedCounterEx.get());
                // this is only necessary because of grouping of sequences
                // conditional on first output
                ensureConsistencyWithTree();
                groupedAccSequences = null;
            } else {
                Optional<TimedSuffixTrace> consistencyCounterEx = consistencyCheck();
                System.out.println(consistencyCounterEx);
                if (consistencyCounterEx.isPresent()) {
                    updateE(consistencyCounterEx.get());
                    ensureConsistencyWithTree(); // TODO check if ensuring
                    // consistency with tree is
                    // enough
                    groupedAccSequences = null;
                } else {
                    break;
                }
            }
        } while (true);
        groupShortRows();
    }

    public void updateS(ResetTimedTrace rowTrace) {
        if (shortRows.containsKey(rowTrace)) {
            return;
        }
        groupedAccSequences = null;

        Srows.add(rowTrace);
        Row row = longRows.remove(rowTrace);
        if (row == null) {
            row = new Row(Ecols);
        }

        ensureConsistencyWithTree(rowTrace, row);
        shortRows.put(rowTrace, row);
        timeInputMap.putIfAbsent(rowTrace, new HashSet<>());
        Set<TimedInput> timedInputs = timeInputMap.get(rowTrace);
        for (TimedSuffixTrace singleInput : singleInputCols) {
            timedInputs.add(singleInput.getFirstInput());
        }
        addLongRows(rowTrace);
    }

    /**
     * Adds a new column.
     *
     * @param columnTrace
     *            the new column
     */
    public void updateE(TimedSuffixTrace columnTrace) {
        if (Ecols.contains(columnTrace))
            return;

        groupedAccSequences = null;
        Ecols.add(columnTrace);
        for (ResetTimedTrace shortRow : shortRows.keySet()) {
            addFreshCellToTable(shortRow, columnTrace);
        }
        for (ResetTimedTrace longRow : longRows.keySet()) {
            addFreshCellToTable(longRow, columnTrace);
        }
    }

    private void addFreshCellToTable(ResetTimedTrace row, TimedSuffixTrace col) {
        addTableCell(row, col, Answer.ValidAnswer());
    }

    private void addTableCell(ResetTimedTrace row, TimedSuffixTrace col, Answer cellToAdd) {
        Row tableRow = null;
        if (Srows.contains(row))
            tableRow = shortRows.get(row);
        else
            tableRow = longRows.get(row);
        if (tableRow == null) {
            System.out.println(row);
            System.out.println(Srows.contains(row));
            System.out.println(shortRows.containsKey(row));
            System.out.println(longRows.containsKey(row));
            throw new RuntimeException(
                    "Inconsistency detected: we want to add a cell to row that" + " does not exist.");
        }
        // TODO check whether we do redundant work
        Answer tableCell = tableRow.get(col);
        if (tableCell == null) {
            tableRow.put(col, cellToAdd);
        }
    }

    private Set<TimedInput> getAllSameInput(ResetTimedTrace t1, ResetTimedTrace t2) {
        Set<TimedInput> timedInputs = new HashSet<>(timeInputMap.get(t1));
        timedInputs.retainAll(timeInputMap.get(t2));
        return timedInputs;
    }

    @Override
    public PTA buildHypothesis() {
        return hypothesis((row1, row2) -> row1.statRowEquivalence(row2, compChecker));
    }

    private PTA hypothesis(BiPredicate<Row, Row> rowEquiv) {
        groupShortRows();
        if (closenessCheck().isPresent() || consistencyCheck().isPresent()) {
            System.out.println(closenessCheck());
            System.out.println(consistencyCheck());

            throw new RuntimeException("Cannot create hypothesis");
        }
        PTA hypothesis = new PTA();
        Map<ResetTimedTrace, FastImmPair<Location, Set<ResetTimedTrace>>> traceToStates = createStatesForGroups(rowEquiv);
        // assumption DLMDP -> exactly one initial output
        Location initial = traceToStates.get(firstShortTrace).getLeft();
        hypothesis.setInitial(initial);
        hypothesis.addLocation(chaosLocation);
        hypothesis.addLocation(invalidLocation);
        Set<Location> addedStates = addTransitions(initial, traceToStates);
        for (Location location : addedStates)
            hypothesis.addLocation(location);
        hypothesis.setInputs(inputs);
        return hypothesis;
    }

    private Map<ResetTimedTrace, FastImmPair<Location, Set<ResetTimedTrace>>> createStatesForGroups(
            BiPredicate<Row, Row> rowEquiv) {
        Map<ResetTimedTrace, FastImmPair<Location, Set<ResetTimedTrace>>> stateMap = new HashMap<>();
        // int id = 0;
        for (Map.Entry<ResetTimedTrace, FastImmPair<Integer, Set<ResetTimedTrace>>> compatGroup : groupedAccSequences.entrySet()) {
            stateMap.put(compatGroup.getKey(), FastImmPair.of(new Location(compatGroup.getValue().getLeft(),
                    compatGroup.getKey().lastOutput(), compatGroup.getKey()), compatGroup.getValue().getRight()));
        }
        return stateMap;
    }

    private Set<Location> addTransitions(Location initial,
                                         Map<ResetTimedTrace, FastImmPair<Location, Set<ResetTimedTrace>>> traceToStates) {
        LinkedList<Location> schedule = new LinkedList<>();
        schedule.add(initial);
        Set<Location> alreadyAddedTrans = new HashSet<>();
        while (!schedule.isEmpty()) {
            Location current = schedule.removeFirst();
            if (alreadyAddedTrans.contains(current) || current.getLabel().equals(Output.chaos()))
                continue;
            Map.Entry<ResetTimedTrace, FastImmPair<Location, Set<ResetTimedTrace>>> traceAndState = findTraceAndState(traceToStates,
                    current);
            ResetTimedTrace accSeq = traceAndState.getKey();
            Location location = traceAndState.getValue().getLeft();

            addTransitionsForState(location, accSeq, traceToStates, schedule);

            alreadyAddedTrans.add(current);
        }
        return alreadyAddedTrans;
    }

    private List<ResetTimedTrace> findExt(ResetTimedTrace accSeq, TimedInput i) {
        List<ResetTimedTrace> extensions = new ArrayList<>();
        for (ResetTimedTrace shortTrace : shortRows.keySet()) {
            if (shortTrace.isExtensionOf(accSeq, i))
                extensions.add(shortTrace);
        }
        for (ResetTimedTrace longTrace : longRows.keySet()) {
            if (longTrace.isExtensionOf(accSeq, i))
                extensions.add(longTrace);
        }
        return extensions;
    }

    private Map.Entry<ResetTimedTrace, FastImmPair<Location, Set<ResetTimedTrace>>> findTraceAndState(
            Map<ResetTimedTrace, FastImmPair<Location, Set<ResetTimedTrace>>> traceToStates, Location current) {
        for (Map.Entry<ResetTimedTrace, FastImmPair<Location, Set<ResetTimedTrace>>> traceAndState : traceToStates.entrySet()) {
            if (traceAndState.getValue().getLeft().equals(current))
                return traceAndState;
        }
        return null;
    }

    private Location findStateForExtension(Map<ResetTimedTrace, FastImmPair<Location, Set<ResetTimedTrace>>> traceToStates,
                                           ResetTimedTrace ext, Row rowForExt) {
        for (Map.Entry<ResetTimedTrace, FastImmPair<Location, Set<ResetTimedTrace>>> tracesForState : traceToStates.entrySet()) {
            Row shortRow = shortRows.get(tracesForState.getKey());
            if (shortRow.statRowEquivalence(rowForExt, compChecker)
                    && tracesForState.getKey().lastOutput().equals(ext.lastOutput())) {
                return tracesForState.getValue().getLeft();
            }
        }
        if (shortRows.containsKey(ext)) {
            System.out.println(
                    "WARNING: there is a short row which is not compatible to another short row representative.");
            return chaosLocation;
        }

        throw new Error("We did not find a state to transit to");
    }

    private void addTransitionsForState(Location location, ResetTimedTrace accSeq,
                                        Map<ResetTimedTrace, FastImmPair<Location, Set<ResetTimedTrace>>> traceToLocations,
                                        LinkedList<Location> schedule) {
        Map<Input, Set<Map<Edge, Integer>>> frequencySets = new HashMap<>();
        Map<Input, Set<FastImmPair<Double, Map<Edge, Integer>>>> discreteTransitions = new HashMap<>();
        Map<Input, List<Double>> chaosClockValuations = new HashMap<>();
        // may exist two transitions have a complete distribution and a chaos distribution started from the same location, and labeled with the same action, clockValuation
        Map<Input, List<Double>> unambiguousClockValuation = new HashMap<>();
        Map<Input, List<Double>> invalidClockValuations = new HashMap<>();
        for (TimedSuffixTrace singleInputCol : singleInputCols) {
            Input input = singleInputCol.getFirstInput().getInput();
            frequencySets.put(input, new HashSet<>());
            discreteTransitions.put(input, new HashSet<>());
            chaosClockValuations.put(input, new ArrayList<>());
            unambiguousClockValuation.put(input, new ArrayList<>());
            invalidClockValuations.put(input, new ArrayList<>());
        }

        Set<ResetTimedTrace> groupTraces = traceToLocations.get(accSeq).right;
        for (ResetTimedTrace trace : groupTraces) {
            for (TimedInput i : timeInputMap.get(trace)) {
                TimedSuffixTrace iCol = TimedSuffixTrace.empty(i);
                // System.out.println("Adding trans for input " + i);
                Answer answer = teacher.query(trace, iCol);
                if (!answer.isValid()) {
                    if (!unambiguousClockValuation.get(i.getInput()).contains(i.getClockVal())
                            && !chaosClockValuations.get(i.getInput()).contains(i.getClockVal())) {
                        invalidClockValuations.get(i.getInput()).add(i.getClockVal());
                    }
                    continue;
                }
                boolean extensionComplete = answer.isComplete();
                Map<TimedOutput, Integer> outputDistributionForI = answer.getFrequencies();

                List<ResetTimedTrace> extensions = findExt(trace, i);

                if (outputDistributionForI.size() != extensions.size()) {
                    System.out.println(outputDistributionForI.size() + "!=" + extensions.size());
                    System.out.println(extensions);
                    System.out.println(outputDistributionForI);

                    for (ResetTimedTrace ext : extensions) {
                        System.out.println(ext + ":" + longRows.containsKey(ext));
                    }
                    throw new RuntimeException("We do not have the same number of trace extension as "
                            + "entries in the corresponding table cell");
                }
                if (extensions.isEmpty()) {
                    if (!unambiguousClockValuation.get(i.getInput()).contains(i.getClockVal())) {
                        chaosClockValuations.get(i.getInput()).add(i.getClockVal());
                    }
                } else {
                    if (extensionComplete) {
                        unambiguousClockValuation.get(i.getInput()).add(i.getClockVal());
                        Map<Edge, Integer> frequencies = new HashMap<>();
                        for (ResetTimedTrace ext : extensions) {
                            Row rowForExt = shortRows.containsKey(ext) ? shortRows.get(ext) : longRows.get(ext);
                            int observationsForExt = outputDistributionForI.get(ext.getLastOutput());
                            Location target = findStateForExtension(traceToLocations, ext, rowForExt);
                            frequencies.put(new Edge(ext.getLastOutput().isReset(), target), observationsForExt);
                            schedule.add(target);
                        }
                        frequencySets.get(i.getInput()).add(frequencies);
                        discreteTransitions.get(i.getInput()).add(FastImmPair.of(i.getClockVal(), frequencies));
                        chaosClockValuations.get(i.getInput()).remove(i.getClockVal());
                        invalidClockValuations.get(i.getInput()).remove(i.getClockVal());
                    } else {
                        if (!unambiguousClockValuation.get(i.getInput()).contains(i.getClockVal())) {
                            chaosClockValuations.get(i.getInput()).add(i.getClockVal());
                        }
                    }
                }
            }
        }
        constructTransitions(chaosLocation, invalidLocation, compChecker, location, chaosClockValuations, invalidClockValuations, discreteTransitions, frequencySets);
    }

    @Override
    public PTA getFinalHypothesis() {
        return null;
    }
}
