CREATE TABLE WeatherPrediction(
	zipcode INT NOT NULL,
	time_Applicable VARCHAR(30) NOT NULL,
	last_Updated VARCHAR(30) NOT NULL,
	temp INT,
	liquid_Precip DOUBLE,
	wind_Speed INT,
	ice_Accum DOUBLE,
	snow_Amount DOUBLE,
	gust_Speeds INT,
	humidity INT,
	PRIMARY KEY (zipcode, time_Applicable)
);





CREATE TABLE latLon(
	zipcode INT NOT NULL,
	lattitude DOUBLE NOT NULL,
	longitude DOUBLE NOT NULL,
	PRIMARY KEY ( zipcode )
);