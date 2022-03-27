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
package base.teacher.oracle.conv;

import automaton.PTA;
import base.teacher.oracle.ShortTraceToChaosGenerator;
import trace.TimedIncompleteTrace;
import java.util.List;

/**
 * Stopping criterion which stops if either:
 * (1) a minimum number of rounds have been executed and the chaos state is unreachable in the 
 * current hypotheses and the relative number of unambiguous traces (labelling rows in the 
 * observation table) is above a given threshold, or
 * (2) a maximum number of rounds have been executed.
 * 
 *
 * This is the stopping criterion discussed in the paper "L*-Based Learning of Markov Decision 
 * Processes".
 * 
 * @author Martin Tappler
 *
 */
public class RoundBasedAndUnambigTraceCriterion implements ConvergenceCriterion {

	private int maxRounds;
	private double portionOfUnambiguousTraces;
	private int minRounds;

	public RoundBasedAndUnambigTraceCriterion(int minRounds, int maxRounds, double portionOfUnambiguousTraces) {
		this.maxRounds = maxRounds;
		this.minRounds = minRounds;
		this.portionOfUnambiguousTraces = portionOfUnambiguousTraces;
	}

	@Override
	public boolean converged(int rounds, PTA hypo, double unambiguousRatio) {
		System.out.println("Unambiguous short rows: " + unambiguousRatio);

		double regionNormalization = 0.1;
		List<TimedIncompleteTrace> tracesToChaos = ShortTraceToChaosGenerator.findTracesToChaos(hypo, regionNormalization);
		return (rounds >= minRounds && tracesToChaos.isEmpty() && (unambiguousRatio > portionOfUnambiguousTraces)) || rounds >= maxRounds;
	}

	@Override
	public String description() {
		return String.format("round-based-unambig(min=%d,max=%d,ratio-unambig=%.2f)",
				minRounds,maxRounds,portionOfUnambiguousTraces);
	}
	

}
