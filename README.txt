By Charles Cody Farris
cfarrisutd@gmail.com

********************************************************************************
Built on Java 1.7

This is the main class for the NDFD data request and insert into the local
database. Overall the purpose of this program is to populate a database 
containing weather prediction data for each US zip code. The weather parameters 
that we are after are temperature, rainfall, snowfall, ice accumulation, 
humidity, wind speed and wind gusts.

********************************************************************************

In order to successfully run this java program you'll need to setup a local mysql
database. To create the database see tables_created.sql. Then inside
soap/src/soap/WeatherRequest.java you can set your mysql settings at line 286.