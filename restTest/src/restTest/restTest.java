package restTest;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;

import org.json.*;


public class restTest {
	
	public static HashMap<String, ArrayList<TransactionNode>> transactionsMap;

	public static void main(String[] args) {
		
		int totalCount;
		
		ArrayList<JSONObject> pages;

		int currentPage = 1;
		JSONObject page = getTransactionsJSONObject(Constants.API_ENDPOINT + currentPage + ".json");
		
		if (page != null) {
			try {
				
				/*
				 * Calculate how many pages we need to read from the endpoint 
				 */
				
				// Init new arraylist to keep track of all pages
				pages = new ArrayList<JSONObject>();
				pages.add(page);
				
				totalCount = page.getInt(Constants.PAGE_TOTAL_COUNT_KEY);
				System.out.println("Total transactions:");
				System.out.println(totalCount);

				
				JSONArray transOnPage = page.getJSONArray(Constants.TRANSACTION_KEY);

				int transactionsLeft = calculateTransactionsLeft(totalCount, transOnPage.length());
				System.out.println("Transactions Left to load:");
				System.out.println(transactionsLeft);
				
				while (transactionsLeft > 0) {
					currentPage++;
					page = getTransactionsJSONObject(Constants.API_ENDPOINT + currentPage + ".json");
					
					transOnPage = page.getJSONArray(Constants.TRANSACTION_KEY);

					transactionsLeft = calculateTransactionsLeft(transactionsLeft, transOnPage.length());		
					System.out.println(transactionsLeft);
					
					pages.add(page);
				}
				
				System.out.println("Total pages:");
				System.out.println(pages.size());
				
				/*
				 * Parse transactions and store them by date
				 */
				
				parseTransactions(pages);
				
				
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}			
		}
		
		

	}
	
	private static int calculateTransactionsLeft(int currentTotal, int transOnPage) {
		return currentTotal - transOnPage;
	}
	
	private static void parseTransactions(ArrayList<JSONObject> pages) {
		
		for (JSONObject page : pages) {
			try {
				
				JSONArray transactions = page.getJSONArray(Constants.TRANSACTION_KEY);
				
				for (int i = 0; i < transactions.length(); i++) {
					JSONObject trans = transactions.getJSONObject(i);
					
					String date = trans.getString(Constants.TRANSACTION_DATE_KEY);
					String ledger = trans.getString(Constants.TRANSACTION_LEDGER_KEY);
					String amount = trans.getString(Constants.TRANSACTION_AMOUNT_KEY);
					String company = trans.getString(Constants.TRANSACTION_COMPANY_KEY);
					
					TransactionNode transaction = new TransactionNode(date, ledger, amount, company);
					
					if(transactionsMap.containsKey(date)) {
						transactionsMap.get(date).add(transaction);
					} else {
						ArrayList<TransactionNode> listOfTransactions = new ArrayList<TransactionNode>();
						listOfTransactions.add(transaction);
						transactionsMap.put(date, listOfTransactions);
					}
				}
				
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
	}
	
	private static JSONObject getTransactionsJSONObject(String urlString) {

		StringBuilder sb = new StringBuilder();
		URLConnection urlConnection = null;
		InputStreamReader input = null;
		
		/*
		 * Read JSON from URL
		 */
		try {
			URL url = new URL(urlString);
			
			urlConnection = url.openConnection();
			
			if (urlConnection != null)
				urlConnection.setReadTimeout(60000);
			
			if (urlConnection != null && urlConnection.getInputStream() != null) {
				
				input = new InputStreamReader(urlConnection.getInputStream(),
											  Charset.defaultCharset());
				
				BufferedReader bufferedReader = new BufferedReader(input);
				if (bufferedReader != null) {
					int cp;
					while ((cp = bufferedReader.read()) != -1) {
						sb.append((char) cp);
					}
					bufferedReader.close();
				}
			}
			
			input.close();
			
		} catch (Exception e) {
			throw new RuntimeException("ERROR: Couldn't reach "+ urlString, e);
		} 
		
		JSONObject transObj = null;
		
		/*
		 * Create JSON object from string read from URL
		 * If it fails, return a null
		 */
		try {
			transObj = new JSONObject(sb.toString());
		} catch (Exception e) {
			
		}
		
		return transObj;
	}

}
