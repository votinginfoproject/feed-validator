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
        
        private static String topTypes = "|street_segment|precinct|precinct_split|contest|ballot|candidate|electoral_district|" + 
        "polling_location|early_vote_site|election_administration|election_official" + "|locality|state|" + "source|election|" + "ballot|candidate|referendum|ballot_response|custom_ballot|";
        private static String allTypes = VipContentHandler.topTypes + "precinct_id|precinct_split_id|electoral_district_id|" + 
                "polling_location_id|early_vote_site_id|election_administration_id|eo_id|ovc_id|" + "locality_id|state_id|" + "feed_contact_id|" +
                "ballot_id|candidate_id|referendum_id|ballot_response_id|custom_ballot_id|";
        private static String addressTypes = "|physical_address|mailing_address|address|non_house_address|filed_mailing_address|";
                
        public VipContentHandler(SemVal sv) {
                super();
                this.sv = sv;
        }

        //localName is the important one
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
                                }
                        }
                }
                content="";
        }

        public void characters(char[] ch, int start, int end) throws SAXException {
                if (lowLevelType != null) {
                        content += new String(ch, start, end);
                } else {
                        content="";
                }
        }

        public void endElement(String namespaceURI, String localName, String rawName) throws SAXException {
                String rawNameChk = "|" + rawName + "|";
                //System.out.println(namespaceURI + " " + rawName + " " + rawName);
                if (!addressMode) {
                        if (VipContentHandler.allTypes.contains(rawNameChk)) {
                                if (VipContentHandler.topTypes.contains(rawNameChk)) {
                                        topLevelType = null;
                                        //System.out.println("Top level is null");
                                        topLevelId = null;
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
                                        }
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