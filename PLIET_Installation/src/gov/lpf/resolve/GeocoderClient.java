/*
 * /*
 * Informational Notice:
 * This software was developed under contract funded by the National Library of Medicine, which is part of the National Institutes of Health, 
 * an agency of the Department of Health and Human Services, United States Government.
 *
 * The license of this software is an open-source BSD license.  It allows use in both commercial and non-commercial products.
 *
 * The license does not supersede any applicable United States law.
 *
 * The license does not indemnify you from any claims brought by third parties whose proprietary rights may be infringed by your usage of this software.
 *
 * Government usage rights for this software are established by Federal law, which includes, but may not be limited to, Federal Acquisition Regulation 
 * (FAR) 48 C.F.R. Part52.227-14, Rights in Dataï¿½General.
 * The license for this software is intended to be expansive, rather than restrictive, in encouraging the use of this software in both commercial and 
 * non-commercial products.
 *
 * LICENSE:
 *
 * Government Usage Rights Notice:  The U.S. Government retains unlimited, royalty-free usage rights to this software, but not ownership,
 * as provided by Federal law.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 * -	Redistributions of source code must retain the above Government Usage Rights Notice, this list of conditions and the following disclaimer.
 *
 * -	Redistributions in binary form must reproduce the above Government Usage Rights Notice, this list of conditions and the following disclaimer 
 * in the documentation and/or other materials provided with the distribution.
 *
 * -	The names,trademarks, and service marks of the National Library of Medicine, the National Cancer Institute, the National Institutes 
 * of Health,  and the names of any of the software developers shall not be used to endorse or promote products derived from this software without 
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE U.S. GOVERNMENT AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, 
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE U.S. GOVERNMENT
 * OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, 
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, 
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, 
 * EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package gov.lpf.resolve;

/* Author: 
 *    Change Log ...
 *      Changed name from original LPFNamedEntityUtil
 *       Made other updates and better structure, with bean methods
 *      Updates to fet address based on lat/long
*/

import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

import org.apache.log4j.Logger;

public class GeocoderClient
{
    private static Logger log = Logger.getLogger(GeocoderClient.class);
    
    private static final String GEOCODER_REQUEST_PREFIX_FOR_XML =
      // "http://maps.google.com/maps/api/geocode/xml";
       "http://maps.googleapis.com/maps/api/geocode/xml";
        
    private static final String GEOCODER_RESULT_XPATH =
        "/GeocodeResponse[status/text()='OK']/result "
        + "[type/text()='street_address' or type/text()='point_of_interest' "
        + "or type/text()='locality' "
        + "or type/text()='sublocality' "
        + "or type/text()='country' "
        + "or type/text()='administrative_area_level_1' "
        + "or type/text()='administrative_area_level_2' "
        + "or type/text()='administrative_area_level_3' "
        + "]";
    


    /********************************************************************************/
    private GeoLocationInfo glInfo;         // for the current Geo location address
    
    public GeocoderClient()
    {
    }
    /********************************************************************************
    // Validate the given location, specified as address, through Google Geocoder Service
     * *****************************************************************************/
    public boolean validateLocation(String address)
    {
        System.out.println(">> Address: " +address);
        glInfo = new GeoLocationInfo(address);     // initialize
        glInfo.valid = false;
        
          try
         {
            // prepare a URL to the geocoder
            URL url = new URL(GEOCODER_REQUEST_PREFIX_FOR_XML +
                "?address=" + URLEncoder.encode(address, "UTF-8") + "&sensor=false");
            System.out.println(url.toString());
            // query for address, based on lat/long
            return queryForGeocodeInfo(url);
         }
         catch (Exception e)
         {
            //log.error("Error encoding params Geocoder query: " + address);
              System.out.println("Error encoding params Geocoder query: " +address);
            return false;
        }
    }

    /*-----------------------------------------------------------------------------------------------------
     * Get the location information based upon  specified lat/long, specified as 
     * xml?latlng=nn.nn,yy.yy&sensor=true_or_false
     *------------------------------------------------------------------------------------------------------*/
    
     public boolean validateLatLong(String lat, String lng)
    {
       // Example: latlng=40.714224,-73.961452&sensor=true_or_false
        
        System.out.println(">> Latitude: " + lat + ", Longitude: " + lng);
        glInfo = new GeoLocationInfo("");     // initialize
        glInfo.valid = false;

        float flat = Double.valueOf(lat).floatValue(); 
        float flong =  Double.valueOf(lng).floatValue();
        if (flat < -90.0 || flat > 90.0 || flong < -180.0 || flong > 180.0)
            return false;               // invalid input data
         
        String latlng = lat + "," +lng;
         try
         {
            // prepare a URL to the geocoder
             URL url = new URL(GEOCODER_REQUEST_PREFIX_FOR_XML +
                    "?latlng=" + URLEncoder.encode(latlng, "UTF-8") + "&sensor=false");
            // query for address, based on lat/long
            return queryForGeocodeInfo(url);
         }
         catch (Exception e)
         {
            //log.error("Error encoding params Geocoder query: " + latlng);
             System.out.println("Error encoding params Geocoder query: " + latlng);
            return false;
        }
    }
     /*--------------------------------------------------------------------------------------------------------------------
      * Query for a geocode address info or a reverse geocode address based on lat/long
      *--------------------------------------------------------------------------------------------------------------------*/
     protected boolean queryForGeocodeInfo(URL url)
     {
        Document geocoderResultDocument = null;
        try
        { 
            // prepare an HTTP connection to the geocoder
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            try
            {
                // open the connection and get results as InputSource.
                conn.connect();
                InputSource geocoderResultInputSource = new InputSource(conn.getInputStream());

                // read result and parse into XML Document
                geocoderResultDocument = 
                    DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(geocoderResultInputSource);
            } 
            finally
            {
                conn.disconnect();
            }
        } 
        catch (Exception e)
        {
            log.error("Error querying for  Geocoder result with URL: " +url);
            return false;
        }
        
        // Now format the result
        boolean isValid = formatGeocodeResult(url, geocoderResultDocument);
        return isValid;
    }
     
   /**************************************************************************************************/
   // Format the result returned through geodode or reverse geocode service
   /**************************************************************************************************/
    protected boolean  formatGeocodeResult (URL url, Document geocoderResultDocument)
    {
        try
        {
            // prepare XPath
            XPath xpath = XPathFactory.newInstance().newXPath();

            // extract the result
            NodeList resultNodeList = null;
            resultNodeList = (NodeList) xpath.evaluate(GEOCODER_RESULT_XPATH, 
                        geocoderResultDocument, XPathConstants.NODESET);
            if (resultNodeList == null ||  resultNodeList.getLength() ==  0)
                return  false;          // not a valid location
            else
            {
                NodeList nl = (NodeList) xpath.evaluate("formatted_address", resultNodeList.item(0), XPathConstants.NODESET);
                if (nl == null || nl.getLength() == 0)
                    return false;
            
                // A valid Geo-location. Extract all fields from the returned info and stored in the data structure
                glInfo.valid = true;
                glInfo.location = nl.item(0).getTextContent();
               //System.out.println( glInfo.location);

                glInfo.lat = (String) xpath.evaluate("geometry/location/lat/text()", resultNodeList.item(0), XPathConstants.STRING);
                glInfo.lng = (String) xpath.evaluate("geometry/location/lng/text()", resultNodeList.item(0), XPathConstants.STRING);

                NodeList typel = (NodeList) xpath.evaluate("address_component", resultNodeList.item(0), XPathConstants.NODESET);
                String level3  = "";
                String level2 = "";
                 if (typel != null && typel.getLength() > 0)
                 {
                     for (int i = 0; i < typel.getLength(); i++)
                     {
                         Node node =  typel.item(i);
                         String type = (String) xpath.evaluate("type/text()", node, XPathConstants.STRING);
                         if (type.equals("locality"))
                              glInfo.locality = (String) xpath.evaluate("long_name/text()", node, XPathConstants.STRING);
                         else if (type.equals("country"))
                             glInfo.country=(String) xpath.evaluate("long_name/text()", node, XPathConstants.STRING);
                         else if (type.equals("administrative_area_level_1"))                     
                             glInfo.region = (String) xpath.evaluate("long_name/text()", node, XPathConstants.STRING);

                     }
                     if (glInfo.location != null)
                     {
                         String[] parts = glInfo.location.split(",\\W*");
                         if (parts.length > 0)
                            glInfo.city = parts[0];     
                     }
                 }
               System.out.println("Geovalidation  location: " +  glInfo.location + ", locality: " +  glInfo.locality + ", country: " +  glInfo.country);
            }
        } catch (Exception e)
        {
            log.error("Error parsing Geocoder result for URL : " + url);
            e.printStackTrace();
            return false;
        }
        return glInfo.valid;
    }
    
    /*-------------------------------------------------------------------------------------------------*/
    
    public GeoLocationInfo getGeoLocationInfo()
    {
        return glInfo;
    }
    public String getGeoLocation()
    {
        return glInfo.location;
    }

    public String getLatLng()
    {
        if (glInfo.lat == null && glInfo.lng == null)
        {
            return null;
        }

        StringBuffer buf = new StringBuffer();
        if (glInfo.lat != null)
        {
            buf.append(glInfo.lat);
        }
        buf.append(",");
        if (glInfo.lng != null)
        {
            buf.append(glInfo.lng);
        }
        return buf.toString();
    }

    public String getLat()
    {
        return glInfo.lat;
    }

    public String getLng()
    {
        return glInfo.lng;
    }

    public String getCountry()
    {
        return glInfo.country;
    }
    public String getLocality()
    {
        return glInfo.locality;
    }
    /*--------------------------------------------------------------------------*/
    public static void main( String[] args)
    {
        String[][] lat_longs = {
            {"39 ", "-77.101"},
            {"19.2700", "84.9200"},
            {"20.951",  "85.098"}
        };
            
/*      for (int i = 0; i < lat_longs.length; i++  )
      {
          String lat = lat_longs[i][0];
          String lng = lat_longs[i][1];
         GeocoderClient gcClient = new GeocoderClient();
          gcClient.validateLatLong(lat, lng);
          GeoLocationInfo gcLocInfo = gcClient.getGeoLocationInfo();
          System.out.println("Location: " + gcLocInfo.location  + ", City/Region/Country: " + 
                gcLocInfo.city +", " + gcLocInfo.region+ ", " + gcLocInfo.country);
      }*/
      
      String[] addresses = {
            "Damascus, USA",
          "1600 Amphitheatre Parkway, Mountain View, CA",
           "11508 Swains Lock Terrace, Potomac, Maryland, USA",
        /*   "London", 
           "London, Canada",
           "Cambridge",
           "Cambridge, UK",
           "Miami",
           "Miami, Ohio",
          "NIH, Bethesda",
          "Bethesda",
          "Suburban hospital, Maryland", 
          "Bhubaneswar",
          "Gopalpur, India",*/
          "Nuakhali, India"
      };
      System.out.println("=================================");
               
      for (int i = 0; i <addresses.length; i++  )
      {
         GeocoderClient gcClient = new GeocoderClient();
         gcClient.validateLocation(addresses[i]);
          GeoLocationInfo gcLocInfo = gcClient.getGeoLocationInfo();
          System.out.println("Location: " + gcLocInfo.location  + ", City/Region/Country: " + 
                gcLocInfo.city +", " + gcLocInfo.region+ ", " + gcLocInfo.country);
      }
      
    }
}
