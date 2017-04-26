package crawler;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.jsoup.Jsoup;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;

public class Worker extends Thread {
	private static String _name;
	private static String _state;
	private static List<Integer> _zipcodes;
	private static int _month;
	private static int _year;
	private static Proxy _proxy;
	private static WebDriver _driver; 

	public Worker(String name, String state, List<Integer> zipcodes, int month, int year, Proxy proxy) {
		_name = name;
		_state = state;
		_zipcodes = zipcodes;
		_month = month;
		_year = year;
		_proxy = proxy;
		_driver = null;
	}

	public void run() {
		System.out.println("Running worker " + _name);
		try {
			_driver = new HtmlUnitDriver();
			for (int zipcode : _zipcodes) {
				crawlZipcode(zipcode);
				Thread.sleep(3000);
			}
			tryCloseDriver();
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
	public static void crawlZipcode(int zipcode) throws Exception {
		try {
			System.out.println(_name + " crawling " + zipcode + ", " + _month + "/" + _year);

			Calendar calendar = new GregorianCalendar(_year, _month, 1);
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
	public static void savePageSourceFromListingUrl(int zipcode, String url)
			throws Exception {
		System.out.println("Entered savePageSourceFromListingUrl");
		
//		String pageSource = Jsoup.connect(url).timeout(30000)
//				.userAgent(
//						"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/57.0.2987.133 Safari/537.36")
//				.proxy(_proxy)
//				.get()
//				.html();

		_driver.get(url);
		Thread.sleep(3000);
		String pageSource = _driver.getPageSource();
		
		String fileName = zipcode + "_" + _month + "_" + _year + ".txt";
		String directory = "./pagesources/" + _state + "/";
		writeStringToFile(directory, fileName, pageSource);
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
	 * @title tryCloseDriver
	 * @param
	 * @return
	 * @desc Ends _driver<WebDriver>'s session if session is not null
	 */
	public static void tryCloseDriver() {
		if (_driver != null) {
			_driver.close();
			_driver.quit();
		}
	}
}
