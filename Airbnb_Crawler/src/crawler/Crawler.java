package crawler;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.firefox.FirefoxBinary;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxProfile;

public class Crawler {
	private static Connection _connection;
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
		}
		_connection = getConnection();
		_primaryDriver = null;
		_calendar = Calendar.getInstance();
		_month = _calendar.get(Calendar.MONTH);
		_year = _calendar.get(Calendar.YEAR);

		File file = new File("G:/Eclipse/eclipse/chromedriver.exe");
		System.setProperty("webdriver.chrome.driver", file.getAbsolutePath());

		String selection = args[0];
		int month = args[1];
		int year = args[2];
		if (selection.equals("all")) {
			crawlStates(GROUP_ONE);
			crawlStates(GROUP_TWO);
			crawlStates(GROUP_THREE);
			crawlStates(GROUP_FOUR);
			crawlStates(GROUP_FIVE);
			crawlStates(GROUP_SIX);
			crawlStates(GROUP_SEVEN);
			crawlStates(GROUP_EIGHT);
			crawlStates(GROUP_NINE);
			crawlStates(GROUP_TEN);
		} else if (selection.equals("1")) {
			crawlStates(GROUP_ONE);
		} else if (selection.equals("2")) {
			crawlStates(GROUP_TWO);
		} else if (selection.equals("3")) {
			crawlStates(GROUP_THREE);
		} else if (selection.equals("4")) {
			crawlStates(GROUP_FOUR);
		} else if (selection.equals("5")) {
			crawlStates(GROUP_FIVE);
		} else if (selection.equals("6")) {
			crawlStates(GROUP_SIX);
		} else if (selection.equals("7")) {
			crawlStates(GROUP_SEVEN);
		} else if (selection.equals("8")) {
			crawlStates(GROUP_EIGHT);
		} else if (selection.equals("9")) {
			crawlStates(GROUP_NINE);
		} else if (selection.equals("10")) {
			crawlStates(GROUP_TEN);
		} else {
			System.out.println("Invalid argument passed in. Valid arguments: all, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10");
			System.out.println("Application exiting.");
			System.exit(1);
		}
		System.out.println("Crawling completed (100%).");

		tryClosePrimaryDriver();
	}

	/**
	 * @title crawlStates
	 * @param states<String[]>
	 * @return
	 * @desc Calls crawlState() on each state<String> in states<String[]>
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
	
	public static void crawlState(String state, int month, int year) throws Exception {
		try {
			Statement statement = _connection.createStatement(java.sql.ResultSet.TYPE_FORWARD_ONLY, java.sql.ResultSet.CONCUR_READ_ONLY);
			statement.setFetchSize(Integer.MIN_VALUE);
			ResultSet result = statement.executeQuery("SELECT city FROM cities WHERE state='" + state + "' order by city");
			
			while (result.next()) {
				String city = result.getString("city");
				
				crawlCityStateByMonthYear(city, state, month, year);
			}
			
		} catch (Exception e) {
			System.err.println("Error: " + e.getMessage());
			e.printStackTrace();
			System.exit(-1);
		}
	}

	public static void crawlCityStateByMonthYear(String city, String state, int month, int year) throws Exception {
		city.replaceAll(" ", "-");
		Calendar calendar = new GregorianCalendar(year, month, 1);
		int numDays = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
		String checkInDate = year + "-" + month + "-01";
		String checkOutDate = year + "-" + month + "-" + numDays;
		String defaultUrl = "https://www.airbnb.com/s/" + city + "--" + state + "/homes?checkin=" + checkInDate + "&checkout="
				+ checkOutDate;
		System.out.println(defaultUrl);
		boolean nextPageDoesExist = true;
		int pageCount = 0;
		
		while (nextPageDoesExist) {
			String url = defaultUrl + "&section_offset=" + pageCount;
			pageCount++;
			_primaryDriver = new ChromeDriver();
			_primaryDriver.get(url);
			Thread.sleep(3000);
			
			String pageSource = _primaryDriver.getPageSource();
			String fileName = _month + "-" + _year + "-" + city + "-" + state + "-" + pageCount + ".txt";
			String directory = "./airbnb/pagesources/";
			writeStringToFile(directory, fileName, pageSource);

			if (nextPageDoesExist(_primaryDriver)) {
				nextPageDoesExist = true;
			} else {
				nextPageDoesExist = false;
			}
			_primaryDriver.close();
		}
		
		System.out.println("Finished crawling " + city + ", " + state + " for " + month + "/" + year);
	}

	/**
	 * @title nextPageDoesExist
	 * @param driver<WebDriver>
	 * @return True if "nextPage" element is found in driver, false otherwise
	 */
	public static boolean nextPageDoesExist(WebDriver driver) {
		System.out.println("Entered nextPageDoesExist");
		
		return driver.findElements(By.cssSelector("ul[class=buttonList_11hau3k] li[class=buttonContainer_1am0dt-o_O-noRightMargin_10fyztj] a[aria-label=Next]")).size() > 0;
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
	 * @title getConnection
	 * @param
	 * @return connection<Connection> to MySQL database
	 */
	private static Connection getConnection() throws ClassNotFoundException, SQLException {
		Class.forName("com.mysql.jdbc.Driver");
		String urldb = "jdbc:mysql://localhost/databaselabs";
		String user = "root";
		String password = "A895784e1!";
		Connection connection = DriverManager.getConnection(urldb, user, password);
		return connection;
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
