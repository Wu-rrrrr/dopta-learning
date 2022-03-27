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
package evaluation.prism;

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

import automaton.*;
import com.google.common.collect.BoundType;

public class PrismModelExporter {

	private final StringBuilder code = new StringBuilder("pta\r\n");

	public static void convert (PTA pta, String savePath, String modelName) {
		PrismModelExporter util = new PrismModelExporter();

		Set<Location> locations = pta.getLocations();
		Map<String, Set<Integer>> locationLabels = new HashMap<>();
		locations.forEach(location -> {
			locationLabels.putIfAbsent(location.getLabel().getSymbol(), new HashSet<>());
			Set<Integer> locationId = locationLabels.get(location.getLabel().getSymbol());
			locationId.add(location.getId());
		});
		for (Map.Entry<String, Set<Integer>> entry : locationLabels.entrySet()) {
			util.node(entry.getKey(), entry.getValue());
		}

		util.module(modelName, pta.getLocations().size(), pta.getInitial().getId(), pta.getLocations());

		util.saveCodeToFile(savePath);
	}

	public void node (String label, Set<Integer> values) {
		StringBuilder nodeCode = new StringBuilder(String.format("label \"%s\" = ", label));
		values.forEach(value -> {
			String node = String.format("loc=%d|", value);
			nodeCode.append(node);
		});
		nodeCode.deleteCharAt(nodeCode.lastIndexOf("|"));
		nodeCode.append(";\r\n");
		this.code.append(nodeCode);
	}

	public void module (String name, int locationNum, int init, Set<Location> locations) {
		code.append("module ");
		code.append(String.format("%s \r\n", name));
		code.append(String.format("loc:[0..%d] init %d;\r\n", locationNum - 1, init));
		code.append("x:clock;\r\n");
		for (Location source : locations) {
			for (Input input : source.getTransitions().keySet()) {
				Map<Guard, Map<Edge, Double>> successors = new HashMap<>();
				for (Transition succ : source.getTransitions().get(input)) {
					successors.putIfAbsent(succ.getGuard(), new HashMap<>());
					successors.get(succ.getGuard()).put(new Edge(succ.isReset(), succ.getTarget()), succ.getProbability());
				}
				for (Map.Entry<Guard, Map<Edge, Double>> successor : successors.entrySet()) {
					String guard = guardCode(successor.getKey());
					String updates = updates(successor.getValue());
					link(source.getId(), input.getSymbol(), guard, updates);
				}
			}
		}
		code.append("endmodule\r\n");
	}
	public void link (int source, String action, String guard, String updates) {
		String linkCode = String.format("[%s] loc=%d & %s -> %s;\r\n", action, source, guard, updates);
		code.append(linkCode);
	}

	public String guardCode (Guard guard) {
		StringBuilder guardCode = new StringBuilder();
		for (Interval interval : guard.getIntervals()) {
			if (interval.lowerBoundType() == BoundType.CLOSED) {
				guardCode.append(String.format("x>=%d", (int)(interval.lowerEndpoint())));
			} else {
				guardCode.append(String.format("x>%d", (int)(interval.lowerEndpoint())));
			}
			if (interval.hasBound()) {
				guardCode.append(" & ");
				if (interval.upperBoundType() == BoundType.CLOSED) {
					guardCode.append(String.format("x<=%d", (int)(interval.upperEndpoint())));
				} else {
					guardCode.append(String.format("x<%d", (int)(interval.upperEndpoint())));
				}
			}
			guardCode.append(" | ");
		}
		guardCode.deleteCharAt(guardCode.lastIndexOf("|"));
		return guardCode.toString();
	}

	public String updates (Map<Edge, Double> distribution) {
		StringBuilder updates = new StringBuilder();
		for (Map.Entry<Edge, Double> entry : distribution.entrySet()) {
			String update = String.format("%.3f:(loc'=%d)", entry.getValue(), entry.getKey().getTarget().getId());
			updates.append(update);
			if (entry.getKey().isReset()) {
				updates.append("&(x'=0)");
			}
			updates.append(" + ");
		}
		updates.deleteCharAt(updates.lastIndexOf("+"));
		return updates.toString();
	}

	public void saveCodeToFile (String targetPath) {
		String content = code.toString();
		FileWriter fwriter = null;
		try {
			// true表示不覆盖原来的内容，而是加到文件的后面。若要覆盖原来的内容，直接省略这个参数就好
			fwriter = new FileWriter(targetPath);
			fwriter.write(content);
		} catch (IOException ex) {
			ex.printStackTrace();
		} finally {
			try {
				fwriter.flush();
				fwriter.close();
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
	}
}
