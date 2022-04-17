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
package evaluation;

import utils.FastImmPair;

import java.util.ArrayList;
import java.util.List;

public class EvalResult {
	public EvalResult(String configDescription, List<FastImmPair<String, String>> parameters) {
		super();
		this.configDescription = configDescription;
		this.parameters = parameters;
		actualResults = new ArrayList<>();
	}
	private String configDescription = null;
	private List<FastImmPair<String,String>> parameters = null;
	private List<SingleResult> actualResults = null;
	private int modelSize;
	private long learningTime;
	private long nrSteps;
	private long nrTests;
	private int nrRounds;
	private int nrEq;
	private double passRatio;
	private double klDivergence;
	
	public List<String> export() {
		List<String> exportedLines = new ArrayList<>();

		exportedLines.add("**********************************************************************");
		exportedLines.add("Config: " + configDescription);
		exportedLines.add("**********************************************************************");
		exportedLines.add("-----------");
		exportedLines.add("Parameters:");
		exportedLines.add("-----------");
		
		for(FastImmPair<String, String> p : parameters){
			exportedLines.add(p.getLeft() + ": " + p.getRight());
		}
		exportedLines.add("--------");
		exportedLines.add("Results:");
		exportedLines.add("--------");
		exportedLines.add("learning time: " + learningTime);
		exportedLines.add("# locations: " + modelSize);
		exportedLines.add("# outputs: " + nrSteps);
		exportedLines.add("# tests: " + nrTests);
		exportedLines.add("# rounds: " + nrRounds);
		exportedLines.add("# equivalence queries: " + nrEq);
		exportedLines.add("test case passing ratio: " + passRatio);
		exportedLines.add("the sum of KL Divergence: " + klDivergence);
		
		for(SingleResult r : actualResults){
			exportedLines.add(r.toString());
		}
		return exportedLines;
	}
	public void addResult(SingleResult result){
		actualResults.add(result);
	}
	public long getLearningTime() {
		return learningTime;
	}
	public void setLearningTime(long learningTime) {
		this.learningTime = learningTime;
	}
	public int getModelSize() {
		return modelSize;
	}
	public void setModelSize(int modelSize) {
		this.modelSize = modelSize;
	}
	public void setNrSteps(long nrSteps) {
		this.nrSteps = nrSteps;
	}
	public long getNrTests() {
		return nrTests;
	}
	public void setNrTests(long nrTests) {
		this.nrTests = nrTests;
	}
	public int getNrRounds() {
		return nrRounds;
	}
	public void setNrRounds(int nrRounds) {
		this.nrRounds = nrRounds;
	}
	public void setNrEq(int nrEq) {this.nrEq = nrEq;}
	public double getPassRatio() {
		return passRatio;
	}
	public void setPassRatio(double passRatio) {
		this.passRatio = passRatio;
	}
	public void setKlDivergence(double klDivergence) {
		this.klDivergence = klDivergence;
	}

	public double getKlDivergence() {
		return klDivergence;
	}
}
