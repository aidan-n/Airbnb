package scraper;

import java.sql.Timestamp;

public class Airbnb {
	private int _zipcode;
	private int _averagePrice;
	private int _month;
	private int _year;
	private String _url;
	private String _priceType;
	private Timestamp _crawlTime;
	
	public Airbnb() {
		_zipcode = -1;
		_averagePrice = -1;
		_month = -1;
		_year = -1;
		_url = "";
		_priceType = "";
		_crawlTime = null;
	}
	
	public int getZipcode() {
		return _zipcode;
	}
	
	public void setZipcode(int value) {
			_zipcode = value;
	}
	
	public int getAveragePrice() {
		return _averagePrice;
	}
	
	public void setAveragePrice(int value) {
		_averagePrice = value;
	}
	
	public int getMonth() {
		return _month;
	}
	
	public void setMonth(int value) {
		_month = value;
	}
	
	public int getYear() {
		return _year;
	}
	
	public void setYear(int value) {
		year = value;
	}
	public String getUrl() {
		return _url;
	}

	public void setUrl(String value) {
		_url = value;
	}
	
	public Timestamp getCrawlTime() {
		return _crawlTime;
	}
	
	public void setCrawlTime(Timestamp value) {
		_crawlTime = value;
	}
	
	public String getPriceType() {
		return _priceType;
	}
	
	public void setPriceType(String value) {
		_priceType = value;
	}
	
	public void print() {
		System.out.println("Zipcode: " + getZipcode());
		System.out.println("Average Price: " + getAveragePrice());
		System.out.println("Month: " + getMonth());
		System.out.println("Year: " + getYear());
		System.out.println("Url: " + getUrl());
		System.out.println("CrawlTime: " + getCrawlTime());
		System.out.println("Price Type: " + getPriceType());
	}
}
