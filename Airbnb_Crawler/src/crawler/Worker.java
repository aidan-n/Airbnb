package crawler;

import java.io.File;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.openqa.selenium.Proxy;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.DesiredCapabilities;

public class Worker implements Runnable {
	String _name;
	String _state;
	List<Integer> _zipcodes;
	int _month;
	int _year;
	String _proxyAddress;
	WebDriver _driver;

	public Worker(String name, String state, List<Integer> zipcodes, int month, int year, String proxyAddress) {
		_name = name;
		_state = state;
		_zipcodes = zipcodes;
		_month = month;
		_year = year;
		_proxyAddress = proxyAddress;
	}

	public void run() {
		System.out.println("Running worker " + _name);
		try {
			System.out.println(_name + " using proxy address: " + _proxyAddress);
			Proxy proxy = new Proxy();
			proxy.setHttpProxy(_proxyAddress).setFtpProxy(_proxyAddress).setSslProxy(_proxyAddress);
			DesiredCapabilities capabilities = new DesiredCapabilities();
			capabilities.setCapability(CapabilityType.PROXY, proxy);
			_driver = new HtmlUnitDriver(capabilities);

			System.out.println(_name + " will crawl " + _zipcodes.size() + " zipcodes.");
			int count = 0;
			for (count = 0; count < _zipcodes.size(); ++count) {
				int zipcode = _zipcodes.get(count);
				crawlZipcode(zipcode);
			}
			if (count >= _zipcodes.size()) {
				_driver.close();
				_driver.quit();
			}
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
	public void crawlZipcode(int zipcode) throws Exception {
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
	public void savePageSourceFromListingUrl(int zipcode, String url)
			throws Exception {
		System.out.println("Entered savePageSourceFromListingUrl");
		
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
	public void writeStringToFile(String directory, String fileName, String text) throws Exception {
		String fullFileName = directory + fileName;

		File file = new File(fullFileName);
		FileUtils.writeStringToFile(file, text, "UTF-8");
	}
}