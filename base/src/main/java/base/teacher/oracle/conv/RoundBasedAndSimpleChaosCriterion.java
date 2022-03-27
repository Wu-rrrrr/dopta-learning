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
 * current hypotheses, or
 * (2) a maximum number of rounds have been executed.
 * 
 * @author Martin Tappler
 *
 */
public class RoundBasedAndSimpleChaosCriterion implements ConvergenceCriterion {

	private final int minRounds;
	private final int maxRounds;

	public RoundBasedAndSimpleChaosCriterion(int minRounds, int maxRounds) {
		this.minRounds = minRounds;
		this.maxRounds = maxRounds;
	}

	@Override
	public boolean converged(int rounds, PTA hypo, double unambiguousRatio) {
		double regionNormalization = 0.1;
		List<TimedIncompleteTrace> tracesToChaos = ShortTraceToChaosGenerator.findTracesToChaos(hypo, regionNormalization);
		return (tracesToChaos.isEmpty() && rounds >= minRounds) || rounds >= maxRounds;
	}

	@Override
	public String description() {
		return String.format("round-based-simple-chaos(min=%d,max=%d)", minRounds,maxRounds);
	}
	

}
