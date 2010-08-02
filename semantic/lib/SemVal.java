import java.util.*;
import javax.xml.transform.*;
import javax.xml.validation.*;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.transform.dom.*;
import javax.xml.transform.sax.*;
import javax.xml.parsers.*;
import java.net.URL;
import org.xml.sax.*;
import org.xml.sax.helpers.*;
import org.w3c.dom.*;
import org.apache.xerces.jaxp.validation.*;
//import org.apache.xerces.parsers.SAXParser;
import java.io.*;
import javax.jnlp.*;

public class SemVal {

        private FileContents localXMLFC;
        private InputStream localXMLIS;
        private InputStream localXMLIS2;
        private long xmlBytes;
        private String urlXSD;
        public boolean synValid = false;

        private Vector <String> errors;
        
        private TreeSet<Long> allIds;
        private Hashtable <String, Vector<Long>> typeIds;
        private Hashtable <String, Hashtable<Vector<Object>,Long>> links;
        public static final int maxErrors=10;

        public SemVal(String localXML, String urlXSD) {
                File myfile = new File(localXML);
                try {
                        this.xmlBytes = myfile.length();
                        this.localXMLIS = new FileInputStream(myfile);
                        this.localXMLIS2 = new FileInputStream(myfile);
                } catch (Exception e) {
                        System.out.println(e.getMessage());
                        output(e.getMessage());
                }
                this.urlXSD = urlXSD;
                this.allIds = new TreeSet<Long>();
                //System.out.println("Object created");
        }


        public SemVal(FileContents localXMLFC, String urlXSD) {
                try {
                        this.localXMLFC=localXMLFC;
                        this.localXMLIS = localXMLFC.getInputStream();
                        this.localXMLIS2 = null;
                        this.urlXSD = urlXSD;
                        this.xmlBytes = localXMLFC.getLength();
                } catch (Exception e) {
                        System.out.println(e.getMessage());
                        output(e.getMessage());
                }
        }

        public String validateSyntax() {
                XMLSchemaFactory xmlSF = new XMLSchemaFactory();
                String lngStr = "";
                if (xmlBytes > (1024 * 1024)) {
                        lngStr = Math.round(xmlBytes / (1024 * 1024)) +
                                "MB";
                } else if (xmlBytes > 1024) {
                        lngStr = Math.round(xmlBytes / 1024) + "KB";
                } else {
                        lngStr = xmlBytes + " bytes";
                }
                output("Staring syntactic validation....");
                output("Your XML file is " +
                        lngStr + ".");
                output("On test computers, files of size 500MB took about one minute.");
                Schema xmlS = null;
                try {
                        URL schemaURL = new URL(urlXSD);
                        xmlS = xmlSF.newSchema(schemaURL);
                } catch (Exception e) {
                        return(e.getMessage());
                }
                Validator myval = xmlS.newValidator();

                InputSource myis = new InputSource(localXMLIS);
                SAXSource myss = new SAXSource(myis);


                SAXResult res = null;
                try {
                        res = new SAXResult();
                        //System.out.println("start");
                        myval.validate(myss, res);
                        synValid = true;
                        return("Syntactic Validation Passed!");
                } catch (Exception e) {
                        synValid=false;
                        return(e.getMessage() + "\n" + res.toString());
                }
        }
       
        public String validateSemantics() {

                output("Starting Semantic Validation...this could take a while.");
                XMLReader parser;
                try {                
                        //XMLReader parser = new SAXParser();
                        SAXParserFactory factory = SAXParserFactory.newInstance();
                        parser = factory.newSAXParser().getXMLReader();
                        //System.out.println(parser.getProperty("http://apache.org/xml/properties/input-buffer-size"));
                        //parser.setProperty("http://apache.org/xml/properties/input-buffer-size",new Integer(1));
                        //System.out.println(parser.getProperty("http://apache.org/xml/properties/input-buffer-size"));
                        
                        errors = new Vector < String > ();
                        typeIds = new Hashtable <String, Vector<Long>> ();
                        links = new Hashtable <String, Hashtable<Vector<Object>,Long>>();
                        
                        ContentHandler contentHandler =
                                new VipContentHandler(this);
                        //DefaultHandler dh = (DefaultHandler)contentHandler;
                        parser.setContentHandler(contentHandler);

                } catch (Exception e) {
                        return("Caught pre-parse error: " + e.getMessage());
                }
                        
                try {
                        if (this.localXMLIS2==null) {
                                //this.localXMLIS.close();
                                //this.localXMLIS2=this.localXMLFC.getInputStream();
                                //this.localXMLIS2=this.localXMLIS;
                        }
                        parser.parse(new InputSource(localXMLIS2));

                } catch (Exception e) {
                        return("Caught error on parse: " + e.getMessage());
                }
                
                //report any errors
                output("\n\n");
                if (errors.size()>0) {
                        output("Found " + errors.size() + " top-level error(s)." + (errors.size()==maxErrors ? "(Stopped after " + maxErrors + " errors.)" : ""));
                        if (errors.size()>=SemVal.maxErrors) output("Displaying first " + SemVal.maxErrors +" errors below.");
                        for(int i =0; i<errors.size() && i< SemVal.maxErrors; i++) {
                                int num=i+1;
                                output("Error " + num + ": " + (String)errors.get(i));
                        }
                        //output("Fix these error(s) first.\n");
                }
                        
                        //report ids
                        
                        if(1==0) {
                                for (Enumeration e = links.keys(); e.hasMoreElements() ;) {
                                        String key = (String)e.nextElement();
                                        Hashtable<Vector<Object>,Long> thisTypeLinks = links.get(key);
                                        System.out.println("Found " + thisTypeLinks.size() + " links(s) of type " + key);
                                }
                        }
                        //test individual subsets
                        boolean allGood=true;
                        boolean perfect=false;
                        boolean streetSegsGoodP=false;
                        boolean streetSegsGoodPS=false;
                        
                        if (allGood && (hasType("street_segment"))) {
                                output("Checking semantics of Street Segments...");
                                if(!hasType("precinct") && !hasType("precinct_split")) {
                                        output("Error: You have street_segment elements but no precinct or precinct_split elements.");
                                }
                                streetSegsGoodP = checkLinkType("street_segment->precinct_id");
                                streetSegsGoodPS = checkLinkType("street_segment->precinct_split_id");                        
                                
                                allGood=(streetSegsGoodP || streetSegsGoodPS);
                                perfect=(streetSegsGoodP && streetSegsGoodPS);
                                output("..." + (allGood ? (perfect ? "passed." : "passed with non-fatal errors.") : "failed."));
                        }
                        if (allGood && hasType("precinct")) {
                                output("\nChecking semantics of Precincts...");
                                if(!hasType("locality")) {
                                        output("Error: You have precinct elements but no street_segment elements.");
                                }
                                streetSegsGoodP = checkLinkType("precinct->locality_id");
                                
                                allGood=(streetSegsGoodP || streetSegsGoodPS);
                                perfect=(streetSegsGoodP && streetSegsGoodPS);
                                output("..." + (allGood ? (perfect ? "passed." : "passed with non-fatal errors.") : "failed."));
                        }                        
                        if (allGood && hasType("polling_location")) {
                                output("\nChecking Semantics of Poll Locations...");
                                if(!hasType("precinct") && !hasType("precinct_split")) {
                                        output("Error: You have polling_location elements but no precinct or precinct_split elements.");
                                }
                                boolean precinctToSplit = checkLinkType("precinct_split->precinct_id");
                                boolean precinctToPoll = checkLinkType("precinct->polling_location_id");
                                boolean splitToPoll = checkLinkType("precinct_split->polling_location_id");
                                allGood=(streetSegsGoodP && precinctToPoll)  || (streetSegsGoodPS && splitToPoll) || 
                                        (streetSegsGoodPS && precinctToSplit && precinctToPoll);
                                perfect = streetSegsGoodPS && precinctToSplit && precinctToPoll && splitToPoll;
                                output("..." + (allGood ? (perfect ? "passed." : "passed with non-fatal errors.") : "failed."));

                                if (allGood && hasType("electoral_district")) {
                                        output("\nChecking Semantics of Districts...");
                                        if(!hasType("precinct") && !hasType("precinct_split")) {
                                                output("Error: You have electoral_district elements but no precinct or precinct_split elements.");
                                        }
                                        boolean precinctToED = checkLinkType("precinct->electoral_district_id");
                                        boolean splitToED = checkLinkType("precinct_split->electoral_district_id");
                                        allGood=(streetSegsGoodP && precinctToED)  || (streetSegsGoodPS && splitToED) || 
                                                (streetSegsGoodPS && precinctToSplit && precinctToED);
                                        perfect = streetSegsGoodPS && precinctToSplit && precinctToED && splitToED;
                                        output("..." + (allGood ? (perfect ? "passed." : "passed with non-fatal errors.") : "failed."));
                                }
                                
                        }

                        if (allGood && hasType("contest")) {
                                output("\nChecking Semantics of Contests...");
                                if(!hasType("electoral_district")) {
                                        output("Error: You have contest elements but no electoral_district elements.");
                                }
                                if(!hasType("ballot")) {
                                        output("Error: You have contest elements but no ballot elements.");
                                }                                
                                boolean contestToED = checkLinkType("contest->electoral_district_id");
                                boolean contestToBallot = checkLinkType("contest->ballot_id");
                                allGood=contestToED && (contestToBallot || !hasType("ballot"));
                                perfect=contestToED && contestToBallot;
                                output("..." + (allGood ? (perfect ? "passed." : "passed with non-fatal errors.") : "failed."));
                        }                  

                        if (allGood && hasType("ballot")) {
                                output("\nChecking Semantics of Ballots...");
                                if(!hasType("candidate") && !hasType("referendum") && !hasType("custom_ballot")) {
                                        output("Error: You have ballot elements but no candidate, referendu, or custom_ballot elements.");
                                        allGood=false;
                                }
                                boolean ballotToCandidate = checkLinkType("ballot->candidate_id");
                                boolean ballotToRefendum = checkLinkType("ballot->referendum_id");
                                boolean ballotToCustomBallot = checkLinkType("ballot->custom_ballot_id");
                                allGood=allGood && (ballotToCandidate || !hasType("candidate")) && (ballotToRefendum || !hasType("referendum")) &&
                                        (ballotToCustomBallot || !hasType("custom_ballot"));
                                perfect=allGood;
                                output("..." + (allGood ? (perfect ? "passed." : "passed with non-fatal errors.") : "failed."));
                        }
                        
                        if (allGood && hasType("locality")) {
                                output("\nChecking Semantics of Locality...");
                                if(!hasType("state")) {
                                        output("Error: You have locality elements by no state element");
                                        allGood=false;
                                }
                                boolean localityToState = checkLinkType("locality->state_id");
                                allGood=allGood && localityToState;
                                perfect=allGood;
                                output("..." + (allGood ? (perfect ? "passed." : "passed with non-fatal errors.") : "failed."));
                        }
                        
                        if (allGood) {
                                return("\nSemantic Validation Passed!");
                        } else {
                                return("\nSemantic Validation Failed.");
                        }
        }
        
        private void output(String msg) {
                if (!VipSemChecker.isAlive()) {
                        System.out.println(msg);
                } else {
                        VipSemChecker.appendMessage(msg);
                }
        }

        private boolean hasType(String typeKey) {
                Vector<Long> allIdsOfType = typeIds.get(typeKey);
                return  (allIdsOfType!=null  &&  (allIdsOfType.size()>0));
        }
        
        //does the feed have links of this type
        private boolean hasLinkType(String typeKey) {
                Hashtable<Vector<Object>,Long> thisTypeLinks = links.get(typeKey);
                return(thisTypeLinks!=null);
        }
        
        private Vector<Object> findKeyForValue(Hashtable<Vector<Object>,Long> thisTypeLinks, Long needle) {
                Iterator<Map.Entry<Vector<Object>,Long>> iter=thisTypeLinks.entrySet().iterator();
                while(iter.hasNext()) {
                        Map.Entry<Vector<Object>,Long> me = iter.next();
                        if (me.getValue()==needle) {
                                return(me.getKey());
                        }
                }
                return(null);
        }
        
        //return true only when links exists and no link maps to a nonexistent object
        private boolean checkLinkType(String typeKey) {
                Hashtable<Vector<Object>,Long> thisTypeLinks = links.get(typeKey);
                if (thisTypeLinks==null) return(false);
                int numLinks=thisTypeLinks.size();
                if (numLinks>0) {
                        Collection<Long> toIds = thisTypeLinks.values(); //Collection of Long
                        String toType = SemVal.getToType(typeKey,true);
                        Vector<Long> allIdsOfType = typeIds.get(toType);
                        try {
                                if (allIdsOfType!=null) toIds.removeAll(allIdsOfType);
                        } catch(Exception e) {
                                System.out.println(e.getMessage());
                        }
                        String msg = "Relationship " + typeKey + " has " + numLinks + " link(s)";
                        boolean retv= !(toIds!=null && toIds.size()>0);
                        if (!retv) {
                                int numMissLinks = toIds.size();
                                msg += ", but includes " + numMissLinks + " missing link(s).\n";
                                Iterator iterMiss = toIds.iterator();
                                Long exId = (Long) iterMiss.next();
                                Vector<Object> exV=findKeyForValue(thisTypeLinks,exId);
                                if (exV==null) {
                                        msg+="Program error: couldn't find key " + exId;
                                } else {
                                        Long fromId=(Long)exV.get(0);
                                        Long lineNumber=(Long)exV.get(1);
                                        msg +="Example of error: At line number " + lineNumber + 
                                        " you linked from ID " + fromId + " to ID " + exId + " but there is no object with that ID.";
                                }
                        } else {
                                msg +=", and they look good.";
                        }
                        output(msg);
                        return(retv);
                } else {
                        return(false);
                }
        }
        
        public void addIdType(Long id, String elType, int lineNumber) {
                boolean newId = allIds.add(id);
                if (!newId) {
                     addError("Duplicate ID: " + id + " at line number " + lineNumber);   
                } else {
                        if (!typeIds.containsKey(elType)) {
                                typeIds.put(elType,new Vector<Long>());
                        }
                        Vector<Long> v = typeIds.get(elType);
                        v.add(id);
                }
        }

        private static String concatLinkTypes(String fromType, String toType) {
                return(fromType + "->" + toType);
        }
        
        private static String getToType(String concattedType, boolean dropIdPart) {
                String[] retvA = concattedType.split("->");
                String retv=retvA[1];
                if (dropIdPart) {
                        retv=retv.substring(0,retv.length()-3);
                }
                return(retv);
        }
        
        public void addLink(String fromType, Long fromId, int lineNumber, String toType, Long toId) {
                String linkType = SemVal.concatLinkTypes(fromType, toType);
                if (!links.containsKey(linkType)) {
                        links.put(linkType, new Hashtable<Vector<Object>,Long>());
                }
                Hashtable<Vector<Object>,Long> thisTypeLinks = links.get(linkType);
                Vector<Object> tmp = new Vector<Object>();
                tmp.add(fromId);
                tmp.add(new Long(lineNumber));
                thisTypeLinks.put(tmp, toId);
        }

        public void addError(String message) {
                errors.add(message);
        }
        
        public int getErrorCount() {
                return(errors.size());
        }

        public boolean isSynValid() {
                return(synValid);
        }

        public static void main(String[] args) {
                //SemVal sv = new SemVal("D:/google/spec/buggy feed for v2.1.xml.txt","file://localhost/D:/google/spec/vip_spec_v2.1.xsd");
                SemVal sv = new SemVal("D:/google/state data/VA/2010/vipFeed-51-2010-05-19T16-04-00.xml","file://localhost/D:/google/spec/vip_spec_v2.1.xsd");
                //SemVal sv = new SemVal("D:/google/state data/MT/vipFeed-30-2010-05-24T15-06-54.xml","file://localhost/D:/google/spec/vip_spec_v2.1.xsd");
                
                System.out.println(sv.validateSemantics());
        }

}