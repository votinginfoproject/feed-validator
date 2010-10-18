import java.io.IOException;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;

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
        
        private static String topTypes = "|street_segment|precinct|precinct_split|contest|ballot|candidate|electoral_district|" + 
        "polling_location|early_vote_site|election_administration|election_official" + "|locality|state|" + "source|election|" + "ballot|candidate|referendum|ballot_response|custom_ballot|";
        private static String allTypes = VipContentHandler.topTypes + "precinct_id|precinct_split_id|electoral_district_id|" + 
                "polling_location_id|early_vote_site_id|election_administration_id|eo_id|ovc_id|" + "locality_id|state_id|" + "feed_contact_id|" +
                "ballot_id|candidate_id|referendum_id|ballot_response_id|custom_ballot_id|";
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
        
                                        String attrName = atts.getQName(0);
                                        if (attrName == "id") {
                                                Long idLong = new Long(atts.getValue(0));
                                                topLevelId = idLong;
                                                //System.out.println("Adding ID " + idLong + " of type " + topLevelType);
                                                sv.addIdType(idLong, rawName, locator.getLineNumber());
                                        } else {
                                                sv.addError("First attribute for " + rawName + " at line " + locator.getLineNumber() + " is " + attrName);
                                        }
                                } else { //connector
                                        lowLevelType = rawName;
                                        getContent=true;
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
                }
                if (VipContentHandler.numCheckTypes.contains(rawNameChk)) {
                        if (rawName.equals("end_house_number")) {
                                try {
                                        long endnum = Long.parseLong(content);
                                        if (endnum==0) {
                                                sv.addError("Your " + rawName + " at line " + locator.getLineNumber() + " is 0, which doesn't make sense. To signify the entire street is in this segment, use a very large number like 99999.");
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
                                } catch (Exception e) {
                                        sv.addError("Content of " + rawName + " at line " + locator.getLineNumber() +  " is not a (parseable) number: " + content);
                                }
                        }
                        getContent=false;
                }
                if (rawName.equals("state") && addressMode) {
                        if(content.length() != 2) {
                                sv.addWarning("Your " + topLevelType +" state at line " + locator.getLineNumber() + " is not two characters long.");
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

}