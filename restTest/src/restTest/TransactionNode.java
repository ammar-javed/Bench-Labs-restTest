package restTest;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
	
	String formattedCompany;
	
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
		
		this.formattedCompany = validateCompanyName(c);
	}
	
	/**
	 * Company original Company name from transaction against a regular expression.
	 * If it matches, we may have a potentially readable name with extra garbage values removed.
	 * 
	 * If it doesn't match, we will return the original company name.
	 * 
	 * @param c Raw company name
	 * @return extracted Company name, or old company name if none were found
	 */
	private String validateCompanyName(String c) {
		// Explanation of Regular expression can be found here: https://regex101.com/r/bA2tD7/1
		String pattern = "^[.@&]?[a-zA-Z0-9 ]+[ '!.@&()]?[ a-zA-Z0-9!()]+";
		
		Pattern p = Pattern.compile(pattern);
		
		Matcher m = p.matcher(c);
		
		if (m.find()){
			return m.group(0);
		}
		
		return c;
	}
	
	public String getFormattedCompanyName(){
		
		if (this.formattedCompany.length() > 15){
			return this.formattedCompany.substring(0, 13) + "...";
		}
		
		return this.formattedCompany;
	}
}
