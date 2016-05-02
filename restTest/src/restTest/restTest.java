package restTest;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import org.json.*;


public class restTest {
	
	/*
	 * Hashmap which holds all transactions for each unique date
	 */
	public static HashMap<String, ArrayList<TransactionNode>> transactionsMap;
	
	/*
	 * Flag settings passed through command line argument
	 */
	static boolean verbose = false;
	static boolean daily = false;
	static boolean dailyVerbose = false;
	static boolean expenseList = false;
	
	public static void main(String[] args) {
				
		parseArguments(args);
		
		// total number of transactions
		int totalCount;
		
		// Arraylist holding JSONObjects for each page retrieved from endpoint
		ArrayList<JSONObject> pages;

		int currentPage = 1;
		JSONObject page = getTransactionsJSONObject(Constants.API_ENDPOINT + currentPage + ".json");
		
		if (page != null) {
			try {
				
				
				// Calculate how many pages we need to read from the endpoint 
				pages = new ArrayList<JSONObject>();
				pages.add(page);
				
				totalCount = page.getInt(Constants.PAGE_TOTAL_COUNT_KEY);
				
				if (verbose) {
					System.out.println("Total transactions:");
					System.out.println(totalCount);
				}

				// Number of transactions each page contains
				// Keeps the code dynamic assuming each page doesn't always contain 10 transactions
				JSONArray transOnPage = page.getJSONArray(Constants.TRANSACTION_KEY);
				int transactionsLeft = calculateTransactionsLeft(totalCount, transOnPage.length());
				
				// Get the next page if there's still transactions left to retrieve (in pages)
				while (transactionsLeft > 0) {
					currentPage++;
					page = getTransactionsJSONObject(Constants.API_ENDPOINT + currentPage + ".json");
					
					transOnPage = page.getJSONArray(Constants.TRANSACTION_KEY);

					transactionsLeft = calculateTransactionsLeft(transactionsLeft, transOnPage.length());		
					
					pages.add(page);
				}
				
				if (verbose) {
					System.out.println("Total pages:");
					System.out.println(pages.size() + "\n\n");
				}
				
				// Parse transactions and store them by date
				transactionsMap = new HashMap<String, ArrayList<TransactionNode>>();
				parseTransactions(pages);
				
				//Print the Expense list if verbose or exp flags were set
				if (expenseList || verbose)
					calculateAndPrintExpenseList();
					
				// Print the total balance.
				calculateAndPrintBalance();

			} catch (JSONException e) {
				e.printStackTrace();
			}			
		}

	}

	/**
	 * Parses commandline arguments and turns on relevant internal flags
	 * @param args command line arguments passed to main
	 */
	private static void parseArguments(String[] args) {
		if (args.length < 1) {
			return;
		}
		
		for (String arg : args) {	
			switch (arg) {
				case Constants.VERBOSE:
					verbose = true;
					break;
				case Constants.VERBOSE_DAILY_BALANCE:
					dailyVerbose = true;
					break;
				case Constants.DAILY_BALANCE:
					daily = true;
					break;
				case Constants.EXPENSE_LIST:
					expenseList = true;
					break;
				default:
					System.out.println("Bad argument" + arg);
					System.out.println("The correct usage is:\n");
					System.out.println("java -jar restTest.jar [-v] [--daily] [--cat] [--verbose-daily]");
					System.out.println("The order of arguments does not matter.\n");
					throw new IllegalArgumentException("Illegal Command Line Argument: " + arg);				
			}
		}
	}
	
	/**
	 * Connect to API and retrieve pages of transactions
	 * @param urlString
	 * @return JSONObject retrieved from API
	 */
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
				urlConnection.setReadTimeout(60000); // Timeout for response
			
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
	
	/**
	 * Calculate how many transactions are left
	 * @param currentTotal Transactions expected
	 * @param transOnPage Transactions on current page
	 * @return # of transactions left [to load]
	 */
	private static int calculateTransactionsLeft(int currentTotal, int transOnPage) {
		return currentTotal - transOnPage;
	}
	
	/**
	 * Parses the JSON objects retrieved from the API, creates TransactionNodes for each
	 * to store them in memory and adds them in the transactionsMap (HashMap) under appropriate day
	 * @param pages List containing all pages loaded from API
	 */
	private static void parseTransactions(ArrayList<JSONObject> pages) {
		
		HashSet<String> uniqueTrans = new HashSet<String>();
		
		for (JSONObject page : pages) {
			try {
				
				JSONArray transactions = page.getJSONArray(Constants.TRANSACTION_KEY);
				
				for (int i = 0; i < transactions.length(); i++) {
					JSONObject trans = transactions.getJSONObject(i);
					
					String date = trans.getString(Constants.TRANSACTION_DATE_KEY);
					String ledger = trans.getString(Constants.TRANSACTION_LEDGER_KEY);
					String amount = trans.getString(Constants.TRANSACTION_AMOUNT_KEY);
					String company = trans.getString(Constants.TRANSACTION_COMPANY_KEY);
					
					//Create new node which will convert/format the date and amount as necessary
					TransactionNode transaction = new TransactionNode(date, ledger, amount, company);
					
					boolean unique = uniqueTrans.add(date+ledger+amount+company);
					
					if (!unique)
						transaction.declareDuplicate(true);
					
					// Add transaction node to hashmap
					if(transactionsMap.containsKey(date)) {
						// Add to hashmap
						transactionsMap.get(date).add(transaction);
					} else {
						// Create new entry
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
	
	/**
	 * Formats arguments passed on as evenly spaced columns
	 * @param columns Variable argument; each column to print and format
	 */
	private static void printAndFormatColumns(String... columns) {
		// Assume no string is larger than 25 characters
		String spaces = "                         ";
		
		for (String col : columns) {
			System.out.print(col);
			if (col.length() < spaces.length())
				System.out.print(spaces.substring(col.length()));
		}
		System.out.println("");
	}
	
	/**
	 * Format and print appropriate header for Daily balance report
	 */
	private static void printDailyBalanceHeader() {
		if (verbose || daily || dailyVerbose) {
			System.out.println("\n\n");
			printAndFormatColumns("", "Daily Balance Report", "");
			printAndFormatColumns("", "--------------------", "");
			System.out.println("\n\n");
		}
		
		if (!verbose && daily) {
			printAndFormatColumns("Daily Balance", "Payments", "Expenses", "Date");
			printAndFormatColumns("-------------", "--------", "--------", "----");
		} else if (verbose || dailyVerbose) {
			printAndFormatColumns("Company", "Daily Balance", "Payments", "Expenses", "Date");
			printAndFormatColumns("-------", "-------------", "--------", "--------", "----");
		}
	}
	
	/**
	 * Print Summary report headers
	 */
	private static void printSummaryHeader() {
		
		printAndFormatColumns("", "Summary Report", "");
		printAndFormatColumns("", "--------------", "");
		System.out.println("\n");
		printAndFormatColumns("Final Balance", "Total Payments", "Total Expenses");
		printAndFormatColumns("-------------", "--------------", "--------------");
		
	}
	
	/**
	 * Print Expense report header
	 */
	private static void printExpenseReportHeader() {

		printAndFormatColumns("", "Expense Report", "");
		printAndFormatColumns("", "--------------", "");
		System.out.println("");
		
	}

	/**
	 * Calculates daily and total balance.
	 * It brings the daily balance only if the flag is set, otherwise 
	 * only calculates and prints the final summary
	 */
	private static void calculateAndPrintBalance(){		
		// Sort dates stored in transactionsMap
		ArrayList<String> dates = new ArrayList<String>(transactionsMap.keySet());
		java.util.Collections.sort(dates);
		ArrayList<TransactionNode> transactions;
		
		// Nifty decimal formatting pattern found online!
		DecimalFormat currencyFormat = new DecimalFormat("$#,##0.00;-$#,##0.00");
		
		// Totals
		double totalBalance = 0;
		double totalExpenses = 0;
		double totalPayments = 0;
		
		// Daily figures
		double dailyBalance;
		double dailyExpenses;
		double dailyPayments;
		
		printDailyBalanceHeader();
		
		for (String date : dates) {
			transactions = transactionsMap.get(date);
			
			dailyBalance = 0;
			dailyExpenses = 0;
			dailyPayments = 0;
			
			for (TransactionNode trans : transactions) {
				
				dailyBalance += trans.amount;
				
				if (trans.amount < 0) {
					
					if (verbose || dailyVerbose)
						printAndFormatColumns(trans.getFormattedCompanyName(),
											  "",
											  "",
											  currencyFormat.format(trans.amount),
											  date);
					
					dailyExpenses += trans.amount;
				} else {
					if (verbose || dailyVerbose)
						printAndFormatColumns(trans.getFormattedCompanyName(),
											  "",
											  currencyFormat.format(trans.amount),
											  "",
											  date);
					
					dailyPayments += trans.amount;
				}
			}
			
			totalBalance += dailyBalance;
			totalExpenses += dailyExpenses;
			totalPayments += dailyPayments;
			
			if (!verbose && daily){
				
				printAndFormatColumns(currencyFormat.format(totalBalance),
									  currencyFormat.format(dailyPayments),
								   	  currencyFormat.format(dailyExpenses),
								   	  date);
				System.out.println("");
				
			} else if (verbose || dailyVerbose) {
				
				System.out.println("");
				printAndFormatColumns("Total", 
									  currencyFormat.format(dailyBalance),
									  currencyFormat.format(dailyPayments),
									  currencyFormat.format(dailyExpenses),
									  "");
				System.out.println("");
				
			}
		}
		
		printSummaryHeader();
		
		printAndFormatColumns(currencyFormat.format(totalBalance),
							  currencyFormat.format(totalPayments),
							  currencyFormat.format(totalExpenses));
	}

	/**
	 * Calculates and prints the expense list
	 */
	private static void calculateAndPrintExpenseList() {
		HashMap<String, ArrayList<TransactionNode>> expenseMap = groupTransactionsByExpense();
		
		// Nifty decimal formatting pattern found online!
		DecimalFormat currencyFormat = new DecimalFormat("$#,##0.00;-$#,##0.00");
		
		printExpenseReportHeader();
		
		for (String expense : expenseMap.keySet()) {
			System.out.println(expense);
			System.out.println("");
			
			printAndFormatColumns("", "Name", "Expense");
			printAndFormatColumns("", "----", "-------");
			
			double ledgeExpense = 0;
			
			ArrayList<TransactionNode> transactions = expenseMap.get(expense);
			for (TransactionNode trans : transactions) {
				printAndFormatColumns("", 
									  trans.getFormattedCompanyName(), 
									  currencyFormat.format(trans.amount));
				
				ledgeExpense += trans.amount;
			}
			
			System.out.println("");
			printAndFormatColumns("Total", "", currencyFormat.format(ledgeExpense));
			System.out.println("");
		}
		
	}
	
	/**
	 * Helper function which changes groupings for transactions (Expenses instead of date)
	 * It removes all payments from the final map since this only considers expenses.
	 * @return new hashmap with the Ledger as the key
	 */
	private static HashMap<String, ArrayList<TransactionNode>> groupTransactionsByExpense(){
		HashMap<String, ArrayList<TransactionNode>> newMap = new HashMap<String, ArrayList<TransactionNode>>();
		
		ArrayList<TransactionNode> transDate;
		for (String date : transactionsMap.keySet()) {
			transDate = transactionsMap.get(date);
			
			for (TransactionNode node : transDate) {
				if (!node.ledger.equals("")) {
					if (newMap.containsKey(node.ledger)) {
						newMap.get(node.ledger).add(node);
					} else {
						ArrayList<TransactionNode> newList = new ArrayList<TransactionNode>();
						newList.add(node);
						newMap.put(node.ledger, newList);
					}
				}
			}
		}
		return newMap;
	}
}
