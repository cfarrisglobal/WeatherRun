package soap;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.sql.*;

public class populateLatLon {

	public static void main(String[] args)
	{
		ArrayList toInsert = getZips();
		insertZips(toInsert);
		
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
					String latAndLon[] = zipAndRest[1].split(",");
					//System.out.println(zipAndRest[0] + " " + latAndLon[0] + " " + latAndLon[1]);
					String[] allSeperated = new String[]{zipAndRest[0], latAndLon[0], latAndLon[1]};
					allZips.add(allSeperated);
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
	
	public static void insertZips(ArrayList Zips)
	{
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
             * Using preparedStatement so that we don't have to worry about sql injection
             * If a row already exists we leave it alone since this information is static
             * If there is a change to a zipcodes coordinates this may have to change or we can just flush the table
             * and run the program again
            */
            PreparedStatement pst = con.prepareStatement("INSERT INTO latLon(zipcode, lattitude, longitude) VALUES(?, ?, ?) "
            		+ "ON DUPLICATE KEY UPDATE zipcode = zipcode, lattitude = lattitude, longitude = longitude;");
            
            // We have to create a statement for each line in the zip file
            for(int x = 0; x < Zips.size(); x++)
            {
	            String[] toInsert = (String[]) Zips.get(x);
	            
	            // setstring sets the ? values in the preparedStatement with the data from the zips arraylist
	            pst.setString(1, toInsert[0]);
	            pst.setString(2, toInsert[1]);
	            pst.setString(3, toInsert[2]);
	            
	            // we add each statement to a batch that is later executed
	            pst.addBatch();
	            
	            // System.out.println(x);
	            
	            // This if statement executes the batch every 1000 lines or at the end of the arraylist zips.
	            if( x % 1000 == 0 || x == (Zips.size() - 1))
	            {
		            pst.executeBatch();
		            System.out.println("Executed " + x + " rows");
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
}
