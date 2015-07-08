package soap;

import java.sql.Timestamp;
import java.util.Calendar;

class HourPrediction
{
	private int zipcode;
	private String timeApplicable;
	private Timestamp lastUpdated;
	private int temperature;
	private double liquidPrecip;
	private int windSpeed;
	private double iceAccum;
	private double snowAmount;
	private int gustSpeeds;
	private int humidity;
	

	public HourPrediction(String zip, String timeApp) {
		zipcode = Integer.parseInt(zip);
		timeApplicable = timeApp;
		updateTime();
		System.out.println("Created hourPrediciton with zip: " + zip + " and timeStamp " + timeApp);
	}
	
	private void updateTime() {
		Calendar calendar = Calendar.getInstance();
		Timestamp currentTimestamp = new Timestamp(calendar.getTime().getTime());
		lastUpdated = currentTimestamp;
	}
	
	public void setTemperature(String temp) {
		temperature = Integer.parseInt(temp);
	}
	
	public void setWindSpeed(String wind) {
		windSpeed = Integer.parseInt(wind);
		//System.out.println("Set wind speed: " + wind);
	}
	
	public void setPrecip(String precip) {
		liquidPrecip = Double.parseDouble(precip);
		//System.out.println("Set precip: " + precip);
	}
	
	public void setIce(String ice) {
		iceAccum = Double.parseDouble(ice);
		//System.out.println("Set ice: " + ice);
	}
	
	public void setSnow(String snow) {
		snowAmount = Double.parseDouble(snow);
		//System.out.println("Set snow: " + snow);
	}
	
	public void setGust(String gust) {
		gustSpeeds = Integer.parseInt(gust);
		//System.out.println("Set gust: " + gust);
	}
	
	public void setHumidity(String humid) {
		humidity = Integer.parseInt(humid);
		//System.out.println("Set humidity: " + humid);
	}
	
	public String getTimeApplicable() {
		return timeApplicable;
	}
	
	public Timestamp getTimeUpdated() {
		return lastUpdated;
	}
	
	public int getTemperature() {
		return temperature;
	}
	
	public int getWindSpeed() {
		return windSpeed;
	}
	
	public double getPrecip() {
		return liquidPrecip;
	}
	
	public double getIce() {
		return iceAccum;
	}
	
	public double getSnow() {
		return snowAmount;
	}
	
	public int getGust() {
		return gustSpeeds;
	}
	
	public int getHumidity() {
		return humidity;
	}
	
	public int getZip() {
		return zipcode;
	}
	
	public void printWeather() {
		System.out.println("zipcode: " + zipcode + " timeapp: " + timeApplicable + " timeUpdate " + lastUpdated + " temperature: "
				 + temperature + " liquidPrecip: " + liquidPrecip + " windSpeed: " + windSpeed + " iceAccum " + iceAccum + 
				 " snowAmount " + snowAmount + " gustSpeeds: " + gustSpeeds + " humidty: " + humidity);
	}
}