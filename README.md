# Bench-Labs-restTest
API &amp; Data Transformation Test

A coding exercise for Bench Labs. This program reads financial 'pages' from the API located at http://resttest.bench.co/transactions/ and calculates the Final balance after all payments and expenses.

To run this program, clone this repository to your computer, or just download the restTest.jar file. From within the directory containing the .jar file, run:

java -jar restTest.jar

This will output the Final Balance, Total Payments and Total Expenses report.

There are  multiple flags which are supported:

java -jar restTest.jar [-v] [--daily] [--verbose-daily]

The -v (verbose) flag outputs everything, ranging from number of pages read from the API, to every single daily transaction information.

The --daily flag outputs a calculated Daily Balance.

The --verbose-daily flag outputs calculated Daily Balances along with each inidividual transactions as well.
