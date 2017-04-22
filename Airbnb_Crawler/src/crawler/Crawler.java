package crawler;

import java.io.File;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class Crawler {
	private static Connection _connection;
	private static final String[] GROUP_ONE = { "AL", "AR" };
	private static final String[] GROUP_TWO = { "AK", "AZ", "MI", "NY" };
	private static final String[] GROUP_THREE = { "CO", "GA", "PA", "UT" };
	private static final String[] GROUP_FOUR = { "MD", "ME", "NM", "TX" };
	private static final String[] GROUP_FIVE = { "CT", "IA", "MS", "VA", "WA", "WY" };
	private static final String[] GROUP_SIX = { "DC", "MO", "MT", "NC", "ND", "NE", "NH" };
	private static final String[] GROUP_SEVEN = { "DE", "FL", "HI", "IL", "KS", "RI" };
	private static final String[] GROUP_EIGHT = { "ID", "IN", "KY", "LA", "MA", "OR" };
	private static final String[] GROUP_NINE = { "MN", "NJ", "NV", "OK", "PR", "SC", "SD", "VT" };
	private static final String[] GROUP_TEN = { "OH", "TV", "WI", "WV" };
	private static final String[] PROXIES = { "d01.cs.ucr.edu", "d02.cs.ucr.edu", "d03.cs.ucr.edu", "d04.cs.ucr.edu",
			"d05.cs.ucr.edu", "d06.cs.ucr.edu", "d07.cs.ucr.edu", "d08.cs.ucr.edu", "d09.cs.ucr.edu",
			"d10.cs.ucr.edu" };
	private static final int PROXY_PORT = 3128;

	/**
	 * @title main
	 * @param args<String[]>
	 * @return
	 * @desc Main function
	 */
	public static void main(String[] args) throws Exception {
		if (args.length < 3) {
			System.out.println("Error: Not enough arguments passed in.");
			System.out.println("Correct usage: ./crawler <selection> <month> <year>");
			System.exit(1);
		}

		String selection = args[0];
		int month = convertToInt(args[1]);
		int year = convertToInt(args[2]);

		if (month < 1 || month > 12) {
			System.out.println("Error: Invalid month passed in. Value must be between 1-12");
			System.exit(1);
		}

		_connection = getConnection();
		
		File file = new File("G:/Eclipse/eclipse/chromedriver.exe");
		System.setProperty("webdriver.chrome.driver", file.getAbsolutePath());

		if (selection.equals("all")) {
			crawlStates(GROUP_ONE, month, year);
			crawlStates(GROUP_TWO, month, year);
			crawlStates(GROUP_THREE, month, year);
			crawlStates(GROUP_FOUR, month, year);
			crawlStates(GROUP_FIVE, month, year);
			crawlStates(GROUP_SIX, month, year);
			crawlStates(GROUP_SEVEN, month, year);
			crawlStates(GROUP_EIGHT, month, year);
			crawlStates(GROUP_NINE, month, year);
			crawlStates(GROUP_TEN, month, year);
		} else if (selection.equals("1")) {
			crawlStates(GROUP_ONE, month, year);
		} else if (selection.equals("2")) {
			crawlStates(GROUP_TWO, month, year);
		} else if (selection.equals("3")) {
			crawlStates(GROUP_THREE, month, year);
		} else if (selection.equals("4")) {
			crawlStates(GROUP_FOUR, month, year);
		} else if (selection.equals("5")) {
			crawlStates(GROUP_FIVE, month, year);
		} else if (selection.equals("6")) {
			crawlStates(GROUP_SIX, month, year);
		} else if (selection.equals("7")) {
			crawlStates(GROUP_SEVEN, month, year);
		} else if (selection.equals("8")) {
			crawlStates(GROUP_EIGHT, month, year);
		} else if (selection.equals("9")) {
			crawlStates(GROUP_NINE, month, year);
		} else if (selection.equals("10")) {
			crawlStates(GROUP_TEN, month, year);
		} else {
			System.out.println("Invalid argument passed in. Valid arguments: all, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10");
			System.out.println("Application exiting.");
			System.exit(1);
		}
		System.out.println("Crawling completed (100%).");

		_connection.close();
	}

	/**
	 * @title crawlStates
	 * @param states<String[]>,
	 *            month<int>, year<int>
	 * @return
	 * @desc Calls crawlState() on each state<String> in states<String[]> for
	 *       month<int>, year<int>
	 */
	public static void crawlStates(String[] states, int month, int year) throws Exception {
		try {
			for (String state : states) {
				crawlState(state, month, year);
			}
		} catch (Exception e) {
			System.err.println("Error: " + e.getMessage());
			e.printStackTrace();
			System.exit(-1);
		}
	}

	/**
	 * @title crawlState
	 * @param state<String>,
	 *            month<int>, year<int>
	 * @return
	 * @desc Calls crawlZipcodeByMonthYear for each zipcode in state<String> for
	 *       month<int>, year<int>
	 */
	public static void crawlState(String state, int month, int year) throws Exception {
		try {
			System.out.println("Crawling " + state + ", " + month + "/" + year);

			Statement statement = _connection.createStatement(java.sql.ResultSet.TYPE_FORWARD_ONLY,
					java.sql.ResultSet.CONCUR_READ_ONLY);
			statement.setFetchSize(Integer.MIN_VALUE);
			// ResultSet result = statement
			// .executeQuery("SELECT zip FROM cities_extended WHERE
			// state_code='" + state + "' order by zip");
			ResultSet result = statement.executeQuery(
					"SELECT zip_code FROM zipcodes_bystate WHERE state='" + state + "' order by zip_code");

			List<Integer> bucket1 = new ArrayList<Integer>();
			List<Integer> bucket2 = new ArrayList<Integer>();
//			List<Integer> bucket3 = new ArrayList<Integer>();
//			List<Integer> bucket4 = new ArrayList<Integer>();
//			List<Integer> bucket5 = new ArrayList<Integer>();
//			List<Integer> bucket6 = new ArrayList<Integer>();
//			List<Integer> bucket7 = new ArrayList<Integer>();
//			List<Integer> bucket8 = new ArrayList<Integer>();
//			List<Integer> bucket9 = new ArrayList<Integer>();
//			List<Integer> bucket10 = new ArrayList<Integer>();
			int counter = 0;

			while (result.next()) {
				// int zipcode = convertToInt(result.getString("zip"));
				int zipcode = convertToInt(result.getString("zip_code"));

				if (zipcode > 0) {
					if (counter % 2 == 0) {
						bucket1.add(zipcode);
					} else if (counter % 2 == 1) {
						bucket2.add(zipcode);
//					} else if (counter % 10 == 2) {
//						bucket3.add(zipcode);
//					} else if (counter % 10 == 3) {
//						bucket4.add(zipcode);
//					} else if (counter % 10 == 4) {
//						bucket5.add(zipcode);
//					} else if (counter % 10 == 5) {
//						bucket6.add(zipcode);
//					} else if (counter % 10 == 6) {
//						bucket7.add(zipcode);
//					} else if (counter % 10 == 7) {
//						bucket8.add(zipcode);
//					} else if (counter % 10 == 8) {
//						bucket9.add(zipcode);
//					} else if (counter % 10 == 9) {
//						bucket10.add(zipcode);
					}
					++counter;
				}
			}
			result.close();

			if (!bucket1.isEmpty()) {
				Proxy proxy1 = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(PROXIES[0], PROXY_PORT));
				Worker worker1 = new Worker("worker1", state, bucket1, month, year, proxy1);
				worker1.start();
			}
			if (!bucket2.isEmpty()) {
				Proxy proxy2 = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(PROXIES[1], PROXY_PORT));
				Worker worker2 = new Worker("worker2", state, bucket2, month, year, proxy2);
				worker2.start();
			}
//			if (!bucket3.isEmpty()) {
//				Proxy proxy3 = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(PROXIES[2], PROXY_PORT));
//				Worker worker3 = new Worker("worker3", state, bucket3, month, year, proxy3);
//				worker3.start();
//			}
//			if (!bucket4.isEmpty()) {
//				Proxy proxy4 = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(PROXIES[3], PROXY_PORT));
//				Worker worker4 = new Worker("worker4", state, bucket4, month, year, proxy4);
//				worker4.start();
//			}
//			if (!bucket5.isEmpty()) {
//				Proxy proxy5 = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(PROXIES[4], PROXY_PORT));
//				Worker worker5 = new Worker("worker5", state, bucket5, month, year, proxy5);
//				worker5.start();
//			}
//			if (!bucket6.isEmpty()) {
//				Proxy proxy6 = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(PROXIES[5], PROXY_PORT));
//				Worker worker6 = new Worker("worker6", state, bucket6, month, year, proxy6);
//				worker6.start();
//			}
//			if (!bucket7.isEmpty()) {
//				Proxy proxy7 = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(PROXIES[6], PROXY_PORT));
//				Worker worker7 = new Worker("worker7", state, bucket7, month, year, proxy7);
//				worker7.start();
//			}
//			if (!bucket8.isEmpty()) {
//				Proxy proxy8 = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(PROXIES[7], PROXY_PORT));
//				Worker worker8 = new Worker("worker8", state, bucket8, month, year, proxy8);
//				worker8.start();
//			}
//			if (!bucket9.isEmpty()) {
//				Proxy proxy9 = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(PROXIES[8], PROXY_PORT));
//				Worker worker9 = new Worker("worker9", state, bucket9, month, year, proxy9);
//				worker9.start();
//			}
//			if (!bucket10.isEmpty()) {
//				Proxy proxy10 = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(PROXIES[9], PROXY_PORT));
//				Worker worker10 = new Worker("worker10", state, bucket10, month, year, proxy10);
//				worker10.start();
//			}

			System.out.println("Finished crawling " + state + ", " + month + "/" + year);
		} catch (Exception e) {
			System.err.println("Error: " + e.getMessage());
			e.printStackTrace();
			System.exit(-1);
		}
	}

	/**
	 * @title nextPageDoesExist
	 * @param driver<WebDriver>
	 * @return True if "nextPage" element is found in driver, false otherwise
	 * @desc NO LONGER USED
	 */
	// public static boolean nextPageDoesExist(WebDriver driver) {
	// System.out.println("Entered nextPageDoesExist");
	//
	// return driver
	// .findElements(By.cssSelector(
	// "ul[class=buttonList_11hau3k]
	// li[class=buttonContainer_1am0dt-o_O-noRightMargin_10fyztj]
	// a[aria-label=Next]"))
	// .size() > 0;
	// }

	/**
	 * @title getConnectionToZipcodes
	 * @param
	 * @return connection<Connection> to MySQL database where zipcodes are
	 *         stored
	 */
	private static Connection getConnection() throws Exception {
		Class.forName("com.mysql.jdbc.Driver");
		// String urldb = "jdbc:mysql://localhost/business";
		// String user = "jonathan";
		// String password = "password";

		String urldb = "jdbc:mysql://localhost/cs179_project";
		String user = "root";
		String password = "A895784e1!";
		Connection connection = DriverManager.getConnection(urldb, user, password);
		return connection;
	}

	/**
	 * @title getNumericalCharacters
	 * @param value<String>
	 * @return value<String> with numerical characters only
	 */
	public static String getNumericalCharacters(String value) {
		return value.replaceAll("[^0-9]", "");
	}

	/**
	 * @title convertToInt
	 * @param value<String>
	 * @return Converts value<String> to an integer after removing all
	 *         non-numerical characters
	 */
	public static int convertToInt(String value) {
		String filteredValue = getNumericalCharacters(value);

		if (filteredValue.length() > 0) {
			return Integer.parseInt(filteredValue);
		} else {
			return 0;
		}
	}
}
