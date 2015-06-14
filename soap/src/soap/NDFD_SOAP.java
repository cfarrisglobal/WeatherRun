package soap;


import javax.xml.soap.*;
import javax.xml.ws.Dispatch;
import javax.xml.ws.Service;
import javax.xml.ws.soap.SOAPFaultException;
import javax.xml.namespace.QName;








import java.io.*;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class NDFD_SOAP
{
	public static void main(String[] args) throws Exception
	{
		
		//Put all valid zip codes stored in txt file into arraylist
		ArrayList<String[]> Zips = new ArrayList();
		Zips = getZips();
		
		weatherRequest[] threadArray = new weatherRequest[2];
		for(int x = 0; x < threadArray.length; x++)
		{
			threadArray[x] = new weatherRequest(Integer.toString(x), Zips);
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
	private static boolean printStuff = false;
	private static ArrayList<hourPrediction> masterHourPrediction;
	private static int masterHourPredictionIndex = 0;
	private static int hourPredictionsSinceLastInsert = 0;
	
	public weatherRequest(String name, ArrayList<String[]> Zipsx)
	{
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
		masterHourPrediction = new ArrayList();
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
			System.out.println("Request for " + failed[0] + " in thread " + threadName + " has failed!");
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
        	  System.out.println(Thread.currentThread().getStackTrace()[2].getLineNumber());
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
		
		putXMLResponse(response, zip, coordinates);
	}

	
	public void putXMLResponse(String XML, String zip, String coordinates) throws Exception
	{
		String[] temp = new String[]{XML, zip, coordinates};
		hourPrediction[] fromParse = parse(temp);
		populateDatabase(fromParse);
	}
	
	private static synchronized void populateDatabase(hourPrediction[] fromParse)
	{
		for(int x = 0; x < fromParse.length; x++)
		{
			masterHourPrediction.add(fromParse[x]);
		}
		if(hourPredictionsSinceLastInsert >= 1000)
		{
			hourPredictionsSinceLastInsert = 0;
			//Database insert here
			
			Connection con = null;
	        Statement st = null;
	        ResultSet rs = null;

	        // Current database is hosted on local development machine
	        String url = "jdbc:mysql://localhost:3306/foo";
	        
	        // Local database has this user set up for access to latlon and weatherprediction tables on database foo
	        String user = "tester";
	        String password = "userpass";
	        try {
	            // The newInstance() call is a work around for some
	            // broken Java implementations

	            Class.forName("com.mysql.jdbc.Driver").newInstance();
	        } catch (Exception ex) {
	            // handle the error
	        }
	        //System.out.println("Here 3");
	        try {
	        	// Create Drivermanager and execute versionquery
	            con = DriverManager.getConnection(url, user, password);
	            st = con.createStatement();
	            rs = st.executeQuery("SELECT VERSION()");

	            // Output versionquery to check for established connection
	            if (rs.next()) {
	                System.out.println(rs.getString(1));
	            }
	            
	            /*
	             * The following prepared statement creates a new weatherprediction row if the zipcode and time_Applicable
	             * do not exist in the current database. If it does exists the ON DUPLICATE KEY UPDATE activates and
	             * everything is updated with the current data except for the key which contains the zipcode and time_applicable
	             * 
	             * Later we may need to test for nulls so that we are not replacing a valid data point with a null datapoint
	             * since we are most interested in the latest data for each prediction. As time moves forward the current
	             * time will no longer be available in the NDFD prediction data.
	             * 
	            */
	            
	            PreparedStatement pst = con.prepareStatement("INSERT INTO weatherprediction(zipcode, time_Applicable, last_Updated"
	            		+ ", temp, liquid_Precip, wind_Speed, ice_Accum, snow_Amount, gust_Speeds, humidity) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?) "
	            		+ "ON DUPLICATE KEY UPDATE zipcode = zipcode, time_Applicable = time_Applicable, last_Updated = ?, temp = ?,"
	            		+ " liquid_Precip = ?, wind_Speed = ?, ice_Accum = ?, snow_Amount = ?, gust_Speeds = ?, humidity = ?;");
	            
	            //tempMasterhourPredictionIndex is a copy of masterHourPredictionIndex so that we can update
	            //the number of predictions we have already added to the database
	            
	            int tempMasterHourPredictionIndex = 0;
	            // We have to create a statement for each line in the zip file
	            for(int x = 0; x < masterHourPrediction.size() - masterHourPredictionIndex; x++)
	            {
		            hourPrediction currentHourPrediction = masterHourPrediction.get((masterHourPredictionIndex + x));
		            
		            tempMasterHourPredictionIndex++;
		            
		            currentHourPrediction.printWeather();
		            // setstring sets the ? values in the preparedStatement with the data from the zips arraylist
		            pst.setInt(1, currentHourPrediction.getZip());
		            pst.setString(2, currentHourPrediction.getTimeApplicable());
		            pst.setString(3, currentHourPrediction.getTimeUpdated().toString());
		            pst.setInt(4, currentHourPrediction.getTemperature());
		            pst.setDouble(5, currentHourPrediction.getPrecip());
		            pst.setInt(6, currentHourPrediction.getWindSpeed());
		            pst.setDouble(7, currentHourPrediction.getIce());
		            pst.setDouble(8, currentHourPrediction.getSnow());
		            pst.setInt(9, currentHourPrediction.getGust());
		            pst.setInt(10, currentHourPrediction.getHumidity());
		            pst.setString(11, currentHourPrediction.getTimeUpdated().toString());
		            pst.setInt(12, currentHourPrediction.getTemperature());
		            pst.setDouble(13, currentHourPrediction.getPrecip());
		            pst.setInt(14, currentHourPrediction.getWindSpeed());
		            pst.setDouble(15, currentHourPrediction.getIce());
		            pst.setDouble(16, currentHourPrediction.getSnow());
		            pst.setInt(17, currentHourPrediction.getGust());
		            pst.setInt(18, currentHourPrediction.getHumidity());
		            
		            // we add each statement to a batch that is later executed
		            pst.addBatch();
		            
		            System.out.println(x + " masterHourPrediction: " + masterHourPrediction.size() + " masterHourPredictionIndex: " + masterHourPredictionIndex);
		            
		            // 
		            if( x >= masterHourPrediction.size() - masterHourPredictionIndex - 1)
		            {
			            pst.executeBatch();
			            System.out.println("Executed " + x + " rows");
			            masterHourPredictionIndex += tempMasterHourPredictionIndex;
		            }
		            
	            }
	            
	        } catch (SQLException ex) {
	            Logger lgr = Logger.getLogger(populateLatLon.class.getName());
	            lgr.log(Level.SEVERE, ex.getMessage(), ex);

	        } finally {
	            try {
	                if (rs != null) {
	                    rs.close();
	                }
	                if (st != null) {
	                    st.close();
	                }
	                if (con != null) {
	                    con.close();
	                }

	            } catch (SQLException ex) {
	                Logger lgr = Logger.getLogger(populateLatLon.class.getName());
	                lgr.log(Level.WARNING, ex.getMessage(), ex);
	            }
		
	        }
		}
		else
		{
			hourPredictionsSinceLastInsert += fromParse.length;
		}
	}
	
	private hourPrediction[] parse(String[] parseThis) throws Exception
	{
		hourPrediction[] prediction = null;
		
		try {
			//Attempt 2 at creating a decent parse
			//First get the document toParse which is in XML, zip, coordinate format
			String[] toParse = parseThis;
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
			//System.out.println(endOfDocument + " " + indexForLeftPre + " " + indexForRightPre + " " + indexForLeftPost + " " + indexForRightPost);
			
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
			boolean isPassed = false;
			
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
						dataWeatherTemp.add(toSort.substring(toSort.indexOf("k-"), toSort.length() - 1));
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
					else if((toSort.charAt(0) == 'k') && (toSort.charAt(1) == '-') && isPassed == false)
					{
						//Add timestamp key and then following timestamps to a String arraylist
						ArrayList<String> timeStampTemp = new ArrayList();
						//Temp arraylist to be added after the while loop completes
						timeStampTemp.add(toSort);
						//System.out.println(toSort);
						toSort = pulledLines.get(pulledLinesIndex);
						pulledLinesIndex++;
						//While loop goes through each of the timestamps and adds them to the temporary arraylist
						while(((toSort.charAt(0) != 'k') && (toSort.charAt(1) != '-')) && (toSort.equalsIgnoreCase("Daily Maximum Temperature") == false))
						{
							
							timeStampTemp.add(toSort);
							//System.out.println(toSort);
							toSort = pulledLines.get(pulledLinesIndex);
							pulledLinesIndex++;
						}
						// the temporary arraylist is added to the arraylist of arraylist dataTimeStamps
						dataTimeStamps.add(timeStampTemp);
					}
					
				}
			}
			
			//Now we need to construct objects for each hour to be added to the database
			//64 hours are set for each parsed document
			
			//tempParameter holds one of the data types that we are looking to store in the case of dataWeather.get(0) it is temperature
			ArrayList tempParameter = dataWeather.get(0);
			//parameterName should just be "Temperature"
			String parameterName = (String) tempParameter.get(0);
			//dataTimeKey is the timestamp key that pertains to the parameter temperature
			String dataTimeKey = (String) tempParameter.get(1);
			//System.out.println(parameterName + " " + parameterKey);
			
			//prediction is an array of objects that we are attempting to sort all of the parameters into for each hours prediction
			prediction = new hourPrediction[tempParameter.size()-2];
	
			// pulledFromDataTimeStamps is the first arraylist of timestamps stored in dataTimeStamps arraylist of arraylists
			// its initialized here for reasons but later the same data is initialized in the for loop
			ArrayList pulledFromDataTimeStamps = dataTimeStamps.get(0);
			//System.out.println(dataTimeStamps.size());
			
			// relevantTimeData is the index of the arrayList containing the timeStamps inside dataTimeStamps
			int relevantTimeData = 0;
			
			//goal of this for loop is to find the index of the matching timestamp keys between the arraylists of timestamps and
			//the parameters stored key
			for(int b = 0; b < dataTimeStamps.size(); b++)
			{
				pulledFromDataTimeStamps = dataTimeStamps.get(b);
				String timeStampTest = (String) pulledFromDataTimeStamps.get(0);
				//System.out.println(dataTimeKey + " " + timeStampTest);
				if(timeStampTest.equals(dataTimeKey))
				{
					//System.out.println(dataTimeKey + " " + timeStampTest + " " + "success" + " " + parameterName);
					relevantTimeData = b;
					break;
					
				}
			}
			//System.out.println("relevantTimeData: " + relevantTimeData);
			
			ArrayList masterTimeStamp = dataTimeStamps.get(relevantTimeData);
			//System.out.println("tempTimeStamp length: " + tempTimeStamp.size());
			
			//The follow sets temperature
			for(int z = 2; z < tempParameter.size(); z++)
			{
				//System.out.println(tempParameter.get(z));
				//System.out.println(masterTimeStamp.get(z-1));
				String predictionTimeStamp = (String) masterTimeStamp.get(z-1);
				prediction[z-2] = new hourPrediction(toParse[1], predictionTimeStamp);
				prediction[z-2].setTemperature((String) tempParameter.get(z)); 
			}
			
	
			
			//The following sets liquid precipitation amount
			tempParameter = dataWeather.get(1);
			parameterName = (String) tempParameter.get(0);
			dataTimeKey = (String) tempParameter.get(1);
			pulledFromDataTimeStamps = dataTimeStamps.get(0);
			//System.out.println(dataTimeStamps.size());
			relevantTimeData = 0;
			for(int b = 0; b < dataTimeStamps.size(); b++)
			{
				pulledFromDataTimeStamps = dataTimeStamps.get(b);
				String timeStampTest = (String) pulledFromDataTimeStamps.get(0);
				//System.out.println(dataTimeKey + " " + timeStampTest);
				if(timeStampTest.equals(dataTimeKey))
				{
					//System.out.println(dataTimeKey + " " + timeStampTest + " " + "success");
					relevantTimeData = b;
					break;
					
				}
			}
			// relevantTimeData is the index of the arrayList containing the timeStamps inside dataTimeStamps
	
			// The offset of the current timestamp arraylist that we are looking at against the masterTimeStamps arraylist
			// that we used to create the objects with
			int offSetFromMaster = 0;
			for(int a = 1; a < pulledFromDataTimeStamps.size(); a++)
			{
				if(pulledFromDataTimeStamps.get(a).equals(masterTimeStamp.get(a)))
				{
					offSetFromMaster = a - 1;
					//System.out.println("Sucess " + offSetFromMaster + " " + pulledFromDataTimeStamps.get(a) + " " + parameterName);
					break;
				}
			}
			
			//Now that we know the offset we can set precipitation amount
			for(int c = offSetFromMaster + 2; c < tempParameter.size(); c++)
			{
				prediction[c].setPrecip((String) tempParameter.get(c)); 
			}
			
			//The following sets wind speed
			tempParameter = dataWeather.get(2);
			parameterName = (String) tempParameter.get(0);
			dataTimeKey = (String) tempParameter.get(1);
			pulledFromDataTimeStamps = dataTimeStamps.get(0);
			//System.out.println(dataTimeStamps.size());
			relevantTimeData = 0;
			for(int b = 0; b < dataTimeStamps.size(); b++)
			{
				pulledFromDataTimeStamps = dataTimeStamps.get(b);
				String timeStampTest = (String) pulledFromDataTimeStamps.get(0);
				//System.out.println(dataTimeKey + " " + timeStampTest);
				if(timeStampTest.equals(dataTimeKey))
				{
					//System.out.println(dataTimeKey + " " + timeStampTest + " " + "success");
					relevantTimeData = b;
					break;
					
				}
			}
			// relevantTimeData is the index of the arrayList containing the timeStamps inside dataTimeStamps
	
			// The offset of the current timestamp arraylist that we are looking at against the masterTimeStamps arraylist
			// that we used to create the objects with
			offSetFromMaster = 0;
			for(int a = 1; a < pulledFromDataTimeStamps.size(); a++)
			{
				if(pulledFromDataTimeStamps.get(a).equals(masterTimeStamp.get(a)))
				{
					offSetFromMaster = a - 1;
					//System.out.println("Sucess " + offSetFromMaster + " " + pulledFromDataTimeStamps.get(a) + " " + parameterName);
					break;
				}
			}
			
			//Now that we know the offset we can set wind amount 
			for(int c = offSetFromMaster + 2; c < tempParameter.size(); c++)
			{
				prediction[c-2].setWindSpeed((String) tempParameter.get(c)); 
			}
			
			//The following sets Ice accumulation
			tempParameter = dataWeather.get(3);
			parameterName = (String) tempParameter.get(0);
			dataTimeKey = (String) tempParameter.get(1);
			pulledFromDataTimeStamps = dataTimeStamps.get(0);
			//System.out.println(dataTimeStamps.size());
			relevantTimeData = 0;
			for(int b = 0; b < dataTimeStamps.size(); b++)
			{
				pulledFromDataTimeStamps = dataTimeStamps.get(b);
				String timeStampTest = (String) pulledFromDataTimeStamps.get(0);
				//System.out.println(dataTimeKey + " " + timeStampTest);
				if(timeStampTest.equals(dataTimeKey))
				{
					//System.out.println(dataTimeKey + " " + timeStampTest + " " + "success");
					relevantTimeData = b;
					break;
					
				}
			}
			// relevantTimeData is the index of the arrayList containing the timeStamps inside dataTimeStamps
	
			// The offset of the current timestamp arraylist that we are looking at against the masterTimeStamps arraylist
			// that we used to create the objects with
			offSetFromMaster = 0;
			for(int a = 1; a < pulledFromDataTimeStamps.size(); a++)
			{
				if(pulledFromDataTimeStamps.get(a).equals(masterTimeStamp.get(a)))
				{
					offSetFromMaster = a - 1;
					//System.out.println("Sucess " + offSetFromMaster + " " + pulledFromDataTimeStamps.get(a) + " " + parameterName);
					break;
				}
			}
			
			//Now that we know the offset we can set wind amount 
			int overallIceCount = 0;
			for(int c = offSetFromMaster + 2; c < tempParameter.size(); c++)
			{
				for(int d = 0; d < 6; d++)
				{
					if(overallIceCount >= prediction.length)
					{
						break;
					}
					prediction[(c-2)*6 + d].setIce((String) tempParameter.get(c)); 
					overallIceCount++;
				}
				
			}
			
			//The following sets snow amount
			tempParameter = dataWeather.get(4);
			parameterName = (String) tempParameter.get(0);
			dataTimeKey = (String) tempParameter.get(1);
			pulledFromDataTimeStamps = dataTimeStamps.get(0);
			//System.out.println(dataTimeStamps.size());
			relevantTimeData = 0;
			for(int b = 0; b < dataTimeStamps.size(); b++)
			{
				pulledFromDataTimeStamps = dataTimeStamps.get(b);
				String timeStampTest = (String) pulledFromDataTimeStamps.get(0);
				//System.out.println(dataTimeKey + " " + timeStampTest);
				if(timeStampTest.equals(dataTimeKey))
				{
					//System.out.println(dataTimeKey + " " + timeStampTest + " " + "success");
					relevantTimeData = b;
					break;
					
				}
			}
			// relevantTimeData is the index of the arrayList containing the timeStamps inside dataTimeStamps
	
			// The offset of the current timestamp arraylist that we are looking at against the masterTimeStamps arraylist
			// that we used to create the objects with
			offSetFromMaster = 0;
			for(int a = 1; a < pulledFromDataTimeStamps.size(); a++)
			{
				if(pulledFromDataTimeStamps.get(a).equals(masterTimeStamp.get(a)))
				{
					offSetFromMaster = a - 1;
					//System.out.println("Sucess " + offSetFromMaster + " " + pulledFromDataTimeStamps.get(a) + " " + parameterName);
					break;
				}
			}
			
			//Now that we know the offset we can set wind amount 
			int overallSnowCount = 0;
			for(int c = offSetFromMaster + 2; c < tempParameter.size(); c++)
			{
				for(int d = 0; d < 6; d++)
				{
					if(overallSnowCount >= prediction.length)
					{
						break;
					}
					prediction[(c-2)*6 + d].setSnow((String) tempParameter.get(c)); 
					overallSnowCount++;
				}
				
			}
			
			//The following sets Wind Gust Speed
			tempParameter = dataWeather.get(5);
			parameterName = (String) tempParameter.get(0);
			dataTimeKey = (String) tempParameter.get(1);
			pulledFromDataTimeStamps = dataTimeStamps.get(0);
			//System.out.println(dataTimeStamps.size());
			relevantTimeData = 0;
			for(int b = 0; b < dataTimeStamps.size(); b++)
			{
				pulledFromDataTimeStamps = dataTimeStamps.get(b);
				String timeStampTest = (String) pulledFromDataTimeStamps.get(0);
				//System.out.println(dataTimeKey + " " + timeStampTest);
				if(timeStampTest.equals(dataTimeKey))
				{
					//System.out.println(dataTimeKey + " " + timeStampTest + " " + "success");
					relevantTimeData = b;
					break;
					
				}
			}
			// relevantTimeData is the index of the arrayList containing the timeStamps inside dataTimeStamps
	
			// The offset of the current timestamp arraylist that we are looking at against the masterTimeStamps arraylist
			// that we used to create the objects with
			offSetFromMaster = 0;
			for(int a = 1; a < pulledFromDataTimeStamps.size(); a++)
			{
				if(pulledFromDataTimeStamps.get(a).equals(masterTimeStamp.get(a)))
				{
					offSetFromMaster = a - 1;
					//System.out.println("Sucess " + offSetFromMaster + " " + pulledFromDataTimeStamps.get(a) + " " + parameterName);
					break;
				}
			}
			
			//Now that we know the offset we can set wind amount 
			int overallGustCount = 0;
			for(int c = offSetFromMaster + 2; c < tempParameter.size(); c++)
			{
				for(int d = 0; d < 3; d++)
				{
					if(overallGustCount >= prediction.length)
					{
						break;
					}
					prediction[(c-2)*3 + d].setGust((String) tempParameter.get(c)); 
					overallGustCount++;
				}
				
			}
			
			//The following sets Humidity
			tempParameter = dataWeather.get(6);
			parameterName = (String) tempParameter.get(0);
			dataTimeKey = (String) tempParameter.get(1);
			pulledFromDataTimeStamps = dataTimeStamps.get(0);
			//System.out.println(dataTimeStamps.size());
			relevantTimeData = 0;
			for(int b = 0; b < dataTimeStamps.size(); b++)
			{
				pulledFromDataTimeStamps = dataTimeStamps.get(b);
				String timeStampTest = (String) pulledFromDataTimeStamps.get(0);
				//System.out.println(dataTimeKey + " " + timeStampTest);
				if(timeStampTest.equals(dataTimeKey))
				{
					//System.out.println(dataTimeKey + " " + timeStampTest + " " + "success");
					relevantTimeData = b;
					break;
					
				}
			}
			// relevantTimeData is the index of the arrayList containing the timeStamps inside dataTimeStamps
	
			// The offset of the current timestamp arraylist that we are looking at against the masterTimeStamps arraylist
			// that we used to create the objects with
			offSetFromMaster = 0;
			for(int a = 1; a < pulledFromDataTimeStamps.size(); a++)
			{
				if(pulledFromDataTimeStamps.get(a).equals(masterTimeStamp.get(a)))
				{
					offSetFromMaster = a - 1;
					//System.out.println("Sucess " + offSetFromMaster + " " + pulledFromDataTimeStamps.get(a) + " " + parameterName);
					break;
				}
			}
			
			//Now that we know the offset we can set wind amount 
			int overallHumCount = 0;
			for(int c = offSetFromMaster + 2; c < tempParameter.size(); c++)
			{
				for(int d = 0; d < 3; d++)
				{
					if(overallHumCount >= prediction.length)
					{
						break;
					}
					prediction[(c-2)*3 + d].setHumidity((String) tempParameter.get(c)); 
					overallHumCount++;
				}
				
			}
		}catch (Exception e) {
      	  //System.out.println("What the fuck is happening : " + e.getMessage());
      	  //System.out.println(Thread.currentThread().getStackTrace()[2].getLineNumber());
      }
		
		return prediction;
		
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
	

	public hourPrediction(String zip, String timeApp)
	{
		zipcode = Integer.parseInt(zip);
		timeApplicable = timeApp;
		updateTime();
		//System.out.println("Created hourPrediciton with zip: " + zip + " and timeStamp " + timeApp);
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
		//System.out.println("Set wind speed: " + wind);
	}
	
	public void setPrecip(String precip)
	{
		liquidPrecip = Double.parseDouble(precip);
		//System.out.println("Set precip: " + precip);
	}
	
	public void setIce(String ice)
	{
		iceAccum = Double.parseDouble(ice);
		//System.out.println("Set ice: " + ice);
	}
	
	public void setSnow(String snow)
	{
		snowAmount = Double.parseDouble(snow);
		//System.out.println("Set snow: " + snow);
	}
	
	public void setGust(String gust)
	{
		gustSpeeds = Integer.parseInt(gust);
		//System.out.println("Set gust: " + gust);
	}
	
	public void setHumidity(String humid)
	{
		humidity = Integer.parseInt(humid);
		//System.out.println("Set humidity: " + humid);
	}
	
	public String getTimeApplicable()
	{
		return timeApplicable;
	}
	
	public Timestamp getTimeUpdated()
	{
		return lastUpdated;
	}
	
	public int getTemperature()
	{
		return temperature;
	}
	
	public int getWindSpeed()
	{
		return windSpeed;
	}
	
	public double getPrecip()
	{
		return liquidPrecip;
	}
	
	public double getIce()
	{
		return iceAccum;
	}
	
	public double getSnow()
	{
		return snowAmount;
	}
	
	public int getGust()
	{
		return gustSpeeds;
	}
	
	public int getHumidity()
	{
		return humidity;
	}
	
	public int getZip()
	{
		return zipcode;
	}
	
	public void printWeather()
	{
		
		System.out.println("zipcode: " + zipcode + " timeapp: " + timeApplicable + " timeUpdate " + lastUpdated + " temperature: "
				 + temperature + " liquidPrecip: " + liquidPrecip + " windSpeed: " + windSpeed + " iceAccum " + iceAccum + 
				 " snowAmount " + snowAmount + " gustSpeeds: " + gustSpeeds + " humidty: " + humidity);
	}
}
