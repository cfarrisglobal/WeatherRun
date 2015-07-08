By Charles Cody Farris
cfarrisutd@gmail.com

********************************************************************************
Built on Java 1.7

In order to successfully run this java program you'll need to setup a local mysql
database. To create the database see tables_created.sql. Then inside
soap/src/soap/WeatherRequest.java you can set your mysql settings at line 286.

********************************************************************************

The purpose of this program is to populate a database containing weather prediction
data for each US zip code. The weather parameters that we are after are temperature,
rainfall, snowfall, ice accumulation, humidity, wind speed and wind gusts. The weather
prediction data is provided by the National Weather Service's SOAP NDFD web service.

The NDFD web service returns an XML document based on a zip code's latitude and 
longitude. The resulting XML document must be parsed, sorted and matched to create
a full picture of each hour's weather prediction. Then it may be entered into the 
MySQL database.

See NDFD_XML_Example.xml for a sample file returned by the National Weather Service's 
NDFD we service.

********************************************************************************
