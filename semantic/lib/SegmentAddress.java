/* This class is horrible written with all its public field, but oh well, it's faster */
public class SegmentAddress {
                
                public String houseNumber;
                public String houseNumberPrefix;
                public String houseNumberSuffix;
                public String streetDirection;
                public String streetName;
                public String streetSuffix;
                public String addressDirection;
                public String apartment;
                public String city;
                public String state;
                public String zip;
                
                public SegmentAddress(){
                        houseNumber="";
                        houseNumberPrefix="";
                        houseNumberSuffix="";
                        streetDirection="";
                        streetName="";
                        streetSuffix="";
                        addressDirection="";
                        apartment="";
                        city="";
                        state="";
                        zip="";
                }
                
                public String getFullAddress() {
                        String retv=houseNumberPrefix + houseNumber + (houseNumberSuffix.indexOf('/')>=0 ? " " : "") + houseNumberSuffix;
                        if (streetDirection.length()>0) retv +=  " " + streetDirection;
                        if (streetName.length()>0) retv +=  " " + streetName;
                        if (streetSuffix.length()>0) retv +=  " " + streetSuffix;
                        if (addressDirection.length()>0) retv +=  " " + addressDirection;
                        if (apartment.length()>0) retv +=  " Apt " + apartment;
                        if (city.length()>0) retv +=  " " + city + ",";
                        if (state.length()>0) retv +=  " " + state;
                        if (zip.length()>0) retv +=  " " + zip;
                        return retv;
                }
}