package soap;


import javax.xml.soap.*;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.ws.Dispatch;
import javax.xml.ws.Service;
import javax.xml.ws.soap.SOAPFaultException;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;




import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import java.io.*;
import java.net.URL;
import java.util.ArrayList;
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
		//The following builds an xml document out of a string
		String[] toParse = getXMLDoc();
		DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
        InputSource is = new InputSource(new StringReader(toParse[0]));
        Document doc = docBuilder.parse(is);
		
        // Here we get the root element of the document to confirm that is has been built correctly
        doc.getDocumentElement().normalize();
        System.out.println("Root element of the doc is " + doc.getDocumentElement().getNodeName());
        
        // NodeLists holds all of the nodes in the xml document that are tagged with their respective strings
        NodeList listOfHours = doc.getElementsByTagName("start-valid-time");
        NodeList layoutKeys = doc.getElementsByTagName("layout-key");
        //The two following arraylists contain information of the timestamp keys
        //The timestamp keys contain how many timestamps follow and what their interval is
        ArrayList<Integer> keyLengths = new ArrayList();
        ArrayList<String> keys = new ArrayList();
        
        //The following is a workaround in order to get the xml back to a relevant string so that we can work with it easier
        StringWriter writer = new StringWriter();
    	Transformer transformer = TransformerFactory.newInstance().newTransformer();
        
    	//The following for loop takes the keynodes and extracts the amount of timestampts they are assigned to
    	//It also stores the keys into string format so that we can later pull them and compare them to the data they are assigned to
    	for(int x = 0; x < layoutKeys.getLength(); x++)
        {
        	
        	transformer.transform(new DOMSource(layoutKeys.item(x)), new StreamResult(writer));
        	String temp = writer.toString();
        	String trimmedTemp = trimXML(temp, "layout-key");
        	//System.out.println(trimmedTemp);
        	String[] getThisInt = trimmedTemp.split("n");
        	String[] getThisInt2 = getThisInt[1].split("-");
        	int lengthInKey = Integer.parseInt(getThisInt2[0]);
        	keyLengths.add(lengthInKey);
        	keys.add(trimmedTemp);
        }
        
        ArrayList<ArrayList<String>> validTimes = new ArrayList();
        
        
        // The following for loop populates an arraylist of arraylists which contain the relevant time key in the first variable
        // and then all of the assigned times related to the time stamp follow. This iterates on the container arraylist for each of 
        //the time keys
        // I also understand that makes no sense and that I really need to go back over my comments once I'm done with this program.
        int countOverallTimesPassed = 0;
        for(int y = 0; y < layoutKeys.getLength(); y++)
        {
        	ArrayList<String> temp = new ArrayList();
        	temp.add(keys.get(y));
        	for(int z = 0; z < keyLengths.get(y); z++)
        	{
        		transformer.transform(new DOMSource(listOfHours.item(countOverallTimesPassed)), new StreamResult(writer));
        		String fromTransformer = writer.toString();
        		String getFormat = trimXML(fromTransformer, "start-valid-time");
        		System.out.println(fromTransformer + " " + countOverallTimesPassed);
        		temp.add(getFormat);
        		System.out.println(getFormat);
        		countOverallTimesPassed++;
        	}
        	validTimes.add(temp);
        }
	}
	
	// helper method to extract relevant data from string derived from a xml node
	private String trimXML(String toTrim, String keyword)
	{
		
		String[] temp = toTrim.split("<" + keyword + ">");
		String[] toReturn = temp[1].split("<");
		return toReturn[0];
	}
}
