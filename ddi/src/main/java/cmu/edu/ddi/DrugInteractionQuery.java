package cmu.edu.ddi;

import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;

public class DrugInteractionQuery {
	
	private final static String FILENAME = "/RxCUI_levels.csv";
	private final static String RESULT_FILE = "DDI.csv";
	private final static String QUERY_URL = "https://rxnav.nlm.nih.gov/REST/interaction/interaction.json?";
	private static Map<String, Integer> rxcuiMap = new HashMap<String, Integer>();
	
	private static String findInteractionByRxcui(String rxcui) throws Exception {
		StringBuffer response = new StringBuffer();
		String urlEncoding = QUERY_URL + "rxcui="+rxcui;
		URL queryURL = new URL(urlEncoding);
		HttpURLConnection con  = (HttpURLConnection) queryURL.openConnection();
		con.setRequestMethod("GET");
		BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
		String line;
		while ((line = in.readLine()) != null) {
			response.append(line);
		}
		in.close();
		return response.toString();
	}
	
	private static void writeResult(CSVWriter writer, String response, String rxcui) {
		try {
			JSONObject jo = new JSONObject(response);
			JSONArray ja = jo.getJSONArray("interactionTypeGroup");
			for(int i = 0; i < ja.length(); i++) {
				JSONObject interactionType = ja.getJSONObject(i);
				JSONArray interactionArr = interactionType.getJSONArray("interactionType");
				for(int j = 0; j < interactionArr.length();j++) {
					JSONObject interactionPairJo = interactionArr.getJSONObject(j);
					JSONArray interactionPariJa = interactionPairJo.getJSONArray("interactionPair");
					for(int k = 0; k < interactionPariJa.length(); k++) {
						JSONObject interaction = interactionPariJa.getJSONObject(k);
						String severity = interaction.getString("severity");
						String description = interaction.getString("description");
						JSONArray interactionConcept = interaction.getJSONArray("interactionConcept");
						JSONObject minConceptItem1 = interactionConcept.getJSONObject(0).getJSONObject("minConceptItem");
						String name1 = minConceptItem1.getString("name");
						JSONObject minConceptItem2 = interactionConcept.getJSONObject(1).getJSONObject("minConceptItem");
						String rxcui2 = minConceptItem2.getString("rxcui");
						String name2 = minConceptItem2.getString("name");
						// drug1, drug1 name, drug2, drug2 name, severity, description
						String[] res = {rxcui, name1, rxcui2, name2, severity, description};
						writer.writeNext(res);
					}
				}
				
			}		
		}catch(Exception e) {
//			e.printStackTrace();
			System.out.println(rxcui);
		}
	}

	public static void main(String[] args) {
		CSVReader reader = null;
		CSVWriter writer = null;
		try{
			InputStream is = DrugInteractionQuery.class.getResourceAsStream(FILENAME);
			reader = new CSVReader(new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8)));
			String[] line;
			writer = new CSVWriter(new FileWriter(RESULT_FILE));
			reader.readNext(); // skip fields name
			String[] fields = {"Drug1", "Drug1 Name", "Drug2", "Drug2 Name", "Severity", "Description"};
			writer.writeNext(fields);
			while((line = reader.readNext()) != null) {
				String rxcui = line[20];
				if(rxcui.equals("")) {
					continue;
				}
				if(rxcui.split(";").length > 0) {
					String[] rxcuiArr = rxcui.split(";");
					for(int i = 0; i < rxcuiArr.length; i++){
						if(!rxcuiArr[i].equals("")) {
							rxcui = rxcuiArr[i];
							if(!rxcuiMap.containsKey(rxcui)) {
								rxcuiMap.put(rxcui, 1);
								String response = findInteractionByRxcui(rxcui);
								writeResult(writer, response, rxcui);
							}
						}
					}
				} else {
					if(!rxcuiMap.containsKey(rxcui)) {
						rxcuiMap.put(rxcui, 1);
						String response = findInteractionByRxcui(rxcui);
						writeResult(writer, response, rxcui);
					}
				}
			}
		} catch(Exception e) {
			e.printStackTrace();
		}finally {
			if(reader != null) {
				try {
					reader.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if(writer != null) {
				try {
					writer.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

	}

}
