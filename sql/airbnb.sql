DROP TABLE IF EXISTS airbnb;
CREATE TABLE airbnb (
	zipcode int(10) NOT NULL,
	average_price int(10) NOT NULL,
	month int(10) NOT NULL,
	year int(10) NOT NULL,
	url varchar(200) NOT NULL,
	crawl_time timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
	PRIMARY KEY (zipcode, crawl_time, month, year)
);