/*
 * Created by Charles Cody Farris 7/7/15
 * 
 * Last updated 7/8/15
 * 
 * Programs purpose is to populate a database with the National Weather Service's weather prediction data.
 * 
 * This is done by using SOAP requests to the NDFD database and then sorting and inserting the relevant data.
 * 
 */

package soap;

import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.namespace.QName;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.Name;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPBodyElement;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPFactory;
import javax.xml.soap.SOAPHeader;
import javax.xml.soap.SOAPMessage;
import javax.xml.ws.Dispatch;
import javax.xml.ws.Service;
import javax.xml.ws.soap.SOAPFaultException;

import latLon.populateLatLon;

class WeatherRequest implements Runnable {
	
	static boolean zipsSet = false;
	static int zipIndex = 0;
	static ArrayList<String[]> Zips = new ArrayList();
	static ArrayList<String[]> failedSOAPRequest = new ArrayList();
	private String[] keywords = new String[]{"Temperature", "Liquid Precipitation Amount", "Wind Speed", "Ice Accumulation",
			"Snow Amount", "Wind Speed Gust", "Relative Humidity"};
	private Thread t;
	private String threadName;
	private static boolean printStuff = false;
	private static ArrayList<HourPrediction> masterHourPrediction;
	private static int masterHourPredictionIndex = 0;
	private static int HourPredictionsSinceLastInsert = 0;
	
	/*
	 * Method WeatherRequest
	 * 
	 * Purpose: Constructor for class WeatherRequest. Helps create each thread and the first WeatherRequest thread
	 * created initialized the master zip arraylist.
	 */
	
	public WeatherRequest(String name, ArrayList<String[]> Zipsx) {
		threadName = name;
		System.out.println("Creating " + threadName);
		if(zipsSet == false) {
			zipsSet = false;
			Zips = Zipsx;
		}
	}
	
	/*
	 * Method run
	 * 
	 * Purpose: Upon creation of each WeatherRequest thread the run method actually indicates the code
	 * that each weatherRun thread is to run.
	 */
	
	public void run() {
		// For each zip code stored then get the NDFD xml data a print to file
		while(zipIndex < Zips.size()) {
			String fetchZip[] = Zips.get(zipIndex);
			incrementCount();
			String[] fromSOAPRequest = SOAPRequest(fetchZip);
			
			try {

				/*
				 * Used for debugging
				 * putXMLParser(fromSOAPRequest[0], fromSOAPRequest[1], fromSOAPRequest[2]);
				*/ 
				
				populateDatabase(parse(fromSOAPRequest));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	/*
	 * Method start
	 */
	
	public void start() {
		masterHourPrediction = new ArrayList();
		System.out.println("Starting " + threadName);
		if(t == null) {
			t = new Thread (this, threadName);
			t.start();
		}
	}
	
	/*
	 * Method incrementCount
	 * 
	 * Purpose: to increment the zipIndex so that threads are not requesting the same zip data from the ndfd servers.
	 */
	
	private static synchronized void incrementCount() {
		zipIndex++;
	}
	
	
	/*
	 * Method requestFail
	 * 
	 * Purpose: Sometimes even on a good request the NDFD servers will send back an empty message or an error.
	 * On these we activate requestFail and add the failed request in the back of the Zips arraylist. Each failed
	 * zip will be tried 1 more time in the current configuration.
	 */
	
	private void requestFail(String[] failed) {
		//If a request has not already failed
		if(failedSOAPRequest.contains(failed) == false) {
			System.out.println("Request for " + failed[0] + " in thread " + threadName + " has failed!");
			failedSOAPRequest.add(failed);
			Zips.add(failed);
		}
	}
	
	/*
	 * Method SOAPRequest
	 * 
	 * Purpose: This method builds the SOAP request and then wait to receive a response from the NDFD servers.
	 * It returns in a string array the xml response, zipcode, and coordinates.
	 */
	
	public String[] SOAPRequest(String[] zip) {       
			String zipToGet[] = zip;
			String weatherResponse = null;
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
            
            /* Example of SOAP request being built
             * 
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
            weatherResponse = weatherMsg.getSOAPBody().getElementsByTagName("dwmlOut").item(0).getFirstChild().getNodeValue();
            //System.out.println("WR: " + weatherResponse);
            
        	} catch (SOAPFaultException e) {
        		System.out.println("SOAPFaultException: " + e.getFault().getFaultString());
        		
        		//Place failed zip back into the queue to try again later
        		requestFail(zip);
        	} catch (Exception e) {
        	  System.out.println("Exception: " + e.getMessage());
        	  
        	//Place failed zip back into the queue to try again later
        	  requestFail(zip);
        	  System.out.println(Thread.currentThread().getStackTrace()[2].getLineNumber());
        	  
        	  
        }
		String[] requestReturn = new String[]{weatherResponse, zipToGet[0], zipToGet[1]};
		return requestReturn;
	}
	
	/*
	 * Method putXMLParser
	 * 
	 * Purpose: to write the xml document to a file on the local system. This file is used as a backup
	 * as well as a debugging tool. This method is not necessary to populate the weatherPrediction database.
	 */
	
	public void putXMLParser(String response, String zip, String coordinates) throws Exception {
		try {
			FileWriter writer = new FileWriter("C:\\Users\\Cody\\workspace\\soap\\NDFDFiles\\" + zip + ".xml");
			
			writer.write(response);
			
			writer.close();
		}
		catch(IOException ex) {
			System.out.println("Error writing file '" + zip + ".xml" + "'");
			ex.printStackTrace();
		}
	}

	
	/*
	 * PopulateDatabase method first takes the passed HourPrediction objects and adds them to a master list
	 * of HourPrediction objects. When this master list has greater than 1000 predictions that have not been
	 * then it takes that block and inserts it into the database.
	 * 
	 * This method is synchronized from all of the WeatherRequest threads
	 */
	
	private static synchronized void populateDatabase(HourPrediction[] fromParse) {
		
		//Receive the predictions from the parse and add them to our master list
		for(int x = 0; x < fromParse.length; x++) {
			masterHourPrediction.add(fromParse[x]);
		}
		
		// When there are more than 1000 predictions ready we then insert them into the SQL database
		if(HourPredictionsSinceLastInsert >= 1000) {
			HourPredictionsSinceLastInsert = 0;
			
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
	            
	            //tempMasterHourPredictionIndex is a copy of masterHourPredictionIndex so that we can update
	            //the number of predictions we have already added to the database
	            
	            int tempMasterHourPredictionIndex = 0;
	            // We have to create a statement for each line in the zip file
	            for(int x = 0; x < masterHourPrediction.size() - masterHourPredictionIndex; x++) {
		            HourPrediction currentHourPrediction = masterHourPrediction.get((masterHourPredictionIndex + x));
		            
		            tempMasterHourPredictionIndex++;
		            
		            /*
		             * For debugging
		             * currentHourPrediction.printWeather();
		             */
		            
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
		            
		            //For debugging
		            //System.out.println(x + " masterHourPrediction: " + masterHourPrediction.size() + " masterHourPredictionIndex: " + masterHourPredictionIndex);
		            
	            }
	            
	            //Update master index with how many predictions were just inserted
	            masterHourPredictionIndex = masterHourPredictionIndex + tempMasterHourPredictionIndex;
	            
	            // Execute all batches added in the previous for loop
	            pst.executeBatch();
	            
	        } catch (SQLException ex) {
	            Logger lgr = Logger.getLogger(populateLatLon.class.getName());
	            lgr.log(Level.SEVERE, ex.getMessage(), ex);
	            System.out.println("What is going on here?");

	        } catch (Exception ex) {
	        	System.out.println("What is going on? " + ex);
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
		else {
			HourPredictionsSinceLastInsert += fromParse.length;
		}
	}
	
	/*
	 * Method parse
	 * 
	 * Purpose: The master method for dissecting and sorting the xml document from each zip code.
	 * parse returns the completed HourPrediction object array for the current zip code.
	 */
	
	private HourPrediction[] parse(String[] parseThis) throws Exception {
		
		// Initialize the HourPrediction object array
		HourPrediction[] prediction = null;
		String[] toParse = parseThis;
		
		// Pull the xml document out of the toParse string array
		String xml = toParse[0];
		
		// Pulled lines strips the tag portions of the xml document
		ArrayList<String> pulledLines = getPulledLines(parseThis);
		
		// sortLines returns two catagories of data, either a line is a timeStamp or a weather prediction piece
		ArrayList<ArrayList> combinedTimeAndWeather = sortLines(pulledLines);
		
		// combinedTimeAndWeather contains the time stamp data in index 1 and the weather data in index 2
		ArrayList<ArrayList> dataTimeStamps = combinedTimeAndWeather.get(0);
		ArrayList<ArrayList> dataWeather = combinedTimeAndWeather.get(1);
		
		// Get the master time stamp from the matching temperature data
		ArrayList<String> masterTimeStamp = getMasterTimeStamp(dataTimeStamps, dataWeather);
		
		/*
		 *  setPredictionTemperature creates each HourPrediction in the previous null HourPrediction array.
		 *  the temperature parameter uses the masterTimeStamp to assign its applicable time which each
		 *  of the following parameters assign an offset to the master time.
		 */
		
		prediction = setPredictionTemperature(prediction, toParse, dataTimeStamps, dataWeather, masterTimeStamp);
		
		// The following 6 methods set their parameters and return the new prediction HourPrediction object array
		prediction = setPredictionLiquidPrecipitation(prediction, dataTimeStamps, dataWeather, masterTimeStamp);
		prediction = setPredictionWindSpeed(prediction, dataTimeStamps, dataWeather, masterTimeStamp);
		prediction = setPredictionIceAccum(prediction, dataTimeStamps, dataWeather, masterTimeStamp);
		prediction = setPredictionSnowAccum(prediction, dataTimeStamps, dataWeather, masterTimeStamp);
		prediction = setPredictionWindGust(prediction, dataTimeStamps, dataWeather, masterTimeStamp);
		prediction = setPredictionHumidity(prediction, dataTimeStamps, dataWeather, masterTimeStamp);
		
		return prediction;
	}
	
	private ArrayList<String> getPulledLines(String[] parseThisString) {
		//First get the document toParse which is in XML, zip, coordinate format
		String[] toParse = parseThisString;
		String xml = toParse[0];
		ArrayList<String> pulledLines = new ArrayList();
		 
		
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
		while(indexForRightPost != endOfDocument) {
			
			//Whenever there is space between 2 tags we pull that line to add to our arraylist of strings
			if(indexForRightPre != (indexForLeftPost - 1)) {
				toAddToPulledLines = xml.substring(indexForRightPre + 1, indexForLeftPost);
				
				if(toAddToPulledLines.trim().isEmpty() != true) {
					//System.out.println(toAddToPulledLines);
					
					//Check if previous tag is <end-valid-time>
					if(xml.substring(indexForLeftPre, indexForRightPre).contains("end") != true) {
						pulledLines.add(toAddToPulledLines);
					}
				}

				// This for loop searches for keywords that we need to pull the previous tag on.
				for(int y = 0; y < keywords.length; y++) {
					
					if((keywords[y].equalsIgnoreCase(toAddToPulledLines)) == true) {
						//System.out.println(keywords[y].equals(toAddToPulledLines));
						//System.out.println(toAddToPulledLines + " " + keywords[y]);
						//System.out.println(xml.substring(nextNextOldIndexForLeftPostTemp, nextNextOldIndexForRightPostTemp + 1));
						pulledLines.add(xml.substring(nextNextOldIndexForLeftPostTemp, nextNextOldIndexForRightPostTemp));
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
		
		return pulledLines;
	}

	
	private ArrayList<ArrayList> sortLines(ArrayList<String> pulledLines) {
		
		int pulledLinesIndex = 0;
		int pulledLinesSize = pulledLines.size();
		ArrayList<ArrayList> dataTimeStamps = new ArrayList();
		ArrayList<ArrayList> dataWeather = new ArrayList();
		boolean isPassed = false;
		
		//This while loop creates two arrayList containing arrayLists of Strings
		//dataTimeStamps contain the arraylists with the time stamp key in the first string of the arraylist and all the relevant timestamps that follow
		//dataWeather is similar with the first string being the name of the data contained, the second string being the timestamp key
		// and all the relevant data we are trying to collect follows.
		while(pulledLinesIndex < pulledLinesSize) {
			String toSort = pulledLines.get(pulledLinesIndex);
			pulledLinesIndex++;
			//System.out.println(toSort);
			// Test if this is a timestamp key
			//first if statement is for error checking
			
			if(toSort.length() > 2) {
				// This if line tests to see if the block of data is one of the keywords types that we are looking for
				if((toSort.equalsIgnoreCase(keywords[0]) == true) || (toSort.equalsIgnoreCase(keywords[1]) == true) || 
					(toSort.equalsIgnoreCase(keywords[2]) == true) || (toSort.equalsIgnoreCase(keywords[3]) == true) || 
					(toSort.equalsIgnoreCase(keywords[4]) == true) || (toSort.equalsIgnoreCase(keywords[5]) == true) || 
					(toSort.equalsIgnoreCase(keywords[6]) == true)) {
					
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
					while(toSort.length() < 8) {
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
				else if((toSort.charAt(0) == 'k') && (toSort.charAt(1) == '-') && isPassed == false) {
					//Add timestamp key and then following timestamps to a String arraylist
					ArrayList<String> timeStampTemp = new ArrayList();
					
					//Temp arraylist to be added after the while loop completes
					timeStampTemp.add(toSort);
					//System.out.println(toSort); // for debugging
					toSort = pulledLines.get(pulledLinesIndex);
					pulledLinesIndex++;
					//While loop goes through each of the timestamps and adds them to the temporary arraylist
					while(((toSort.charAt(0) != 'k') && (toSort.charAt(1) != '-'))
							&& (toSort.equalsIgnoreCase("Daily Maximum Temperature") == false)) {
						timeStampTemp.add(toSort);
						//System.out.println(toSort);
						toSort = pulledLines.get(pulledLinesIndex);
						pulledLinesIndex++;
					}
					pulledLinesIndex--;
					// the temporary arraylist is added to the arraylist of arraylist dataTimeStamps
					dataTimeStamps.add(timeStampTemp);
				}	
			}
		}
		
		ArrayList<ArrayList> toReturnCombinedLists = new ArrayList();
		toReturnCombinedLists.add(dataTimeStamps);
		toReturnCombinedLists.add(dataWeather);
		return toReturnCombinedLists;
	}
	
	private ArrayList<String> getMasterTimeStamp(ArrayList<ArrayList> dataTimeStamps, ArrayList<ArrayList> dataWeather) {
		//tempParameter holds one of the data types that we are looking to store in the case of dataWeather.get(0) it is temperature
		ArrayList tempParameter = dataWeather.get(0);
		
		//parameterName should just be "Temperature"
		String parameterName = (String) tempParameter.get(0);
		
		//dataTimeKey is the timestamp key that pertains to the parameter temperature
		String dataTimeKey = (String) tempParameter.get(1);
		//System.out.println(parameterName + " " + parameterKey);
		

		// pulledFromDataTimeStamps is the first arraylist of timestamps stored in dataTimeStamps arraylist of arraylists
		// its initialized here for reasons but later the same data is initialized in the for loop
		ArrayList pulledFromDataTimeStamps = dataTimeStamps.get(0);
		//System.out.println(dataTimeStamps.size());
		
		// relevantTimeData is the index of the arrayList containing the timeStamps inside dataTimeStamps
		int relevantTimeData = 0;
		
		// goal of this for loop is to find the index of the matching timestamp keys between the arraylists of timestamps and
		// the parameters stored key
		// System.out.println("datatimestamps.size(): " + dataTimeStamps.size());
		
		for(int b = 0; b < dataTimeStamps.size(); b++) {
			pulledFromDataTimeStamps = dataTimeStamps.get(b);
			String timeStampTest = (String) pulledFromDataTimeStamps.get(0);
			// System.out.println(dataTimeKey + " " + timeStampTest);
			
			if(timeStampTest.equals(dataTimeKey)) {
				// System.out.println(dataTimeKey + " " + timeStampTest + " " + "success" + " " + parameterName);
				relevantTimeData = b;
				break;
				
			}
		}
		// System.out.println("relevantTimeData: " + relevantTimeData);
		
		ArrayList masterTimeStamp = dataTimeStamps.get(relevantTimeData);
		
		return masterTimeStamp;
	}
	
	private HourPrediction[] setPredictionTemperature(HourPrediction[] prediction, String[] toParse, ArrayList<ArrayList> dataTimeStamps, ArrayList<ArrayList> dataWeather, ArrayList<String> masterTimeStamp) {
		
		/* 
		 * Now we need to construct objects for each hour to be added to the database
		 * 64 or less hours are set for each parsed document
		*/
		
		// tempParameter holds one of the data types that we are looking to store in the case of dataWeather.get(0) it is temperature
		ArrayList tempParameter = dataWeather.get(0);
		
		// prediction is an array of objects that we are attempting to sort all of the parameters into for each hours prediction
		prediction = new HourPrediction[tempParameter.size()-2];
		
		// The follow sets temperature
		for(int z = 2; z < tempParameter.size(); z++) {
			// System.out.println(tempParameter.get(z));
			// System.out.println(masterTimeStamp.get(z-1));
			String predictionTimeStamp = (String) masterTimeStamp.get(z-1);
			prediction[z-2] = new HourPrediction(toParse[1], predictionTimeStamp);
			prediction[z-2].setTemperature((String) tempParameter.get(z)); 
		}
		
		return prediction;
		
	}
	
	private HourPrediction[] setPredictionLiquidPrecipitation(HourPrediction[] predictionArg, ArrayList<ArrayList> dataTimeStamps, ArrayList<ArrayList> dataWeather, ArrayList<String> masterTimeStamp) {
		
		HourPrediction[] prediction = predictionArg;
		
		//The following sets liquid precipitation amount
		ArrayList tempParameter = dataWeather.get(1);
		String parameterName = (String) tempParameter.get(0);
		String dataTimeKey = (String) tempParameter.get(1);
		ArrayList pulledFromDataTimeStamps = dataTimeStamps.get(0);
		//System.out.println(dataTimeStamps.size());
		int relevantTimeData = 0;
		
		for(int b = 0; b < dataTimeStamps.size(); b++) {
			pulledFromDataTimeStamps = dataTimeStamps.get(b);
			String timeStampTest = (String) pulledFromDataTimeStamps.get(0);
			//System.out.println(dataTimeKey + " " + timeStampTest);
			
			if(timeStampTest.equals(dataTimeKey)) {
				//System.out.println(dataTimeKey + " " + timeStampTest + " " + "success");
				relevantTimeData = b;
				break;
			}
		}
		// relevantTimeData is the index of the arrayList containing the timeStamps inside dataTimeStamps

		// The offset of the current timestamp arraylist that we are looking at against the masterTimeStamps arraylist
		// that we used to create the objects with
		int offSetFromMaster = 0;
		
		for(int a = 1; a < pulledFromDataTimeStamps.size(); a++) {
			//System.out.println(pulledFromDataTimeStamps.get(a) + " " + masterTimeStamp.get(1));
			if(pulledFromDataTimeStamps.get(a).equals(masterTimeStamp.get(1))) {
				offSetFromMaster = a - 1;
				//System.out.println("Sucess " + offSetFromMaster + " " + pulledFromDataTimeStamps.get(a) + " " + parameterName);
				break;
			}
		}
		
		//Now that we know the offset we can set precipitation amount
		for(int c = offSetFromMaster + 2; c < tempParameter.size(); c++) {
			prediction[c - 2 - offSetFromMaster].setPrecip((String) tempParameter.get(c)); 
			//System.out.println((String) tempParameter.get(c));
			//System.out.println(c);
			//prediction[c - 2 - offSetFromMaster].printWeather();
		}
		
		return prediction;
	}
	
	private HourPrediction[] setPredictionWindSpeed(HourPrediction[] predictionArg, ArrayList<ArrayList> dataTimeStamps, ArrayList<ArrayList> dataWeather, ArrayList<String> masterTimeStamp) {
		HourPrediction[] prediction = predictionArg;
		
		//The following sets wind speed
		ArrayList tempParameter = dataWeather.get(2);
		String parameterName = (String) tempParameter.get(0);
		String dataTimeKey = (String) tempParameter.get(1);
		ArrayList pulledFromDataTimeStamps = dataTimeStamps.get(0);
		//System.out.println(dataTimeStamps.size());
		int relevantTimeData = 0;
		
		for(int b = 0; b < dataTimeStamps.size(); b++) {
			pulledFromDataTimeStamps = dataTimeStamps.get(b);
			String timeStampTest = (String) pulledFromDataTimeStamps.get(0);
			//System.out.println(dataTimeKey + " " + timeStampTest);
			
			if(timeStampTest.equals(dataTimeKey)) {
				//System.out.println(dataTimeKey + " " + timeStampTest + " " + "success");
				relevantTimeData = b;
				break;
			}
		}
		// relevantTimeData is the index of the arrayList containing the timeStamps inside dataTimeStamps

		// The offset of the current timestamp arraylist that we are looking at against the masterTimeStamps arraylist
		// that we used to create the objects with
		int offSetFromMaster = 0;
		
		for(int a = 1; a < pulledFromDataTimeStamps.size(); a++) {
			if(pulledFromDataTimeStamps.get(a).equals(masterTimeStamp.get(a))) {
				offSetFromMaster = a - 1;
				//System.out.println("Sucess " + offSetFromMaster + " " + pulledFromDataTimeStamps.get(a) + " " + parameterName);
				break;
			}
		}
		
		//Now that we know the offset we can set wind amount 
		for(int c = offSetFromMaster + 2; c < tempParameter.size(); c++) {
			prediction[c - 2 - offSetFromMaster].setWindSpeed((String) tempParameter.get(c)); 
		}
		
		return prediction;
	}
	
	private HourPrediction[] setPredictionIceAccum(HourPrediction[] predictionArg, ArrayList<ArrayList> dataTimeStamps, ArrayList<ArrayList> dataWeather, ArrayList<String> masterTimeStamp) {
		HourPrediction[] prediction = predictionArg;
		
		//The following sets Ice accumulation
		ArrayList tempParameter = dataWeather.get(3);
		String parameterName = (String) tempParameter.get(0);
		String dataTimeKey = (String) tempParameter.get(1);
		ArrayList pulledFromDataTimeStamps = dataTimeStamps.get(0);
		//System.out.println(dataTimeStamps.size());
		int relevantTimeData = 0;
		
		for(int b = 0; b < dataTimeStamps.size(); b++) {
			pulledFromDataTimeStamps = dataTimeStamps.get(b);
			String timeStampTest = (String) pulledFromDataTimeStamps.get(0);
			//System.out.println(dataTimeKey + " " + timeStampTest);
			
			if(timeStampTest.equals(dataTimeKey)) {
				//System.out.println(dataTimeKey + " " + timeStampTest + " " + "success");
				relevantTimeData = b;
				break;
			}
		}
		// relevantTimeData is the index of the arrayList containing the timeStamps inside dataTimeStamps

		// The offset of the current timestamp arraylist that we are looking at against the masterTimeStamps arraylist
		// that we used to create the objects with
		int offSetFromMaster = 0;
		
		for(int a = 1; a < pulledFromDataTimeStamps.size(); a++) {
			if(pulledFromDataTimeStamps.get(a).equals(masterTimeStamp.get(a))) {
				offSetFromMaster = a - 1;
				System.out.println("Sucess " + offSetFromMaster + " " + pulledFromDataTimeStamps.get(a) + " " + parameterName);
				break;
			}
		}
		
		//Now that we know the offset we can set wind amount 
		int overallIceCount = 0;
		for(int c = offSetFromMaster + 2; c < tempParameter.size(); c++) {
			
			for(int d = 0; d < 6; d++) {
				
				if(overallIceCount >= prediction.length) {
					break;
				}
				
				prediction[(c-2)*6 + d - offSetFromMaster].setIce((String) tempParameter.get(c)); 
				overallIceCount++;
			}
		}
		return prediction;
	}
	
	private HourPrediction[] setPredictionSnowAccum(HourPrediction[] predictionArg, ArrayList<ArrayList> dataTimeStamps, ArrayList<ArrayList> dataWeather, ArrayList<String> masterTimeStamp) {
		HourPrediction[] prediction = predictionArg;
		
		//The following sets snow amount
		ArrayList tempParameter = dataWeather.get(4);
		String parameterName = (String) tempParameter.get(0);
		String dataTimeKey = (String) tempParameter.get(1);
		ArrayList pulledFromDataTimeStamps = dataTimeStamps.get(0);
		//System.out.println(dataTimeStamps.size());
		int relevantTimeData = 0;
		for(int b = 0; b < dataTimeStamps.size(); b++) {
			pulledFromDataTimeStamps = dataTimeStamps.get(b);
			String timeStampTest = (String) pulledFromDataTimeStamps.get(0);
			//System.out.println(dataTimeKey + " " + timeStampTest);
			
			if(timeStampTest.equals(dataTimeKey)) {
				//System.out.println(dataTimeKey + " " + timeStampTest + " " + "success");
				relevantTimeData = b;
				break;
			}
		}
		// relevantTimeData is the index of the arrayList containing the timeStamps inside dataTimeStamps

		// The offset of the current timestamp arraylist that we are looking at against the masterTimeStamps arraylist
		// that we used to create the objects with
		int offSetFromMaster = 0;
		for(int a = 1; a < pulledFromDataTimeStamps.size(); a++) {
			if(pulledFromDataTimeStamps.get(a).equals(masterTimeStamp.get(a))) {
				offSetFromMaster = a - 1;
				//System.out.println("Sucess " + offSetFromMaster + " " + pulledFromDataTimeStamps.get(a) + " " + parameterName);
				break;
			}
		}
		
		//Now that we know the offset we can set wind amount 
		int overallSnowCount = 0;
		for(int c = offSetFromMaster + 2; c < tempParameter.size(); c++) {
			for(int d = 0; d < 6; d++) {
				if(overallSnowCount >= prediction.length) {
					break;
				}
				prediction[(c-2)*6 + d - offSetFromMaster].setSnow((String) tempParameter.get(c)); 
				overallSnowCount++;
			}
		}
		
		return prediction;
	}
	
	private HourPrediction[] setPredictionWindGust(HourPrediction[] predictionArg, ArrayList<ArrayList> dataTimeStamps, ArrayList<ArrayList> dataWeather, ArrayList<String> masterTimeStamp) {
		HourPrediction[] prediction = predictionArg;
		//The following sets Wind Gust Speed
		ArrayList tempParameter = dataWeather.get(5);
		String parameterName = (String) tempParameter.get(0);
		String dataTimeKey = (String) tempParameter.get(1);
		ArrayList pulledFromDataTimeStamps = dataTimeStamps.get(0);
		//System.out.println(dataTimeStamps.size());
		int relevantTimeData = 0;
		
		for(int b = 0; b < dataTimeStamps.size(); b++) {
			pulledFromDataTimeStamps = dataTimeStamps.get(b);
			String timeStampTest = (String) pulledFromDataTimeStamps.get(0);
			//System.out.println(dataTimeKey + " " + timeStampTest);
			if(timeStampTest.equals(dataTimeKey)) {
				//System.out.println(dataTimeKey + " " + timeStampTest + " " + "success");
				relevantTimeData = b;
				break;
			}
		}
		// relevantTimeData is the index of the arrayList containing the timeStamps inside dataTimeStamps

		// The offset of the current timestamp arraylist that we are looking at against the masterTimeStamps arraylist
		// that we used to create the objects with
		int offSetFromMaster = 0;
		
		for(int a = 1; a < pulledFromDataTimeStamps.size(); a++) {
			if(pulledFromDataTimeStamps.get(a).equals(masterTimeStamp.get(a))) {
				offSetFromMaster = a - 1;
				//System.out.println("Sucess " + offSetFromMaster + " " + pulledFromDataTimeStamps.get(a) + " " + parameterName);
				break;
			}
		}
		
		//Now that we know the offset we can set wind amount 
		int overallGustCount = 0;
		for(int c = offSetFromMaster + 2; c < tempParameter.size(); c++) {
			for(int d = 0; d < 3; d++) {
				if(overallGustCount >= prediction.length) {
					break;
				}
				prediction[(c-2)*3 + d - offSetFromMaster].setGust((String) tempParameter.get(c)); 
				overallGustCount++;
			}
		}
		return prediction;
	}
	
	private HourPrediction[] setPredictionHumidity(HourPrediction[] predictionArg, ArrayList<ArrayList> dataTimeStamps, ArrayList<ArrayList> dataWeather, ArrayList<String> masterTimeStamp) {
		HourPrediction[] prediction = predictionArg;
		//The following sets Humidity
		ArrayList tempParameter = dataWeather.get(6);
		String parameterName = (String) tempParameter.get(0);
		String dataTimeKey = (String) tempParameter.get(1);
		ArrayList pulledFromDataTimeStamps = dataTimeStamps.get(0);
		//System.out.println(dataTimeStamps.size());
		int relevantTimeData = 0;
		
		for(int b = 0; b < dataTimeStamps.size(); b++) {
			pulledFromDataTimeStamps = dataTimeStamps.get(b);
			String timeStampTest = (String) pulledFromDataTimeStamps.get(0);
			//System.out.println(dataTimeKey + " " + timeStampTest);
			if(timeStampTest.equals(dataTimeKey)) {
				//System.out.println(dataTimeKey + " " + timeStampTest + " " + "success");
				relevantTimeData = b;
				break;
			}
		}
		// relevantTimeData is the index of the arrayList containing the timeStamps inside dataTimeStamps

		// The offset of the current timestamp arraylist that we are looking at against the masterTimeStamps arraylist
		// that we used to create the objects with
		int offSetFromMaster = 0;
		for(int a = 1; a < pulledFromDataTimeStamps.size(); a++) {
			if(pulledFromDataTimeStamps.get(a).equals(masterTimeStamp.get(a))) {
				offSetFromMaster = a - 1;
				//System.out.println("Sucess " + offSetFromMaster + " " + pulledFromDataTimeStamps.get(a) + " " + parameterName);
				break;
			}
		}
		
		//Now that we know the offset we can set wind amount 
		int overallHumCount = 0;
		for(int c = offSetFromMaster + 2; c < tempParameter.size(); c++) {
			for(int d = 0; d < 3; d++) {
				if(overallHumCount >= prediction.length) {
					break;
				}
				prediction[(c-2)*3 + d - offSetFromMaster].setHumidity((String) tempParameter.get(c)); 
				overallHumCount++;
			}
		}
		return prediction;
	}
	

}