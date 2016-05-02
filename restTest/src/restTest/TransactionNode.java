package restTest;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * @author Ammar Javed
 * This node class was made with the intention of using Linked Lists as a representation of 
 * the transactions provided by the API. 
 *
 */
class TransactionNode {
	
	Date date;
	
	String ledger;
	
	double amount;
	
	String company;
	
	TransactionNode next;
	
	public TransactionNode(String d, String l, String a, String c) {
		
		// Parse the string date into a comparable format
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
		try {
			this.date = dateFormat.parse(d);
		} catch (ParseException e) {
			System.out.println("Unable to format date:" + d);
			e.printStackTrace();
		}
		
		this.ledger = l;
		
		this.amount = Double.parseDouble(a);
		
		this.company = c;
		
		this.next = null;
	}
}
