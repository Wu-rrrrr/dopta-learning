/*******************************************************************************
 * Lmdp* - L*-Based Learning of Markov Decision Processes
 *  Copyright (C) 2019 TU Graz
 *  
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *  
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *  
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/
package util.export;

import automaton.*;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Set;

/**
 * Class for exporting Markov decision processes in Graphviz dot-format.
 * 
 * @author Martin Tappler
 *
 */
public class DotExporter {

	private static final String stateLabelFormatString = // "%s / %s";
			"<<TABLE BORDER=\"0\" CELLSPACING=\"0\">"
					+ "<TR><TD>%s</TD></TR><TR><TD>%s</TD></TR></TABLE>>";

	public String stateLabel(Location location) {
		return String.format(stateLabelFormatString, location.getId(), location.getLabel().getSymbol().replace("&", "_"));
	}

	public String stateString(Location location) {
		return String.format("%s [shape=\"circle\" margin=0 label=%s];", location.getId(), stateLabel(location));
	}

	public String toDot(PTA pta) {
		StringBuilder sb = new StringBuilder();
		appendLine(sb, "digraph g {");
		appendLine(sb, "__start0 [label=\"\" shape=\"none\"];");
		Set<Location> locations = pta.getLocations();
		locations.forEach(l -> appendLine(sb, stateString(l)));
		locations.forEach(l -> appendTransitionLines(sb, l));

		appendLine(sb, String.format("__start0 -> %s;", Integer.toString(pta.getInitial().getId())));
		appendLine(sb, "}");
		return sb.toString();
	}

	private void appendTransitionLines(StringBuilder sb, Location l) {
		l.getTransitions().forEach((i, t) -> t.forEach(tp -> appendLine(sb, transitionString(tp))));
	}

	private String transitionString(Transition t) {
		return String.format("%s -> %s [label=\"%s,%s,%.2f\"];", Integer.toString(t.getSource().getId()),
				Integer.toString(t.getTarget().getId()), t.getInput().getSymbol(), t.getGuard(), t.getProbability());
	}

	public static void appendLine(StringBuilder sb, String line) {
		sb.append(line);
		sb.append(System.lineSeparator());
	}

	public void writeToFile(PTA pta, String fileName) throws IOException {
		File f = new File(fileName);
		f.getParentFile().mkdirs();
		try (FileWriter fw = new FileWriter(f)) {
			BufferedWriter bw = new BufferedWriter(fw);
			bw.write(toDot(pta));
			bw.flush();
		}
		generateGraph(fileName);
	}

	public void generateGraph(String fileName) throws IOException {
		Runtime run = Runtime.getRuntime();
		run.exec(String.format("dot %s -T pdf -o %s", fileName, fileName.replace(".dot", ".pdf")));
	}

}
