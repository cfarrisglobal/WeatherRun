package soap;


import javax.xml.soap.*;
import javax.xml.ws.Dispatch;
import javax.xml.ws.Service;
import javax.xml.ws.soap.SOAPFaultException;
import javax.xml.namespace.QName;



import java.io.*;
import java.net.URL;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.concurrent.TimeUnit;

public class NDFD_SOAP
{
	public static void main(String[] args) throws Exception
	{
		
		//Put all valid zip codes stored in txt file into arraylist
		XMLParser originalXMLParser = new XMLParser();
		ArrayList<String[]> Zips = new ArrayList();
		Zips = getZips();
		
		weatherRequest[] threadArray = new weatherRequest[1];
		for(int x = 0; x < threadArray.length; x++)
		{
			threadArray[x] = new weatherRequest(Integer.toString(x), Zips, originalXMLParser);
			threadArray[x].start();
			try {
				TimeUnit.MILLISECONDS.sleep(50);
			} catch (InterruptedException e) {
				// Catch the interrupt
				e.printStackTrace();
			}
		}
		//originalXMLParser.start();
	}
	
	public static ArrayList getZips()
	{
		String zipCodeFile = "NewZipFile.txt";
		String line = null;
		ArrayList<String[]> allZips = new ArrayList();
		
		try {
			// Create Filereader
			FileReader getZips = new FileReader(zipCodeFile);
			
			BufferedReader getZipsBufferedReader = new BufferedReader(getZips);
			
			//Read Each line and append any zip codes with 0's if it is less than length 5
			while((line = getZipsBufferedReader.readLine()) != null)
			{
				if(line.length() > 10)
				{
					String zipAndRest[] = line.split(" ");
					allZips.add(zipAndRest);
				}
			}
			
			//close file
			getZipsBufferedReader.close();
		}
		
		catch(FileNotFoundException ex)
		{
			System.out.println("Unable to open file '" + zipCodeFile + "'");
		}
		
		catch(IOException ex)
		{
			System.out.println("Error reading file '" + zipCodeFile + "'");
			ex.printStackTrace();
		}
		
		return allZips;
	}
}


class weatherRequest implements Runnable
{
	static boolean zipsSet = false;
	static int zipIndex = 0;
	static ArrayList<String[]> Zips = new ArrayList();
	static ArrayList<String[]> failedSOAPRequest = new ArrayList();
	private Thread t;
	private String threadName;
	static boolean printStuff = false;
	private XMLParser parser; 
	public weatherRequest(String name, ArrayList<String[]> Zipsx, XMLParser passedParser)
	{
		parser = passedParser;
		threadName = name;
		System.out.println("Creating " + threadName);
		if(zipsSet == false)
		{
			zipsSet = false;
			Zips = Zipsx;
		}
	}
	
	public void run()
	{
		// For each zip code stored then get the NDFD sml data a print to file
		while(zipIndex < Zips.size())
		{
			String fetchZip[] = Zips.get(zipIndex);
			incrementCount();
			SOAPRequest(fetchZip);
		}
		
		
	}
	
	public void start()
	{
		System.out.println("Starting " + threadName);
		if(t == null)
		{
			t = new Thread (this, threadName);
			t.start();
		}
	}
	
	private static synchronized void incrementCount()
	{
		zipIndex++;
	}
	
	private void requestFail(String[] failed)
	{
		//If a request has not already failed
		if(failedSOAPRequest.contains(failed) == false)
		{
			System.out.println("Request for " + failed[1] + " in thread " + threadName + " has failed!");
			failedSOAPRequest.add(failed);
			Zips.add(failed);
		}
	}
	
	public void SOAPRequest(String[] zip)
	{       
			String zipToGet[] = zip;
		try {
            System.out.println("Geocode Vals for zip code " + zipToGet[0] + " are: " + zipToGet[1]);
            String[] temp = new String[2];
            temp[0] = zipToGet[0];
            temp[1] = zipToGet[1];
            String geocodeVals = temp[1];
            //System.out.println(geocodeVals);
            MessageFactory msgFactory = MessageFactory.newInstance();
            String nsSchema = "http://graphical.weather.gov/xml/DWMLgen/schema/DWML.xsd";
            String soapSchema = "http://schemas.xmlsoap.org/soap/envelope/";
            String xsiSchema = "http://www.w3.org/2001/XMLSchema-instance";
            String encodingStyle  = "http://schemas.xmlsoap.org/soap/encoding/"; 
            
            String wsdl = "http://graphical.weather.gov/xml/SOAP_server/ndfdXMLserver.php?wsdl";
            String targetNS = "http://graphical.weather.gov/xml/DWMLgen/wsdl/ndfdXML.wsdl";
            
            URL url = new URL(wsdl);
            QName serviceName = new QName(targetNS, "ndfdXML");
            QName portName = new QName(targetNS, "ndfdXMLPort");
            Service service = Service.create(url, serviceName);
            
            /*
             * NDFDgenLatLonList operation: gets weather data for a given
             * latitude, longitude pair
             * 
             * Format of the Message: 
             * <SOAP-ENV:Envelope xmlns:SOAP-ENV="http://schemas.xmlsoap.org/soap/envelope/" xmlns:xsd="http://www.w3.org/2001/XMLSchema" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:SOAP-ENC="http://schemas.xmlsoap.org/soap/encoding/" SOAP-ENV:encodingStyle="http://schemas.xmlsoap.org/soap/encoding/">
					<SOAP-ENV:Body>
						<ns8077:LatLonListZipCode xmlns:ns8077="uri:DWMLgen">
							<listZipCodeList xsi:type="xsd:string">25414</listZipCodeList>
						</ns8077:LatLonListZipCode>
					</SOAP-ENV:Body>
				</SOAP-ENV:Envelope>
            */
            SOAPFactory soapFactory = SOAPFactory.newInstance();
            SOAPMessage getWeatherMsg = msgFactory.createMessage();
            SOAPHeader header = getWeatherMsg.getSOAPHeader();
            header.detachNode();  // no header needed
            SOAPBody body = getWeatherMsg.getSOAPBody();
            Name functionCall = soapFactory.createName("NDFDgenLatLonList", "schNS", nsSchema);
            SOAPBodyElement fcElement = body.addBodyElement(functionCall);
            Name attname = soapFactory.createName("encodingStyle", "S", soapSchema);
            fcElement.addAttribute(attname, soapSchema);
            SOAPElement geocodeElement = fcElement.addChildElement("listLatLon");
            geocodeElement.addTextNode(geocodeVals);
            SOAPElement product = fcElement.addChildElement("product");
            product.addTextNode("time-series");
            SOAPElement weatherParameters = fcElement.addChildElement("weatherParameters");
            //SOAPElement maxt = weatherParameters.addChildElement("maxt");
            
            //todo make the message more precise to only request the elements we need
            
            // make web service call using this SOAPMessage
            Dispatch<SOAPMessage> smDispatch = null;
            smDispatch = service.createDispatch(portName, SOAPMessage.class, Service.Mode.MESSAGE);
            SOAPMessage weatherMsg = smDispatch.invoke(getWeatherMsg);
            // weatherMsg.writeTo(System.out); // debugging only
            
            
            // Metro needs normalize() command because it breaks
            // up child dwml element into numerous text nodes.
            weatherMsg.getSOAPBody().getElementsByTagName("dwmlOut").item(0).normalize();
            
            // First child of dwmlOut is the dwml element that we need.
            // It is the root node of the weather data that we will
            // be using to generate the report.
            String weatherResponse = weatherMsg.getSOAPBody().getElementsByTagName("dwmlOut").item(0).getFirstChild().getNodeValue();
            //System.out.println("WR: " + weatherResponse);
            putXMLParser(weatherResponse, zipToGet[0], zipToGet[1]);
        	} catch (SOAPFaultException e) {
        		System.out.println("SOAPFaultException: " + e.getFault().getFaultString());
        		requestFail(zip);
        	} catch (Exception e) {
        	  System.out.println("Exception: " + e.getMessage());
        	  requestFail(zip);
        }
	}
	
	public void putXMLParser(String response, String zip, String coordinates) throws Exception
	{
		try {
			FileWriter writer = new FileWriter("C:\\Users\\Cody\\workspace\\soap\\NDFDFiles\\" + zip + ".xml");
			
			writer.write(response);
			
			writer.close();
		}
		catch(IOException ex)
		{
			System.out.println("Error writing file '" + zip + ".xml" + "'");
			ex.printStackTrace();
		}
		
		parser.putXMLResponse(response, zip, coordinates);
	}
}

class XMLParser
{
	private static ArrayList<String[]> XMLDocuments = new ArrayList();
	
	public void XMLParser()
	{
		
	}
	
	public void start() throws Exception
	{
		while(XMLDocuments.isEmpty())
		{
			//TimeUnit.MILLISECONDS.wait(500);
		}
		//
	}
	
	public void putXMLResponse(String XML, String zip, String coordinates) throws Exception
	{
		String[] temp = new String[]{XML, zip, coordinates};
		XMLDocuments.add(temp);
		parse();
	}
	
	private static synchronized String[] getXMLDoc()
	{
		String[] temp = XMLDocuments.get(0);
		XMLDocuments.remove(0);
		return temp;
	}
	
	private void parse() throws Exception
	{
		//Attempt 2 at creating a decent parse
		//First get the document toParse which is in XML, zip, coordinate format
		String[] toParse = getXMLDoc();
		String xml = toParse[0];
		ArrayList<String> pulledLines = new ArrayList();
		String[] keywords = new String[]{"Temperature", "Liquid Precipitation Amount", "Wind Speed", "Ice Accumulation", "Snow Amount", "Wind Speed Gust", "Relative Humidity"}; 
		
		// Set the initial tag locations around the spot we are looking for  data
		int endOfDocument = xml.lastIndexOf(">");
		int indexForLeftPre = xml.indexOf("<");
		int indexForRightPre = xml.indexOf(">");
		int indexForLeftPost = xml.indexOf("<", indexForRightPre);
		int indexForRightPost = xml.indexOf(">", indexForRightPre + 1);
		
		// The old indexes are used to pull the previous tag whenever we find a match to a keyword. This tag contains the time stamp key
		// that we need to match to the data that follows
		int oldIndexForLeftPostTemp = 0;
		int oldIndexForRightPostTemp = 0;
		int nextOldIndexForLeftPostTemp = 0;
		int nextOldIndexForRightPostTemp = 0;
		int nextNextOldIndexForLeftPostTemp = 0;
		int nextNextOldIndexForRightPostTemp = 0;
		String toAddToPulledLines = " ";
		System.out.println(endOfDocument + " " + indexForLeftPre + " " + indexForRightPre + " " + indexForLeftPost + " " + indexForRightPost);
		
		//The loop continues until the end of the document is reached
		while(indexForRightPost != endOfDocument)
		{
			//Whenever there is space between 2 tags we pull that line to add to our arraylist of strings
			if(indexForRightPre != (indexForLeftPost - 1))
			{
				toAddToPulledLines = xml.substring(indexForRightPre + 1, indexForLeftPost);
				
				
				if(toAddToPulledLines.trim().isEmpty() != true)
				{
					//System.out.println(toAddToPulledLines);
					pulledLines.add(toAddToPulledLines);
				}

				
				
				// This for loop searches for keywords that we need to pull the previous tag on.
				for(int y = 0; y < keywords.length; y++)
				{
					if((keywords[y].equalsIgnoreCase(toAddToPulledLines)) == true)
					{
						//System.out.println(keywords[y].equals(toAddToPulledLines));
						pulledLines.add(xml.substring(nextNextOldIndexForLeftPostTemp, nextNextOldIndexForRightPostTemp));
						//System.out.println(toAddToPulledLines + " " + keywords[y]);
						//System.out.println(xml.substring(nextNextOldIndexForLeftPostTemp, nextNextOldIndexForRightPostTemp + 1));
					}
				}
			}
			
			//Logic to set the next set of indexes
			nextNextOldIndexForLeftPostTemp = nextOldIndexForLeftPostTemp;
			nextNextOldIndexForRightPostTemp = nextOldIndexForRightPostTemp;
			nextOldIndexForLeftPostTemp = oldIndexForLeftPostTemp;
			nextOldIndexForRightPostTemp = oldIndexForRightPostTemp;
			oldIndexForLeftPostTemp = xml.indexOf("<", indexForRightPost + 1);
			oldIndexForRightPostTemp = xml.indexOf(">", indexForRightPost + 1);
			
			indexForLeftPre = indexForLeftPost;
			indexForRightPre = indexForRightPost;
			indexForLeftPost = oldIndexForLeftPostTemp;
			indexForRightPost = oldIndexForRightPostTemp;
			toAddToPulledLines = " ";
			//System.out.println(endOfDocument + " " + indexForLeftPre + " " + indexForRightPre + " " + indexForLeftPost + " " + indexForRightPost);
		}
		
		int pulledLinesIndex = 0;
		int pulledLinesSize = pulledLines.size();
		ArrayList<ArrayList> dataTimeStamps = new ArrayList();
		ArrayList<ArrayList> dataWeather = new ArrayList();
		
		//This while loop creates two arrayList containing arrayLists of Strings
		//dataTimeStamps contain the arraylists with the time stamp key in the first string of the arraylist and all the relevant timestamps that follow
		//dataWeather is similar with the first string being the name of the data contained, the second string being the timestamp key
		// and all the relevant data we are trying to collect follows.
		while(pulledLinesIndex < pulledLinesSize)
		{
			String toSort = pulledLines.get(pulledLinesIndex);
			pulledLinesIndex++;
			//System.out.println(toSort);
			// Test if this is a timestamp key
			//first if statement is for error checking
			if(toSort.length() > 2)
			{
				// This if line tests to see if the block of data is one of the keywords types that we are looking for
				if((toSort.equalsIgnoreCase(keywords[0]) == true) || (toSort.equalsIgnoreCase(keywords[1]) == true) || (toSort.equalsIgnoreCase(keywords[2]) == true) || (toSort.equalsIgnoreCase(keywords[3]) == true) || (toSort.equalsIgnoreCase(keywords[4]) == true) || (toSort.equalsIgnoreCase(keywords[5]) == true) || (toSort.equalsIgnoreCase(keywords[6]) == true))
				{
					ArrayList<String> dataWeatherTemp = new ArrayList();
					
					//keyword is added
					dataWeatherTemp.add(toSort);
					//System.out.println(toSort);
					
					toSort = pulledLines.get(pulledLinesIndex);
					pulledLinesIndex++;
					
					//timestamp key is pulled from the line and added
					dataWeatherTemp.add(toSort.substring(toSort.indexOf("k-"), toSort.length() - 2));
					//System.out.println(toSort.substring(toSort.indexOf("k-"), toSort.length() - 2));
					
					toSort = pulledLines.get(pulledLinesIndex);
					pulledLinesIndex++;
					//This while loop takes each of the lines of data and adds it to the temporty arraylist
					while(toSort.length() < 8)
					{
						dataWeatherTemp.add(toSort);
						toSort = pulledLines.get(pulledLinesIndex);
						pulledLinesIndex++;
						
						//System.out.println(toSort + "here");
					}
					// The temp arraylist is added to the arraylist of arraylist dataWeather
					pulledLinesIndex--;
					dataWeather.add(dataWeatherTemp);
				}
				//else if statement for checking for timestamps which always start with k-
				else if((toSort.charAt(0) == 'k') && (toSort.charAt(1) == '-'))
				{
					//Add timestamp key and then following timestamps to a String arraylist
					ArrayList<String> timeStampTemp = new ArrayList();
					//Temp arraylist to be added after the while loop completes
					timeStampTemp.add(toSort);
					//While loop goes through each of the timestamps and adds them to the temporary arraylist
					while(toSort.length() < 8 || (toSort.equals("Daily Maximum Temperature") != true))
					{
						toSort = pulledLines.get(pulledLinesIndex);
						timeStampTemp.add(toSort);
						pulledLinesIndex++;
					}
					// the temporary arraylist is added to the arraylist of arraylist dataTimeStamps
					dataTimeStamps.add(timeStampTemp);
				}
				
			}
		}
		
		//Now we need to construct objects for each hour to be added to the database
		//64 hours are set for each parsed document
		
		ArrayList temp = dataWeather.get(0);
		String parameterName = (String) temp.get(0);
		String parameterKey = (String) temp.get(1);
		//System.out.println(parameterName + " " + parameterKey);
		
		//First for loop initialilzes the hourPrediction objects with the first dataWeather arraylist
		hourPrediction[] prediction = new hourPrediction[temp.size()-2];
		for(int z = 2; z < temp.size(); z++)
		{
			//System.out.println(temp.get(z));
			prediction[z-2](toParse[1], //todo )
		}
		
		for(int a = 1; a < dataWeather.size(); a++)
		{

		}
	}
}

class hourPrediction
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
	
	public void hourPrediction(String zip, String timeApp)
	{
		zipcode = Integer.parseInt(zip);
		timeApplicable = timeApp;
	}
	
	private void updateTime()
	{
		Calendar calendar = Calendar.getInstance();
		Timestamp currentTimestamp = new Timestamp(calendar.getTime().getTime());
		lastUpdated = currentTimestamp;
	}
	
	public void setTemperature(String temp)
	{
		temperature = Integer.parseInt(temp);
	}
	
	public void setWindSpeed(String wind)
	{
		windSpeed = Integer.parseInt(wind);
	}
	
	public void setPrecip(String precip)
	{
		liquidPrecip = Double.parseDouble(precip);
	}
	
	public void setIce(String ice)
	{
		iceAccum = Double.parseDouble(ice);
	}
	
	public void setSnow(String snow)
	{
		snowAmount = Double.parseDouble(snow);
	}
	
	public void setGust(String gust)
	{
		gustSpeeds = Integer.parseInt(gust);
	}
	
	public void setHumidity(String humid)
	{
		humidity = Integer.parseInt(humid);
	}
}
