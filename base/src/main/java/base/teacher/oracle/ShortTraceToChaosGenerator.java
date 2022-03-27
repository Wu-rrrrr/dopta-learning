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
package base.teacher.oracle;

import automaton.Location;
import automaton.Output;
import automaton.PTA;
import automaton.Transition;
import lombok.AllArgsConstructor;
import trace.TimedIncompleteTrace;
import trace.TimedInput;
import utils.FastImmPair;
import java.util.*;

/**
 * Equivalence query strategy that computes traces to the chaos and tries to test those traces. 
 * 
 * It is currently not used in experiments, since it is rather inefficient. Performing refine 
 * queries, i.e. filling the table based table information, is more efficient. 
 * @author Martin Tappler
 *
 */
@AllArgsConstructor
public class ShortTraceToChaosGenerator {

	public static List<TimedIncompleteTrace> findTracesToChaos(PTA hypo, double regionNormalization) {
		LinkedList<FastImmPair<Location,TimedIncompleteTrace>> schedule = new LinkedList<>();
		schedule.add(FastImmPair.of(hypo.getInitial(), TimedIncompleteTrace.empty()));
		Set<Location> visited = new HashSet<>();
		List<TimedIncompleteTrace> tracesToChaos = new ArrayList<>();
		while(!schedule.isEmpty()){
			FastImmPair<Location, TimedIncompleteTrace> current = schedule.removeFirst();
			Location currentState = current.getLeft();
			TimedIncompleteTrace currentTrace = current.getRight();
			visited.add(currentState);
			for (Transition succ : currentState.getAllTransitions()) {
				List<Double> endpointsOfGuard = succ.getGuard().getEndpoints(regionNormalization);
				if(succ.getTarget().getLabel().equals(Output.chaos())){
					for (double endpoint : endpointsOfGuard) {
						tracesToChaos.add((TimedIncompleteTrace) currentTrace.append(FastImmPair.of(currentState.getLabel(), TimedInput.create(succ.getInput().getSymbol(), endpoint))));
					}
				}
				else if(!visited.contains(succ.getTarget())){
					schedule.add(FastImmPair.of(succ.getTarget(),
							(TimedIncompleteTrace) currentTrace.append(FastImmPair.of(currentState.getLabel(), TimedInput.create(succ.getInput().getSymbol(), endpointsOfGuard.get(0))))));
				}
			}
		}
//		System.out.println("*** Trace to chaos ***");
//		System.out.println(tracesToChaos);
		return tracesToChaos;
	}
}
