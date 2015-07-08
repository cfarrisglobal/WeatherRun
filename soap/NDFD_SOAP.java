/*
 * Created by Charles Cody Farris 6/10/15
 * 
 * Last updated 7/7/15
 * 
 * Programs purpose is to populate a database with the National Weather Service's weather prediction data.
 * 
 * This is done by using SOAP requests to the NDFD database and then sorting and inserting the relevant data.
 * 
 */


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

public class NDFD_SOAP {
	
	public static void main(String[] args) throws Exception {
		
		//Put all valid zip codes stored in txt file into arraylist
		ArrayList<String[]> Zips = new ArrayList();
		Zips = getZips();
		
		
		/*
		 * Depending on what system this is running on you may want to increase or decrease the number of threads
		 * that use the SOAP requests on the NDFD webservice. Each request can take between .5 and 5 seconds
		 */
		
		WeatherRequest[] threadArray = new WeatherRequest[2];
		for(int x = 0; x < threadArray.length; x++) {
			threadArray[x] = new WeatherRequest(Integer.toString(x), Zips);
			threadArray[x].start();
			// Here we wait a small amount of time between starting each thread so that we do not bombard the NDFD servers all at once
			try {
				TimeUnit.MILLISECONDS.sleep(50);
			} catch (InterruptedException e) {
				// Catch the interrupt
				e.printStackTrace();
			}
		}
	}
	
	public static ArrayList getZips() {
		
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
		
		catch(FileNotFoundException ex) {
			System.out.println("Unable to open file '" + zipCodeFile + "'");
		}
		
		catch(IOException ex) {
			System.out.println("Error reading file '" + zipCodeFile + "'");
			ex.printStackTrace();
		}
		
		return allZips;
	}
}