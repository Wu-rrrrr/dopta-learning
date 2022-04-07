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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class EvalMeasurement {

	private String typeOfMeasurement;
	private String sulName;
	private List<EvalResult> resultsForConfigs = new ArrayList<>();
	private List<FastImmPair<String, String>> additionalInformation = new ArrayList<>();
	
	public EvalMeasurement(String typeOfMeasurement, String sulName) {
		super();
		this.typeOfMeasurement = typeOfMeasurement;
		this.sulName = sulName;
	}
	
	public void addResult(EvalResult result){
		resultsForConfigs.add(result);
	}
	
	public void addAdditionalInformation(FastImmPair<String, String> addInfo){
		additionalInformation.add(addInfo);
	}
	
	public void persist(String fileName) throws IOException{
		List<String> lines = new ArrayList<>();
		lines.add("************************************************: ");
		lines.add("************************************************: ");
		lines.add("************************************************: ");
		lines.add("Measurement: " + typeOfMeasurement);
		lines.add("SUL: " + sulName);
		lines.add("************************************************: ");
		lines.add("Results with true model & additional information: ");
		lines.add("************************************************: ");
		for(FastImmPair<String, String> addInfo : additionalInformation){
			lines.add(addInfo.getLeft() + "=" + addInfo.getRight());
		}
		for(EvalResult r : resultsForConfigs)
			lines.addAll(r.export());
		File file = new File(fileName);
		BufferedWriter bw = null;
		try {
			bw = new BufferedWriter(new FileWriter(file, true));
			for (String line : lines) {
				bw.write(line+"\n");
			}
			bw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
//		Files.newBufferedWriter(new File(fileName).toPath(), Charset.defaultCharset()).
//		Files.write(new File(fileName).toPath(), lines, Charset.defaultCharset());
	}

}
