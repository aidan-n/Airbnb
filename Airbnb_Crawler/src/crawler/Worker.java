package crawler;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.DataNode;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;

public class Worker implements Runnable {
	Connection _connection;
	String _name;
	String _state;
	ArrayList<String> _zipcodes;
	int _month;
	int _year;
	String _proxyAddress;
	HtmlUnitDriver _driver;

	public Worker(String name, String state, ArrayList<String> zipcodes, int month, int year, HtmlUnitDriver driver,
			String proxyAddress) {
		_name = name;
		_state = state;
		_zipcodes = zipcodes;
		_month = month;
		_year = year;
		_driver = driver;
		_proxyAddress = proxyAddress;
	}

	public void run() {
		System.out.println("Running worker " + _name);
		try {
			System.out.println(_name + " using proxy address: " + _proxyAddress);
			_connection = getConnection();
			int count = 0;
			for (count = 0; count < _zipcodes.size(); ++count) {
				String zipcode = _zipcodes.get(count);
				crawlZipcode(zipcode);
				System.out.println(_zipcodes.size() - count - 1 + " zipcodes remaining.");
			}
			_connection.close();
		} catch (InterruptedException e) {
			System.out.println("Worker " + _name + " interrupted.");
			e.printStackTrace();
			System.exit(-1);
		} catch (Exception e) {
			System.err.println("Error: " + e.getMessage());
			e.printStackTrace();
			System.exit(-1);
		}
		System.out.println("Worker " + _name + " exiting.");
	}

	/**
	 * @title crawlZipcodeByMonthYear
	 * @param state<String>,
	 *            month<int>, year<int>
	 * @return
	 * @desc Extracts airbnb's average-price/month for zipcode for month<int>,
	 *       year<int> and stores extracted data into database.
	 */
	public void crawlZipcode(String zipcode) throws Exception {
		try {
			System.out.println(_name + " crawling " + zipcode + ", " + _month + "/" + _year);

			Calendar calendar = new GregorianCalendar(_year, _month - 1, 1);
			int numDays = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
			String checkInDate = _year + "-" + _month + "-01";
			String checkOutDate = _year + "-" + _month + "-" + numDays;

			String url = "https://www.airbnb.com/s/" + zipcode + "/homes?checkin=" + checkInDate + "&checkout="
					+ checkOutDate;
			System.out.println(url);

			savePageSourceFromListingUrl(zipcode, url);

			System.out.println("Finished crawling " + zipcode + ", " + _month + "/" + _year);
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
	public String getNumericalCharacters(String value) {
		return value.replaceAll("[^0-9]", "");
	}

	/**
	 * @title convertToInt
	 * @param value<String>
	 * @return Converts value<String> to an integer after removing all
	 *         non-numerical characters
	 */
	public int convertToInt(String value) {
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
	public void savePageSourceFromListingUrl(String zipcode, String url) throws Exception {
		System.out.println("Entered savePageSourceFromListingUrl");

		_driver.get(url);
		Thread.sleep(3500);
		String pageSource = _driver.getPageSource();

		Calendar c = Calendar.getInstance();
		int year = c.get(Calendar.YEAR);
		int month = c.get(Calendar.MONTH) + 1;
		String yearMonthDirectory = year + "_" + String.format("%02d", month);

		String fileName = zipcode + "_" + _month + "_" + _year + ".txt";
		String directory = "./pagesources/" + yearMonthDirectory + "/" + _state + "/";
		writeStringToFile(directory, fileName, pageSource);
		parseAndSaveDataFromPageSource(zipcode, url, pageSource);
	}

	/**
	 * @title writeStringToFile
	 * @param fileName<String>,
	 *            text<String>
	 * @return
	 * @desc Creates fileName, if it does not exist, in /pagesources and writes
	 *       text to file.
	 */
	public void writeStringToFile(String directory, String fileName, String text) throws Exception {
		String fullFileName = directory + fileName;

		File file = new File(fullFileName);
		FileUtils.writeStringToFile(file, text, "UTF-8");
	}

	/**
	 * @title parseAndSaveDataFromFile
	 * @param file<File>
	 * @return
	 * @desc Parses file<File> to get airbnb data and saves data to database
	 */
	public void parseAndSaveDataFromPageSource(String zipcode, String url, String pageSource) throws Exception {
		Document document = Jsoup.parse(pageSource);

		Airbnb airbnb = new Airbnb();
		airbnb.setCrawlTime(getCurrentTimestamp());
		airbnb.setZipcode(convertToInt(zipcode));
		airbnb.setUrl(url);
		airbnb.setMonth(_month);
		airbnb.setYear(_year);

		airbnb.setAveragePrice(getAveragePriceFromDocumentComments(document));
		airbnb.setIsMonthlyPriceType(true); // FIXME: Set to true because function foundMonthlyPriceTypeFromDocumentComments not working correctly
		
		airbnb.print();

		saveAirbnbToDatabase(airbnb);
		System.out.println("Finished parsing " + zipcode);
	}
	
	/**
	 * @title foundMonthlyPriceTypeFromDocumentComments
	 * @param document<Node>
	 * @return true if "price_type":"monthly" found in document<Node>, false otherwise
	 */
	private boolean foundMonthlyPriceTypeFromDocumentComments(Document document) {
		boolean priceType = false;
		Elements scripts = document.getElementsByTag("script");

		for (int i = 0; i < scripts.size() && !priceType; ++i) {
			List<DataNode> scriptDataNodes = scripts.get(i).dataNodes();
			for (int j = 0; j < scriptDataNodes.size() && !priceType; ++j) {
				priceType = foundPriceTypeMonthlyFromText(scriptDataNodes.get(j).getWholeData());
			}
		}
		
		return priceType;
	}

	/**
	 * @title getAveragePriceFromDocumentComments
	 * @param document<Node>
	 * @return averagePrice<int> if found in document<Node>, -1 otherwise
	 */
	private int getAveragePriceFromDocumentComments(Document document) {
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
	 * @title foundPriceTypeMonthlyFromText
	 * @param text<String>
	 * @return true if "price_type":"monthly" found in text, false otherwise
	 */
	public boolean foundPriceTypeMonthlyFromText(String text) {
		String searchText = "\"price_type\":\"monthly\"";
		
		return text.contains(searchText);
	}

	/**
	 * @title getAveragePriceFromText
	 * @param text<String>
	 * @return averagePrice<int> if found in text<String>, -1 otherwise
	 */
	public int getAveragePriceFromText(String text) {
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
	public void saveAirbnbToDatabase(Airbnb airbnb) throws Exception {
		System.out.println("Entered saveAirbnbToDatabase");

		String sqlStatement = "INSERT INTO airbnb(zipcode, average_price, month, year, url, crawl_time) VALUES(?,?,?,?,?,?)";
		PreparedStatement preparedStatement = _connection.prepareStatement(sqlStatement);
		// 1
		preparedStatement.setInt(1, airbnb.getZipcode());
		// 2
		if ((airbnb.getAveragePrice() <= 0) || !airbnb.isMonthlyPriceType()) {
			preparedStatement.setNull(2, java.sql.Types.INTEGER);
		} else {
			preparedStatement.setInt(2, airbnb.getAveragePrice());
		}
		// 3
		preparedStatement.setInt(3, airbnb.getMonth());
		// 4
		preparedStatement.setInt(4, airbnb.getYear());
		// 5
		preparedStatement.setString(5, airbnb.getUrl());
		// 6
		preparedStatement.setTimestamp(6, airbnb.getCrawlTime());
		preparedStatement.executeUpdate();
		preparedStatement.close();
	}

	/**
	 * @title getConnection
	 * @param
	 * @return connection<Connection> to MySQL database where data will be
	 *         stored
	 */
	private Connection getConnection() throws ClassNotFoundException, SQLException {
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
	 * @title getCurrentTimestamp
	 * @param
	 * @return currentTimestamp<Timestamp>
	 */
	private static Timestamp getCurrentTimestamp() {
		System.out.println("Entered getCurrentTimestamp");

		Timestamp currentTimestamp = new java.sql.Timestamp(Calendar.getInstance().getTime().getTime());
		return currentTimestamp;
	}
}