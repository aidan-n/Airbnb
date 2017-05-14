package crawler;

import java.io.File;
import java.net.InetSocketAddress;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.openqa.selenium.Proxy;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;

public class Crawler {
	private static Connection _connection;
	private static final String[] GROUP_ONE = { "AL", "AR", "CA"};
	private static final String[] GROUP_TWO = { "AK", "AZ", "MI", "NY" };
	private static final String[] GROUP_THREE = { "CO", "GA", "PA", "UT" };
	private static final String[] GROUP_FOUR = { "MD", "ME", "NM", "TX" };
	private static final String[] GROUP_FIVE = { "CT", "IA", "MS", "VA", "WA", "WY" };
	private static final String[] GROUP_SIX = { "DC", "MO", "MT", "NC", "ND", "NE", "NH" };
	private static final String[] GROUP_SEVEN = { "DE", "FL", "HI", "IL", "KS", "RI" };
	private static final String[] GROUP_EIGHT = { "ID", "IN", "KY", "LA", "MA", "OR" };
	private static final String[] GROUP_NINE = { "MN", "NJ", "NV", "OK", "PR", "SC", "SD", "VT" };
	private static final String[] GROUP_TEN = { "OH", "TN", "WI", "WV" };
	private static final String[] PROXIES = { "d01.cs.ucr.edu:3128", "d02.cs.ucr.edu:3128", "d03.cs.ucr.edu:3128",
			"d04.cs.ucr.edu:3128", "d05.cs.ucr.edu:3128", "d06.cs.ucr.edu:3128", "d07.cs.ucr.edu:3128",
			"d08.cs.ucr.edu:3128", "d09.cs.ucr.edu:3128", "d10.cs.ucr.edu:3128", "dblab-rack10.cs.ucr.edu:3128",
			"dblab-rack11.cs.ucr.edu:3128", "dblab-rack12.cs.ucr.edu:3128", "dblab-rack13.cs.ucr.edu:3128",
			"dblab-rack14.cs.ucr.edu:3128", "dblab-rack15.cs.ucr.edu:3128"};

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
		} else if (selection.length() == 2) {
			crawlState(selection, month, year);
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
			ResultSet result = statement
					.executeQuery("SELECT zip FROM cities_extended WHERE state_code='" + state + "' order by zip");
			// ResultSet result = statement.executeQuery(
			// "SELECT zip_code FROM zipcodes_bystate WHERE state='" + state +
			// "' order by zip_code");

			ArrayList<String> bucket1 = new ArrayList<String>();
			ArrayList<String> bucket2 = new ArrayList<String>();
			ArrayList<String> bucket3 = new ArrayList<String>();
			ArrayList<String> bucket4 = new ArrayList<String>();
			ArrayList<String> bucket5 = new ArrayList<String>();
			ArrayList<String> bucket6 = new ArrayList<String>();
			ArrayList<String> bucket7 = new ArrayList<String>();
			ArrayList<String> bucket8 = new ArrayList<String>();
			ArrayList<String> bucket9 = new ArrayList<String>();
			ArrayList<String> bucket10 = new ArrayList<String>();
//			List<String> bucket11 = new ArrayList<String>();
//			List<String> bucket12 = new ArrayList<String>();
//			List<String> bucket13 = new ArrayList<String>();
//			List<String> bucket14 = new ArrayList<String>();
//			List<String> bucket15 = new ArrayList<String>();
//			List<String> bucket16 = new ArrayList<String>();
			int counter = 0;

			while (result.next()) {
				String zipcode = result.getString("zip");
				// int zipcode = convertToInt(result.getString("zip_code"));

				if (counter % 10 == 0) {
					bucket1.add(zipcode);
				} else if (counter % 10 == 1) {
					bucket2.add(zipcode);
				} else if (counter % 10 == 2) {
					bucket3.add(zipcode);
				} else if (counter % 10 == 3) {
					bucket4.add(zipcode);
				} else if (counter % 10 == 4) {
					bucket5.add(zipcode);
				} else if (counter % 10 == 5) {
					bucket6.add(zipcode);
				} else if (counter % 10 == 6) {
					bucket7.add(zipcode);
				} else if (counter % 10 == 7) {
					bucket8.add(zipcode);
				} else if (counter % 10 == 8) {
					bucket9.add(zipcode);
				} else if (counter % 10 == 9) {
					bucket10.add(zipcode);
//				} else if (counter % 16 == 10) {
//					bucket11.add(zipcode);
//				} else if (counter % 16 == 11) {
//					bucket12.add(zipcode);
//				} else if (counter % 16 == 12) {
//					bucket13.add(zipcode);
//				} else if (counter % 16 == 13) {
//					bucket14.add(zipcode);
//				} else if (counter % 16 == 14) {
//					bucket15.add(zipcode);
//				} else if (counter % 16 == 15) {
//					bucket16.add(zipcode);
				}
				++counter;
			}
			result.close();

			Thread thread1 = null;
			Thread thread2 = null;
			Thread thread3 = null;
			Thread thread4 = null;
			Thread thread5 = null;
			Thread thread6 = null;
			Thread thread7 = null;
			Thread thread8 = null;
			Thread thread9 = null;
			Thread thread10 = null;
//			Thread thread11 = null;
//			Thread thread12 = null;
//			Thread thread13 = null;
//			Thread thread14 = null;
//			Thread thread15 = null;
//			Thread thread16 = null;

			HtmlUnitDriver driver1 = new CustomHtmlUnitDriver();
			HtmlUnitDriver driver2 = new CustomHtmlUnitDriver();
			HtmlUnitDriver driver3 = new CustomHtmlUnitDriver();
			HtmlUnitDriver driver4 = new CustomHtmlUnitDriver();
			HtmlUnitDriver driver5 = new CustomHtmlUnitDriver();
			HtmlUnitDriver driver6 = new CustomHtmlUnitDriver();
			HtmlUnitDriver driver7 = new CustomHtmlUnitDriver();
			HtmlUnitDriver driver8 = new CustomHtmlUnitDriver();
			HtmlUnitDriver driver9 = new CustomHtmlUnitDriver();
			HtmlUnitDriver driver10 = new CustomHtmlUnitDriver();
//			HtmlUnitDriver driver11 = new CustomHtmlUnitDriver();
//			HtmlUnitDriver driver12 = new CustomHtmlUnitDriver();
//			HtmlUnitDriver driver13 = new CustomHtmlUnitDriver();
//			HtmlUnitDriver driver14 = new CustomHtmlUnitDriver();
//			HtmlUnitDriver driver15 = new CustomHtmlUnitDriver();
//			HtmlUnitDriver driver16 = new CustomHtmlUnitDriver();

			if (!bucket1.isEmpty()) {
				System.out.println("bucket1 size: " + bucket1.size());
				String proxyAddress = PROXIES[0];
				Proxy proxy1 = new Proxy();
				proxy1.setHttpProxy(proxyAddress).setFtpProxy(proxyAddress).setSslProxy(proxyAddress);
				driver1.setProxySettings(proxy1);
				driver1.manage().timeouts().implicitlyWait(30, TimeUnit.SECONDS);

				Worker worker1 = new Worker("worker1", state, bucket1, month, year, driver1, proxyAddress);
				thread1 = new Thread(worker1);
				thread1.start();
			}
			if (!bucket2.isEmpty()) {
				System.out.println("bucket2 size: " + bucket2.size());
				String proxyAddress = PROXIES[1];
				Proxy proxy2 = new Proxy();
				proxy2.setHttpProxy(proxyAddress).setFtpProxy(proxyAddress).setSslProxy(proxyAddress);
				driver2.setProxySettings(proxy2);
				driver2.manage().timeouts().implicitlyWait(30, TimeUnit.SECONDS);

				Worker worker2 = new Worker("worker2", state, bucket2, month, year, driver2, proxyAddress);
				thread2 = new Thread(worker2);
				thread2.start();
			}
			if (!bucket3.isEmpty()) {
				System.out.println("bucket3 size: " + bucket3.size());
				String proxyAddress = PROXIES[2];
				Proxy proxy3 = new Proxy();
				proxy3.setHttpProxy(proxyAddress).setFtpProxy(proxyAddress).setSslProxy(proxyAddress);
				driver3.setProxySettings(proxy3);
				driver3.manage().timeouts().implicitlyWait(30, TimeUnit.SECONDS);

				Worker worker3 = new Worker("worker3", state, bucket3, month, year, driver3, proxyAddress);
				thread3 = new Thread(worker3);
				thread3.start();
			}
			if (!bucket4.isEmpty()) {
				System.out.println("bucket4 size: " + bucket4.size());
				String proxyAddress = PROXIES[3];
				Proxy proxy4 = new Proxy();
				proxy4.setHttpProxy(proxyAddress).setFtpProxy(proxyAddress).setSslProxy(proxyAddress);
				driver4.setProxySettings(proxy4);
				driver4.manage().timeouts().implicitlyWait(30, TimeUnit.SECONDS);

				Worker worker4 = new Worker("worker4", state, bucket4, month, year, driver4, proxyAddress);
				thread4 = new Thread(worker4);
				thread4.start();
			}
			if (!bucket5.isEmpty()) {
				System.out.println("bucket5 size: " + bucket5.size());
				String proxyAddress = PROXIES[4];
				Proxy proxy5 = new Proxy();
				proxy5.setHttpProxy(proxyAddress).setFtpProxy(proxyAddress).setSslProxy(proxyAddress);
				driver5.setProxySettings(proxy5);
				driver5.manage().timeouts().implicitlyWait(30, TimeUnit.SECONDS);

				Worker worker5 = new Worker("worker5", state, bucket5, month, year, driver5, proxyAddress);
				thread5 = new Thread(worker5);
				thread5.start();
			}
			if (!bucket6.isEmpty()) {
				System.out.println("bucket6 size: " + bucket6.size());
				String proxyAddress = PROXIES[5];
				Proxy proxy6 = new Proxy();
				proxy6.setHttpProxy(proxyAddress).setFtpProxy(proxyAddress).setSslProxy(proxyAddress);
				driver6.setProxySettings(proxy6);
				driver6.manage().timeouts().implicitlyWait(30, TimeUnit.SECONDS);

				Worker worker6 = new Worker("worker6", state, bucket6, month, year, driver6, proxyAddress);
				thread6 = new Thread(worker6);
				thread6.start();
			}
			if (!bucket7.isEmpty()) {
				System.out.println("bucket7 size: " + bucket7.size());
				String proxyAddress = PROXIES[6];
				Proxy proxy7 = new Proxy();
				proxy7.setHttpProxy(proxyAddress).setFtpProxy(proxyAddress).setSslProxy(proxyAddress);
				driver7.setProxySettings(proxy7);
				driver7.manage().timeouts().implicitlyWait(30, TimeUnit.SECONDS);

				Worker worker7 = new Worker("worker7", state, bucket7, month, year, driver7, proxyAddress);
				thread7 = new Thread(worker7);
				thread7.start();
			}
			if (!bucket8.isEmpty()) {
				System.out.println("bucket8 size: " + bucket8.size());
				String proxyAddress = PROXIES[7];
				Proxy proxy8 = new Proxy();
				proxy8.setHttpProxy(proxyAddress).setFtpProxy(proxyAddress).setSslProxy(proxyAddress);
				driver8.setProxySettings(proxy8);
				driver8.manage().timeouts().implicitlyWait(30, TimeUnit.SECONDS);

				Worker worker8 = new Worker("worker8", state, bucket8, month, year, driver8, proxyAddress);
				thread8 = new Thread(worker8);
				thread8.start();
			}
			if (!bucket9.isEmpty()) {
				System.out.println("bucket9 size: " + bucket9.size());
				String proxyAddress = PROXIES[8];
				Proxy proxy9 = new Proxy();
				proxy9.setHttpProxy(proxyAddress).setFtpProxy(proxyAddress).setSslProxy(proxyAddress);
				driver9.setProxySettings(proxy9);
				driver9.manage().timeouts().implicitlyWait(30, TimeUnit.SECONDS);

				Worker worker9 = new Worker("worker9", state, bucket9, month, year, driver9, proxyAddress);
				thread9 = new Thread(worker9);
				thread9.start();
			}
			if (!bucket10.isEmpty()) {
				System.out.println("bucket10 size: " + bucket10.size());
				String proxyAddress = PROXIES[9];
				Proxy proxy10 = new Proxy();
				proxy10.setHttpProxy(proxyAddress).setFtpProxy(proxyAddress).setSslProxy(proxyAddress);
				driver10.setProxySettings(proxy10);
				driver10.manage().timeouts().implicitlyWait(30, TimeUnit.SECONDS);

				Worker worker10 = new Worker("worker10", state, bucket10, month, year, driver10, proxyAddress);
				thread10 = new Thread(worker10);
				thread10.start();
			}
//			if (!bucket11.isEmpty()) {
//				System.out.println("bucket11 size: " + bucket11.size());
//				String proxyAddress = PROXIES[10];
//				Proxy proxy11 = new Proxy();
//				proxy11.setHttpProxy(proxyAddress).setFtpProxy(proxyAddress).setSslProxy(proxyAddress);
//				driver11.setProxySettings(proxy11);
//				driver11.manage().timeouts().implicitlyWait(30, TimeUnit.SECONDS);
//
//				Worker worker11 = new Worker("worker11", state, bucket11, month, year, driver11, proxyAddress);
//				thread11 = new Thread(worker11);
//				thread11.start();
//			}
//			if (!bucket12.isEmpty()) {
//				System.out.println("bucket12 size: " + bucket12.size());
//				String proxyAddress = PROXIES[11];
//				Proxy proxy12 = new Proxy();
//				proxy12.setHttpProxy(proxyAddress).setFtpProxy(proxyAddress).setSslProxy(proxyAddress);
//				driver12.setProxySettings(proxy12);
//				driver12.manage().timeouts().implicitlyWait(30, TimeUnit.SECONDS);
//
//				Worker worker12 = new Worker("worker12", state, bucket12, month, year, driver12, proxyAddress);
//				thread12 = new Thread(worker12);
//				thread12.start();
//			}
//			if (!bucket13.isEmpty()) {
//				System.out.println("bucket13 size: " + bucket13.size());
//				String proxyAddress = PROXIES[12];
//				Proxy proxy13 = new Proxy();
//				proxy13.setHttpProxy(proxyAddress).setFtpProxy(proxyAddress).setSslProxy(proxyAddress);
//				driver13.setProxySettings(proxy13);
//				driver13.manage().timeouts().implicitlyWait(30, TimeUnit.SECONDS);
//
//				Worker worker13 = new Worker("worker13", state, bucket13, month, year, driver13, proxyAddress);
//				thread13 = new Thread(worker13);
//				thread13.start();
//			}
//			if (!bucket14.isEmpty()) {
//				System.out.println("bucket14 size: " + bucket14.size());
//				String proxyAddress = PROXIES[13];
//				Proxy proxy14 = new Proxy();
//				proxy14.setHttpProxy(proxyAddress).setFtpProxy(proxyAddress).setSslProxy(proxyAddress);
//				driver14.setProxySettings(proxy14);
//				driver14.manage().timeouts().implicitlyWait(30, TimeUnit.SECONDS);
//
//				Worker worker14 = new Worker("worker14", state, bucket14, month, year, driver14, proxyAddress);
//				thread14 = new Thread(worker14);
//				thread14.start();
//			}
//			if (!bucket15.isEmpty()) {
//				System.out.println("bucket15 size: " + bucket15.size());
//				String proxyAddress = PROXIES[14];
//				Proxy proxy15 = new Proxy();
//				proxy15.setHttpProxy(proxyAddress).setFtpProxy(proxyAddress).setSslProxy(proxyAddress);
//				driver15.setProxySettings(proxy15);
//				driver15.manage().timeouts().implicitlyWait(30, TimeUnit.SECONDS);
//
//				Worker worker15 = new Worker("worker15", state, bucket15, month, year, driver15, proxyAddress);
//				thread15 = new Thread(worker15);
//				thread15.start();
//			}
//			if (!bucket16.isEmpty()) {
//				System.out.println("bucket16 size: " + bucket16.size());
//				String proxyAddress = PROXIES[15];
//				Proxy proxy16 = new Proxy();
//				proxy16.setHttpProxy(proxyAddress).setFtpProxy(proxyAddress).setSslProxy(proxyAddress);
//				driver16.setProxySettings(proxy16);
//				driver16.manage().timeouts().implicitlyWait(30, TimeUnit.SECONDS);
//
//				Worker worker16 = new Worker("worker16", state, bucket16, month, year, driver16, proxyAddress);
//				thread16 = new Thread(worker16);
//				thread16.start();
//			}

			boolean thread1IsAlive = true;
			boolean thread2IsAlive = true;
			boolean thread3IsAlive = true;
			boolean thread4IsAlive = true;
			boolean thread5IsAlive = true;
			boolean thread6IsAlive = true;
			boolean thread7IsAlive = true;
			boolean thread8IsAlive = true;
			boolean thread9IsAlive = true;
			boolean thread10IsAlive = true;
//			boolean thread11IsAlive = true;
//			boolean thread12IsAlive = true;
//			boolean thread13IsAlive = true;
//			boolean thread14IsAlive = true;
//			boolean thread15IsAlive = true;
//			boolean thread16IsAlive = true;

			do {
				if (thread1IsAlive && !thread1.isAlive()) {
					thread1IsAlive = false;
					System.out.println("Thread 1 is dead.");
				}
				if (thread2IsAlive && !thread2.isAlive()) {
					thread2IsAlive = false;
					System.out.println("Thread 2 is dead.");
				}
				if (thread3IsAlive && !thread3.isAlive()) {
					thread3IsAlive = false;
					System.out.println("Thread 3 is dead.");
				}
				if (thread4IsAlive && !thread4.isAlive()) {
					thread4IsAlive = false;
					System.out.println("Thread 4 is dead.");
				}
				if (thread5IsAlive && !thread5.isAlive()) {
					thread5IsAlive = false;
					System.out.println("Thread 5 is dead.");
				}
				if (thread6IsAlive && !thread6.isAlive()) {
					thread6IsAlive = false;
					System.out.println("Thread 6 is dead.");
				}
				if (thread7IsAlive && !thread7.isAlive()) {
					thread7IsAlive = false;
					System.out.println("Thread 7 is dead.");
				}
				if (thread8IsAlive && !thread8.isAlive()) {
					thread8IsAlive = false;
					System.out.println("Thread 8 is dead.");
				}
				if (thread9IsAlive && !thread9.isAlive()) {
					thread9IsAlive = false;
					System.out.println("Thread 9 is dead.");
				}
				if (thread10IsAlive && !thread10.isAlive()) {
					thread10IsAlive = false;
					System.out.println("Thread 10 is dead.");
				}
//				if (thread11IsAlive && !thread11.isAlive()) {
//					thread11IsAlive = false;
//					System.out.println("Thread 11 is dead.");
//				}
//				if (thread12IsAlive && !thread12.isAlive()) {
//					thread12IsAlive = false;
//					System.out.println("Thread 12 is dead.");
//				}
//				if (thread13IsAlive && !thread13.isAlive()) {
//					thread13IsAlive = false;
//					System.out.println("Thread 13 is dead.");
//				}
//				if (thread14IsAlive && !thread14.isAlive()) {
//					thread14IsAlive = false;
//					System.out.println("Thread 14 is dead.");
//				}
//				if (thread15IsAlive && !thread15.isAlive()) {
//					thread15IsAlive = false;
//					System.out.println("Thread 15 is dead.");
//				}
//				if (thread16IsAlive && !thread16.isAlive()) {
//					thread16IsAlive = false;
//					System.out.println("Thread 16 is dead.");
//				}
			} while (thread1IsAlive || thread2IsAlive || thread3IsAlive || thread4IsAlive || thread5IsAlive
					|| thread6IsAlive || thread7IsAlive || thread8IsAlive || thread9IsAlive || thread10IsAlive);
//					|| thread11IsAlive || thread12IsAlive || thread13IsAlive || thread14IsAlive || thread15IsAlive
//					|| thread16IsAlive);

			if (!thread1IsAlive) {
				tryExitDriver(driver1);
			}
			if (!thread2IsAlive) {
				tryExitDriver(driver2);
			}
			if (!thread3IsAlive) {
				tryExitDriver(driver3);
			}
			if (!thread4IsAlive) {
				tryExitDriver(driver4);
			}
			if (!thread5IsAlive) {
				tryExitDriver(driver5);
			}
			if (!thread6IsAlive) {
				tryExitDriver(driver6);
			}
			if (!thread7IsAlive) {
				tryExitDriver(driver7);
			}
			if (!thread8IsAlive) {
				tryExitDriver(driver8);
			}
			if (!thread9IsAlive) {
				tryExitDriver(driver9);
			}
			if (!thread10IsAlive) {
				tryExitDriver(driver10);
			}
//			if (!thread11IsAlive) {
//				tryExitDriver(driver11);
//			}
//			if (!thread12IsAlive) {
//				tryExitDriver(driver12);
//			}
//			if (!thread13IsAlive) {
//				tryExitDriver(driver13);
//			}
//			if (!thread14IsAlive) {
//				tryExitDriver(driver14);
//			}
//			if (!thread15IsAlive) {
//				tryExitDriver(driver15);
//			}
//			if (!thread16IsAlive) {
//				tryExitDriver(driver16);
//			}

			System.out.println("Finished crawling " + state + ", " + month + "/" + year);
		} catch (Exception e) {
			System.err.println("Error: " + e.getMessage());
			e.printStackTrace();
			System.exit(-1);
		}
	}

	/**
	 * @title getConnectionToZipcodes
	 * @param
	 * @return connection<Connection> to MySQL database where zipcodes are
	 *         stored
	 */
	private static Connection getConnection() throws Exception {
		Class.forName("com.mysql.jdbc.Driver");
		String urldb = "jdbc:mysql://localhost/business";
		String user = "jonathan";
		String password = "password";

		// String urldb = "jdbc:mysql://localhost/cs179_project";
		// String user = "root";
		// String password = "A895784e1!";
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

	public static void tryExitDriver(HtmlUnitDriver driver) {
		if (driver != null) {
			driver.close();
		}
	}
}
