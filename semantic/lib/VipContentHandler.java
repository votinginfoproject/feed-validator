import java.io.IOException;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import com.google.code.geocoder.*;
import com.google.code.geocoder.model.*;
import org.votinginfoproject.msgeocoder.*;
import java.io.*;
import java.net.URLConnection;
import java.net.URI;
import java.util.List;
import java.util.Random;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.FieldNamingPolicy;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpConnectionManager;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.methods.GetMethod;


//from: http://java.sun.com/developer/Books/xmljava/ch03.pdf
class VipContentHandler implements org.xml.sax.ContentHandler {

        /** Hold onto the locator for location information */
        private Locator locator;
        private SemVal sv;

        private String topLevelType;
        private Long topLevelId;
        private String lowLevelType;
        private boolean addressMode=false;
        private String content;
        private long startnumHold;
        private boolean getContent=false;
        private final Random randomGen = new Random();

        //for geocoding
        final Geocoder geocoder = new Geocoder();
        final static int geocodeMax=100;
        private int geocodeCount=0;
        private int geocodeSuccess=0;
        private boolean geocodeFailure=false;
        private SegmentAddress curSegmentAddress=null;
                
        private static String topTypes = "|street_segment|precinct|precinct_split|contest|ballot|candidate|electoral_district|" + "ballot_line_result|contest_result|" +
        "polling_location|early_vote_site|election_administration|election_official" + "|locality|state|" + "source|election|" + "ballot|candidate|referendum|ballot_response|custom_ballot|";
        private static String allTypes = VipContentHandler.topTypes + "precinct_id|precinct_split_id|electoral_district_id|" + 
                "polling_location_id|early_vote_site_id|election_administration_id|eo_id|ovc_id|" + "locality_id|state_id|" + "feed_contact_id|" +
                "ballot_id|candidate_id|referendum_id|ballot_response_id|custom_ballot_id|jurisdiction_id|contest_id|";
        private static String numCheckTypes="|start_house_number|end_house_number|";
        private static String addressTypes = "|physical_address|mailing_address|address|non_house_address|filed_mailing_address|";
              
        public VipContentHandler(SemVal sv) {
                super();
                this.sv = sv;
        }

        //rawName is the important one
        public void startElement(String namespaceURI, String localName, String rawName, Attributes atts) throws SAXException {
                String rawNameChk = "|" + rawName + "|";
                if (VipContentHandler.addressTypes.contains(rawNameChk)) {
                        addressMode=true;
                }
                if (!addressMode) {
                        if (VipContentHandler.allTypes.contains(rawNameChk)) {
                                if (VipContentHandler.topTypes.contains(rawNameChk)) {
                                        topLevelType = rawName;
                                        //System.out.println("Top level is: " + topLevelType);
                                        
                                        if (atts.getLength()>0) {
                                                String attrName = atts.getQName(0);
                                                if (attrName.equals("id")) {
                                                        Long idLong = new Long(atts.getValue(0));
                                                        topLevelId = idLong;
                                                        //System.out.println("Adding ID " + idLong + " of type " + topLevelType);
                                                        sv.addIdType(idLong, rawName, locator.getLineNumber());
                                                } else {
                                                        sv.addError("First attribute for " + rawName + " at line " + locator.getLineNumber() + " is " + attrName);
                                                }
                                        } else { //no attributes
                                                sv.addError("No attributes for " + rawName + " at line " + locator.getLineNumber() + "; you need an id attribute here.");
                                        }
                                        
                                        //geocoding -- start a new address
                                        if (topLevelType.equals("street_segment")) {
                                                curSegmentAddress=new SegmentAddress();
                                        }
                                        
                                } else { //connector
                                        lowLevelType = rawName;
                                        getContent=true;
                                        if (topLevelType.equals("ballot") && lowLevelType.equals("candidate_id") && atts.getLength()>0) {
                                                if(atts.getQName(0).equals("sort_order")) {
                                                        sv.addWarning("You included a sort_order attribute in ballot.candidate_id, which has been deprecated as of v3.0. Use candidate.sort_order instead; the VIP staff can help with this transition.");
                                                }
                                        }
                                }
                        }
                        if (VipContentHandler.numCheckTypes.contains(rawNameChk)) getContent=true;
                } else {
                        getContent=true;
                }
                content="";
        }

        public void characters(char[] ch, int start, int end) throws SAXException {
                if (getContent) {
                        content += new String(ch, start, end);
                } else {
                        content="";
                }
        }

        public void endElement(String namespaceURI, String localName, String rawName) throws SAXException {
                String rawNameChk = "|" + rawName + "|";
                //System.out.println(namespaceURI + " " + localName + " " + rawName);
                if (!addressMode) {
                        if (VipContentHandler.allTypes.contains(rawNameChk)) {
                                if (VipContentHandler.topTypes.contains(rawNameChk)) {
                                        topLevelType = null;
                                        //System.out.println("Top level is null");
                                        topLevelId = null;

                                        if (rawName.equals("street_segment")) {
                                                
                                                if (curSegmentAddress!=null) {
                                                        if (!geocodeFailure && (geocodeCount<geocodeMax) && !(sv.getGeocoderType().equals("None")) && 
                                                                (randomGen.nextDouble()<0.05) && !(curSegmentAddress.streetName.equals("*"))) {
                                                                String addr = curSegmentAddress.getFullAddress();
                                                                if (sv.getGeocoderType().equals("Google")) {
                                                                        //geocode test
                                                                        GeocoderRequest geocoderRequest = null;
                                                                        GeocodeResponse geocoderResponse = null;
                                                                        try {
                                                                                geocoderRequest = new GeocoderRequestBuilder().setAddress(addr).setLanguage("en").getGeocoderRequest();
                                                                                geocoderResponse = geocoder.geocode(geocoderRequest);
                                                                        } catch (Exception e) {
                                                                                if (geocodeFailure==false) {
                                                                                        sv.addGeoWarning("Geocoding failed, check Internet connection.");
                                                                                        geocodeFailure=true;
                                                                                }
                                                                        }
                                                                        if (geocoderResponse!=null) {
                                                                                List<GeocoderResult> geocoderResults = geocoderResponse.getResults();
                                                                                
                                                                                //System.out.println("Addr: " + addr);
                                                                                if (geocoderResults.isEmpty()) {
                                                                                        String noWarningStatuses="|OVER_QUERY_LIMIT|";
                                                                                        //don't issue warning for over query limit
                                                                                        if (noWarningStatuses.indexOf(geocoderResponse.getStatus().value())<0) {
                                                                                                sv.addGeoWarning("No geocode results (status of " + geocoderResponse.getStatus().value() +") for address (line " + locator.getLineNumber() + "): " + addr);
                                                                                        }
                                                                                        String notAddrFaultStatuses="|OVER_QUERY_LIMIT|REQUEST_DENIED|UNKNOWN_ERROR|";
                                                                                        if (notAddrFaultStatuses.indexOf(geocoderResponse.getStatus().value())>=0) geocodeCount--;
                                                                                } else {
                                                                                        GeocoderResult geocoderResult = (GeocoderResult)geocoderResults.get(0);
                                                                                        List<GeocoderAddressComponent> addressComponents = geocoderResult.getAddressComponents();
                                                                                        boolean hasRoute=false;
                                                                                        for(int i=0; i<addressComponents.size() && !hasRoute; i++) {
                                                                                                GeocoderAddressComponent addressComponent=(GeocoderAddressComponent)addressComponents.get(i);
                                                                                                List<String> componentTypes = addressComponent.getTypes();
                                                                                                for(int j=0;j<componentTypes.size();j++) {
                                                                                                        if (((String)componentTypes.get(j)).equals("route")) {
                                                                                                                hasRoute=true;
                                                                                                        }
                                                                                                }
                                                                                        }
                                                                                        if(!hasRoute) {
                                                                                                sv.addGeoWarning("Incomplete geocode for (line " + locator.getLineNumber() + "): " + addr);
                                                                                        } else {
                                                                                                geocodeSuccess++;
                                                                                        }
                                                                                }
                                                                                geocodeCount++;
                                                                        }
                                                                } else if (sv.getGeocoderType().equals("Bing")) {
                                                                        MSGeocoderResponse msgr = bingGeocode();
                                                                        if (msgr==null) {
                                                                                sv.addGeoWarning("Geocoding failed (no response object), check Internet connection.");
                                                                                geocodeFailure=true;
                                                                        } else if (msgr.getStatusDescription()==null) {
                                                                                sv.addGeoWarning("Geocoding failed because of an odd response from Bing.");
                                                                                geocodeFailure=true;
                                                                        } else if (!msgr.getStatusDescription().equals("OK")) {
                                                                                sv.addGeoWarning("Geocoding Error: failure status is " + msgr.getStatusDescription());
                                                                                geocodeFailure=true;
                                                                        } else {
                                                                                geocodeCount++;
                                                                                List<MSGeocoderResults> resourceSets = msgr.getResourceSets();
                                                                                if (resourceSets==null || resourceSets.size()==0) {
                                                                                        sv.addGeoWarning("No geocode results (status of " + msgr.getStatusDescription() + ") for address (line " + locator.getLineNumber() + "): " + addr);
                                                                                } else {
                                                                                        boolean hasRoute=false;
                                                                                        for(int i = 0; i<resourceSets.size(); i++) {
                                                                                                MSGeocoderResults resourceSet = resourceSets.get(i);
                                                                                                List<MSGeocoderResult> results = resourceSet.getResources();
                                                                                                for(int j = 0; j<results.size(); j++) {
                                                                                                        MSGeocoderResult result = results.get(j);
                                                                                                        if (result.getEntityType().equals("Address") || result.getEntityType().equals("RoadBlock")) {
                                                                                                                hasRoute=true;
                                                                                                        }
                                                                                                }
                                                                                        }
                                                                                        if(!hasRoute) {
                                                                                                sv.addGeoWarning("Incomplete geocode for (line " + locator.getLineNumber() + "): " + addr);
                                                                                        } else {
                                                                                                geocodeSuccess++;
                                                                                        }
                                                                                }
                                                                       }
                                                                }
                                                        }
                                                        curSegmentAddress=null;
                                                }
                                                
                                                //erase start number
                                                startnumHold=-1;
                                        }

                                } else {
                                        if (content==null || content.equals("")) {
                                                sv.addError("Content of linking element " + lowLevelType + " at line " + locator.getLineNumber() +  " is blank.");
                                        } else {
                                                try {
                                                        Long toId = Long.parseLong(content);
                                                        //System.out.println("Linking element " + topLevelType + ":" + lowLevelType + " at line " + locator.getLineNumber());
                                                        sv.addLink(topLevelType, topLevelId, locator.getLineNumber(), lowLevelType, toId);
                                                } catch (Exception e) {
                                                        sv.addError("Content of linking element " + lowLevelType + " at line " + locator.getLineNumber() +  " is not a (parseable) number: " + content);
                                                }

                                                lowLevelType = null;
                                                getContent=false;
                                        }
                                }
                        }
                } else if (curSegmentAddress!=null) {
                        if (rawName.equals("street_direction")) {
                                curSegmentAddress.streetDirection=content;
                        } else if (rawName.equals("house_number_prefix")) {
                                curSegmentAddress.houseNumberPrefix=content;
                        } else if (rawName.equals("house_number_suffix")) {
                                curSegmentAddress.houseNumberSuffix=content;
                        } else if (rawName.equals("street_name")) {
                                curSegmentAddress.streetName=content;
                        } else if (rawName.equals("street_suffix")) {
                                curSegmentAddress.streetSuffix=content;
                        } else if (rawName.equals("address_direction")) {
                                curSegmentAddress.addressDirection=content;
                        } else if (rawName.equals("city")) {
                                curSegmentAddress.city=content;
                        } else if (rawName.equals("state")) {
                                curSegmentAddress.state=content;
                        } else if (rawName.equals("zip")) {
                                curSegmentAddress.zip=content;
                        }
                }
                if (VipContentHandler.numCheckTypes.contains(rawNameChk)) {
                        if (rawName.equals("end_house_number")) {
                                try {
                                        long endnum = Long.parseLong(content);
                                        if (endnum==0) {
                                                sv.addWarning("Your " + rawName + " at line " + locator.getLineNumber() + " is 0, which doesn't make sense. To signify the entire street is in this segment, use a very large number like 99999.");
                                        }
                                        if (startnumHold > -1 && endnum<startnumHold) {
                                                sv.addError("Your " + rawName + " at line " + locator.getLineNumber() + " is less than the corresponding start_house_number of " + startnumHold + ".");
                                        }
                                } catch (Exception e) {
                                        sv.addError("Content of " + rawName + " at line " + locator.getLineNumber() +  " is not a (parseable) number: " + content);
                                }
                        }
                        if (rawName.equals("start_house_number")) {
                                try {
                                        startnumHold = Long.parseLong(content);
                                        if (curSegmentAddress!=null) {
                                                curSegmentAddress.houseNumber=(startnumHold==0 ? 1 : startnumHold) + "";
                                        }
                                } catch (Exception e) {
                                        sv.addError("Content of " + rawName + " at line " + locator.getLineNumber() +  " is not a (parseable) number: " + content);
                                }
                        }
                        getContent=false;
                }
                if (rawName.equals("name") && !addressMode && topLevelType=="polling_location") {
                        if(content.length() > 0) {
                                sv.addWarning("You have a deprecated <polling_location>.<name> element in your feed at line" + locator.getLineNumber());
                        }
                }
                if (rawName.equals("state") && addressMode) {
                        if(content.length() != 2) {
                                sv.addWarning("Your " + topLevelType +" state at line " + locator.getLineNumber() + " is not two characters long.");
                        }
                }
                if (rawName.equals("vip_object")) {
                        if (geocodeCount>0) {
                                sv.addMessage("Geocoding results: " + geocodeSuccess + " successes out of " + geocodeCount + (geocodeCount!=1 ? " attempts" : " attempt") + ".");
                        }
                }
                if (topLevelType=="polling_location" & (addressMode || rawName.equals("location_name"))) {
                        if (rawName.equals("city") || rawName.equals("line1") || rawName.equals("location_name") || rawName.equals("zip")) {
                                if (content.length()==0) {
                                        sv.addWarning("Your polling location's " + rawName + " at line " + locator.getLineNumber() + " is blank.");
                                        sv.addBlank(rawName);
                                }
                        }
                }
                
                if (VipContentHandler.addressTypes.contains(rawNameChk)) {
                        addressMode=false;
                }
        }

        public void startDocument() throws SAXException {
        }
        public void endDocument() throws SAXException {
        }
        public void processingInstruction(String target,
                String data) throws SAXException {
        }
        public void startPrefixMapping(String prefix, String uri) {
        }
        public void endPrefixMapping(String prefix) {
        }

        public void ignorableWhitespace(char[] ch, int start,
                int end) throws SAXException {
        }
        public void skippedEntity(String name) throws SAXException {
        }

        public void setDocumentLocator(Locator locator) {
                // We save this for later use if desired.
                this.locator = locator;
        }
        
        private MSGeocoderResponse bingGeocode() {
                String url="//dev.virtualearth.net/REST/v1/Locations?" + "CountryRegion=US" + "&adminDistrict=" + curSegmentAddress.state + "&locality=" + curSegmentAddress.city
                         + "&postalCode=" + curSegmentAddress.zip + "&addressLine=" + curSegmentAddress.getLineAddress() + "&o=json&key=Aoxsm2ZLxChtNPudwVL2gCbIW4vlMmct87KIuYXECF4dkJnBBG3CPzuZ3u53e81-";
                         
                HttpClient httpClient = new HttpClient(new MultiThreadedHttpConnectionManager());
                GetMethod getMethod=null;
                try {
                        URI uri = new URI("http",url,null);
                        getMethod = new GetMethod(uri.toString());
                        Gson gson = new Gson();
                        httpClient.executeMethod(getMethod);
                        final Reader reader = new InputStreamReader(getMethod.getResponseBodyAsStream(), getMethod.getResponseCharSet());
                        MSGeocoderResponse msgr =null;
                        try {
                                msgr = gson.fromJson(reader, MSGeocoderResponse.class);
                        } catch (Exception e) {
                                System.out.println("JSON Error, contact VIP staff to fix this error for address: " + curSegmentAddress.getFullAddress());
                                System.out.println(e.getMessage());
                                e.printStackTrace();
                        }
                        return msgr;
                } catch (Exception e) {
                        System.out.println(e.getMessage());
                        e.printStackTrace();
                } finally {
                        getMethod.releaseConnection();
                }               
                return null;       
        }

        
}