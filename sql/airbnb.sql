DROP TABLE IF EXISTS airbnb;
CREATE TABLE airbnb (
	zipcode int(5) UNSIGNED ZEROFILL NOT NULL,
	average_price int(10) DEFAULT NULL,
	month int(10) NOT NULL,
	year int(10) NOT NULL,
	url varchar(200) NOT NULL,
	crawl_time timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
	PRIMARY KEY (zipcode, crawl_time, month, year)
);

CREATE TABLE airbnb2 (
	zipcode int(5) UNSIGNED ZEROFILL NOT NULL,
	average_price int(10) DEFAULT NULL,
	month int(10) NOT NULL,
	year int(10) NOT NULL,
	url varchar(200) NOT NULL,
	crawl_time timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
	PRIMARY KEY (zipcode, crawl_time, month, year)
);

INSERT INTO airbnb2 SELECT * FROM airbnb;

DELETE a2
FROM airbnb3 as a1
JOIN airbnb3 as a2
ON a1.zipcode = a2.zipcode AND a1.month = a2.month AND a1.year = a2.year AND a1.crawl_time > a2.crawl_time;

SELECT COUNT(*)
FROM airbnb3 as a1
JOIN airbnb3 as a2
ON a1.zipcode = a2.zipcode AND a1.month = a2.month AND a1.year = a2.year AND a1.crawl_time > a2.crawl_time;