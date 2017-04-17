package crawler;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Calendar;
import java.util.GregorianCalendar;

import org.apache.commons.io.FileUtils;
import org.jsoup.Jsoup;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.firefox.FirefoxDriver;

public class Crawler {
	private static Connection _connection;
//	private static WebDriver _primaryDriver;
	private static final String[] GROUP_ONE = { "AL", "AK", "AZ", "AR", "CA" };
	private static final String[] GROUP_TWO = { "CO", "CT", "DE", "FL", "GA" };
	private static final String[] GROUP_THREE = { "HI", "ID", "IL", "IN", "IA" };
	private static final String[] GROUP_FOUR = { "KS", "KY", "LA", "ME", "MD" };
	private static final String[] GROUP_FIVE = { "MA", "MI", "MN", "MS", "MO" };
	private static final String[] GROUP_SIX = { "MT", "NE", "NV", "NH", "NJ" };
	private static final String[] GROUP_SEVEN = { "NM", "NY", "NC", "ND", "OH" };
	private static final String[] GROUP_EIGHT = { "OK", "OR", "PA", "RI", "SC" };
	private static final String[] GROUP_NINE = { "SD", "TN", "TX", "UT", "VT" };
	private static final String[] GROUP_TEN = { "VA", "WA", "WV", "WI", "WY", "DC" };

	/**
	 * @title main
	 * @param args<String[]>
	 * @return
	 * @desc Main function
	 */
	public static void main(String[] args) throws Exception {
		if (args.length < 3) {
			System.out.println("Error: Not enough arguments passed in.");
			System.out.println("Format: ./crawler <selection> <month> <year>");
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
//		_primaryDriver = null;

		// File file = new File("G:/Eclipse/eclipse/chromedriver.exe");
		// System.setProperty("webdriver.chrome.driver",
		// file.getAbsolutePath());

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

//		tryClosePrimaryDriver();
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

			while (result.next()) {
				int zipcode = convertToInt(result.getString("zip"));
				// int zipcode = convertToInt(result.getString("zip_code"));

				if (zipcode > 0) {
					crawlZipcodeByMonthYear(state, zipcode, month, year);
				}
			}
			result.close();

			System.out.println("Finished crawling " + state + ", " + month + "/" + year);
		} catch (Exception e) {
			System.err.println("Error: " + e.getMessage());
			e.printStackTrace();
			System.exit(-1);
		}
	}

	/**
	 * @title crawlZipcodeByMonthYear
	 * @param state<String>,
	 *            month<int>, year<int>
	 * @return
	 * @desc Extracts airbnb's average-price/month for zipcode for month<int>,
	 *       year<int> and stores extracted data into database.
	 */
	public static void crawlZipcodeByMonthYear(String state, int zipcode, int month, int year) throws Exception {
		try {
			System.out.println("Crawling " + zipcode + ", " + month + "/" + year);

			Calendar calendar = new GregorianCalendar(year, month, 1);
			int numDays = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
			String checkInDate = year + "-" + month + "-01";
			String checkOutDate = year + "-" + month + "-" + numDays;

			String url = "https://www.airbnb.com/s/" + zipcode + "/homes?checkin=" + checkInDate + "&checkout="
					+ checkOutDate;
			System.out.println(url);

			savePageSourceFromListingUrl(state, zipcode, month, year, url);

			System.out.println("Finished crawling " + zipcode + ", " + month + "/" + year);
		} catch (Exception e) {
			System.err.println("Error: " + e.getMessage());
			e.printStackTrace();
			System.exit(-1);
		}
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

	/**
	 * @title savePageSourceFromListingUrl
	 * @param url<String>
	 * @return
	 * @desc Gets the page source from the url<String> and writes it to a text
	 *       file @ "/airbnb/pagesources/state/zipcode_month_year.txt"
	 */
	public static void savePageSourceFromListingUrl(String state, int zipcode, int month, int year, String url)
			throws Exception {
		System.out.println("Entered savePageSourceFromListingUrl");

		// _primaryDriver = new FirefoxDriver();
		// _primaryDriver = new ChromeDriver();
		// _primaryDriver.get(url);

		String pageSource = Jsoup.connect(url).get().html();
//		Thread.sleep(3000);

		// Get page sources to work offline
		// String pageSource = _primaryDriver.getPageSource();
		String fileName = zipcode + "_" + month + "_" + year + ".txt";
		String directory = "./pagesources/" + state + "/";
		writeStringToFile(directory, fileName, pageSource);

		// _primaryDriver.close();
	}

	/**
	 * @title writeStringToFile
	 * @param fileName<String>,
	 *            text<String>
	 * @return
	 * @desc Creates fileName, if it does not exist, in /pagesources and writes
	 *       text to file.
	 */
	public static void writeStringToFile(String directory, String fileName, String text) throws Exception {
		String fullFileName = directory + fileName;

		File file = new File(fullFileName);
		FileUtils.writeStringToFile(file, text, "UTF-8");
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
		String urldb = "jdbc:mysql://localhost/business";
		String user = "jonathan";
		String password = "password";

		// String urldb = "jdbc:mysql://localhost/cs179_project";
		// String user = "root";
		// String password = "//FIXME: PUT REAL PASS!";
		Connection connection = DriverManager.getConnection(urldb, user, password);
		return connection;
	}

	/**
	 * @title tryClosePrimaryDriver
	 * @param
	 * @return
	 * @desc Ends _primaryDriver<WebDriver>'s session if session is not null
	 * 		 NO LONGER IN USE
	 */
//	public static void tryClosePrimaryDriver() {
//		if (_primaryDriver != null) {
//			_primaryDriver.quit();
//		}
//	}
}
