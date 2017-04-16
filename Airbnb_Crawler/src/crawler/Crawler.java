package crawler;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Comment;
import org.jsoup.nodes.DataNode;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.Elements;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.firefox.FirefoxDriver;

public class Crawler {
	private static Connection _connectionToZipcodes, _connectionToMain;
	private static WebDriver _primaryDriver;
	private static Calendar _calendar;
	private static int _month, _year;
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
			System.out.println("Format: ./crawler.java <selection> <month> <year>");
			System.exit(1);
		}

		String selection = args[0];
		int month = convertToInt(args[1]);
		int year = convertToInt(args[2]);

		if (month < 1 || month > 12) {
			System.out.println("Error: Invalid month passed in. Value must be between 1-12");
			System.exit(1);
		}

		_connectionToMain = getConnectionToMain();
		_connectionToZipcodes = getConnectionToZipcodes();
		_primaryDriver = null;
		_calendar = Calendar.getInstance();
		_month = _calendar.get(Calendar.MONTH);
		_year = _calendar.get(Calendar.YEAR);

//		File file = new File("G:/Eclipse/eclipse/chromedriver.exe");
//		System.setProperty("webdriver.chrome.driver", file.getAbsolutePath());

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

		_connectionToMain.close();
		_connectionToZipcodes.close();

		tryClosePrimaryDriver();
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

			Statement statement = _connectionToZipcodes.createStatement(java.sql.ResultSet.TYPE_FORWARD_ONLY,
					java.sql.ResultSet.CONCUR_READ_ONLY);
			statement.setFetchSize(Integer.MIN_VALUE);
			ResultSet result = statement
					.executeQuery("SELECT zip FROM cities_extended WHERE state_code='" + state + "' order by zip");
			// ResultSet result = statement.executeQuery("SELECT zip_code FROM
			// zipcodes_bystate WHERE state='" + state + "' order by zip_code");

			while (result.next()) {
				int zipcode = convertToInt(result.getString("zip"));
				// int zipcode = convertToInt(result.getString("zip_code"));

				crawlZipcodeByMonthYear(zipcode, month, year);
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
	public static void crawlZipcodeByMonthYear(int zipcode, int month, int year) throws Exception {
		try {
			System.out.println("Crawling " + zipcode + ", " + month + "/" + year);

			Calendar calendar = new GregorianCalendar(year, month, 1);
			int numDays = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
			String checkInDate = year + "-" + month + "-01";
			String checkOutDate = year + "-" + month + "-" + numDays;

			String url = "https://www.airbnb.com/s/" + zipcode + "/homes?checkin=" + checkInDate + "&checkout="
					+ checkOutDate;
			System.out.println(url);

			_primaryDriver = new FirefoxDriver();
//			 _primaryDriver = new ChromeDriver();
			_primaryDriver.get(url);
			Thread.sleep(3000);

			Airbnb airbnb = new Airbnb();
			airbnb.setCrawlTime(getCurrentTimestamp());
			airbnb.setZipcode(zipcode);
			airbnb.setUrl(url);
			airbnb.setMonth(month);
			airbnb.setYear(year);

			String pageSource = _primaryDriver.getPageSource();
			Document document = Jsoup.parse(pageSource);
			airbnb.setAveragePrice(getAveragePriceFromDocumentComments(document));

			airbnb.print();

			saveAirbnbToDatabase(airbnb);

			System.out.println("Finished crawling " + zipcode + ", " + month + "/" + year);
			_primaryDriver.close();
		} catch (Exception e) {
			System.err.println("Error: " + e.getMessage());
			e.printStackTrace();
			System.exit(-1);
		}
	}

	/**
	 * @title getAveragePriceFromDocumentComments
	 * @param document<Node>
	 * @return averagePrice<int> if found in document<Node>, -1 otherwise
	 */
	private static int getAveragePriceFromDocumentComments(Document document) {
		int averagePrice = -1;

		Elements scripts = document.getElementsByTag("script");

		for (int i = 0; i < scripts.size() && averagePrice < 0; ++i) {
			List<DataNode> scriptDataNodes = scripts.get(i).dataNodes();
			for (int j = 0; j < scriptDataNodes.size() && averagePrice < 0; ++j) {
				averagePrice = getAveragePriceFromText(scriptDataNodes.get(j).getWholeData());
			}
		}

		return averagePrice;
	}

	/**
	 * @title getAveragePriceFromText
	 * @param text<String>
	 * @return averagePrice<int> if found in text<String>, -1 otherwise
	 */
	public static int getAveragePriceFromText(String text) {
		Matcher m = Pattern.compile("\"average_price\":[0-9]+[,]").matcher(text);

		int averagePrice = -1;
		while (m.find()) {
			averagePrice = convertToInt(m.group());
			System.out.println("AveragePrice: " + averagePrice);
			if (averagePrice > 0) {
				break;
			}
		}

		return averagePrice;
	}

	/**
	 * @title saveAirbnbToDatabase
	 * @param airbnb<Airbnb>
	 * @throws Exception
	 * @desc Inserts airbnb's data to database
	 */
	public static void saveAirbnbToDatabase(Airbnb airbnb) throws Exception {
		System.out.println("Entered saveAirbnbToDatabase");

		String sqlStatement = "INSERT INTO airbnb(zipcode, average_price, month, year, url, crawl_time) VALUES(?,?,?,?,?,?)";
		PreparedStatement preparedStatement = _connectionToMain.prepareStatement(sqlStatement);
		preparedStatement.setInt(1, airbnb.getZipcode());
		preparedStatement.setInt(2, airbnb.getAveragePrice());
		preparedStatement.setInt(3, airbnb.getMonth());
		preparedStatement.setInt(4, airbnb.getYear());
		preparedStatement.setString(5, airbnb.getUrl());
		preparedStatement.setTimestamp(6, airbnb.getCrawlTime());
		preparedStatement.executeUpdate();
		preparedStatement.close();
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
	 * @title nextPageDoesExist
	 * @param driver<WebDriver>
	 * @return True if "nextPage" element is found in driver, false otherwise
	 */
	public static boolean nextPageDoesExist(WebDriver driver) {
		System.out.println("Entered nextPageDoesExist");

		return driver
				.findElements(By.cssSelector(
						"ul[class=buttonList_11hau3k] li[class=buttonContainer_1am0dt-o_O-noRightMargin_10fyztj] a[aria-label=Next]"))
				.size() > 0;
	}

	/**
	 * @title writeStringToFile
	 * @param fileName<String>,
	 *            text<String>
	 * @return
	 * @desc Creates fileName, if it does not exist, in /pagesources and writes
	 *       text to file.
	 */
	public static void writeStringToFile(String fileName, String text) throws Exception {
		String directory = "./pagesources/other/";

		writeStringToFile(directory, fileName, text);
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
	 * @title getConnectionToMain
	 * @param
	 * @return connection<Connection> to MySQL database where data will be
	 *         stored
	 */
	private static Connection getConnectionToMain() throws ClassNotFoundException, SQLException {
		Class.forName("com.mysql.jdbc.Driver");
		String urldb = "jdbc:mysql://localhost/homeDB";
		String user = "jonathan";
		String password = "password";

		// String urldb = "jdbc:mysql://localhost/databaselabs";
		// String user = "root";
		// String password = "A895784e1!";
		Connection connection = DriverManager.getConnection(urldb, user, password);
		return connection;
	}

	/**
	 * @title getConnectionToZipcodes
	 * @param
	 * @return connection<Connection> to MySQL database where zipcodes are
	 *         stored
	 */
	private static Connection getConnectionToZipcodes() throws Exception {
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
	 * @title getCurrentTimestamp
	 * @param
	 * @return currentTimestamp<Timestamp>
	 */
	private static Timestamp getCurrentTimestamp() {
		System.out.println("Entered getCurrentTimestamp");

		Timestamp currentTimestamp = new java.sql.Timestamp(Calendar.getInstance().getTime().getTime());
		return currentTimestamp;
	}

	/**
	 * @title tryClosePrimaryDriver
	 * @param
	 * @return
	 * @desc Ends _primaryDriver<WebDriver>'s session if session is not null
	 */
	public static void tryClosePrimaryDriver() {
		if (_primaryDriver != null) {
			_primaryDriver.quit();
		}
	}
}
