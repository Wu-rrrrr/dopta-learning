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

import utils.FastImmPair;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class PrismInterface {

	private String pathToPrism;
	private String modelFileName;
	private String propertyFileName;
	
	public PrismInterface(String pathToPrism, String modelFileName, String propertyFileName) {
		super();
		this.pathToPrism = pathToPrism;
		this.modelFileName = modelFileName;
		this.propertyFileName = propertyFileName;
	}

	public static final Pattern PRISM_PROB_CALC = Pattern
			.compile("Value in the initial state: (1\\.\\d+|1|0\\.0|0|0\\.\\d+|\\d\\.\\d+E-\\d)");
	public static final Pattern PROPERTY_CHECK_REGEX = Pattern.compile("Model checking: (P(max|min)=\\? \\[.+\\])");
	
	public FastImmPair<String, Double> computeProbability(int propertyIndex) throws IOException{
	//		String prismCall = String.format("%s %s %s -prop %d",pathToPrism,modelFileName,propertyFileName, propertyIndex);
//		Process p = Runtime.getRuntime().exec(prismCall);
		ProcessBuilder pb = new ProcessBuilder(pathToPrism.trim(), modelFileName, propertyFileName,
				"-prop",  Integer.toString(propertyIndex));
		Process p = pb.start();
		BufferedReader reader = 
	         new BufferedReader(new InputStreamReader(p.getInputStream()));
	    String line = "";
	    double result = -1;
	    String propertyString = "Pmax[prop] ";
	    while ((line = reader.readLine())!= null) {
    		if(line.contains("Value in the initial state")){
    			Matcher m = PRISM_PROB_CALC.matcher(line);
    			m.matches();
    			result = Double.parseDouble(m.group(1));
    		}
    		if(line.contains("Model checking: ")){
    			Matcher m = PROPERTY_CHECK_REGEX.matcher(line);
    			m.matches();
    			String extractedProp = m.group(1);
    			propertyString = extractedProp.replace("Pmax=? ", "Pmax").replace("Pmin=?", "Pmin");
    		
    		}
	    }
	    if(result < 0){
	    	propertyString = "Could not parse Prism's results.";
	    }
	    reader.close();
	    return new FastImmPair<String, Double>(propertyString, result);
	}

	public List<FastImmPair<String,Double>> computeProbabilities(List<Integer> propertyIndexes) throws IOException{
		List<FastImmPair<String, Double>> result = new ArrayList<>();
		for(Integer index : propertyIndexes){
			result.add(computeProbability(index));
		}
		return result;
	}
	public List<FastImmPair<String,Double>> computeAllProbabilities() throws IOException{
		int nrProperties = (int) countProperties();
		List<Integer> propertyIndexes = IntStream.range(1, nrProperties + 1).boxed().collect(Collectors.toList());
		return computeProbabilities(propertyIndexes);
	}

	private long countProperties() throws IOException {
		List<String> propFileLines = Files.readAllLines(new File(propertyFileName).toPath());
		return propFileLines.stream().filter(propertyLine -> propertyLine.trim().length() > 0).count();
	}
}
