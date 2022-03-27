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

/**
 * This stopping criterion checks if a maximum number of rounds have been executed. Learning stopped
 * regardless of the current state of the hypothesis and observation table. 
 * 
 * @author Martin Tappler
 *
 */
public class RoundBasedCriterion implements ConvergenceCriterion {

	private final int maxRounds;

	public RoundBasedCriterion(int maxRounds) {
		this.maxRounds = maxRounds;
	}

	@Override
	public boolean converged(int rounds, PTA hypo, double unambiguousRatio) {
		return rounds == maxRounds;
	}

	@Override
	public String description() {
		return String.format("max-round(max=%d)",
				maxRounds);
	}
}
