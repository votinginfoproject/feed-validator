import java.awt.*;
import javax.swing.*;
import java.io.*;
import java.net.*;
import javax.jnlp.*;
import java.awt.event.*;

public class VipSemChecker {

        private static TextField tf1;
        private static TextField tf2;
        private static TextArea ta1;
        private static Choice c1, choiceGC;
        private static FileContents localXMLFC = null;
        private static JFrame frame;

        public static void main(String args[]) {

                frame = new JFrame("VIP Semantic Checker");
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                Container contentPane = frame.getContentPane();
                SpringLayout layout = new SpringLayout();
                frame.setLayout(layout);

                int cols = 60;
                Label label1 = new Label("XML File");
                Label label2 = new Label("XSD File");
                Label label3 = new Label("Result:");
                Label label4 = new Label("(Full file path)");
                Label label5 = new Label("Last update: August 3, 2011");
                Label labelGC = new Label("Geocoder: ");
                choiceGC = new Choice();
                choiceGC.add("Google");
                choiceGC.add("Bing");
                choiceGC.add("None");
                tf1 = new TextField(cols);
                tf2 = new TextField(cols);
                //tf2.setEditable(false);
                tf2.setEditable(true);
                Button b1 = new Button("Open File");
                c1 = new Choice();
                c1.add("v3.0");
                c1.add("v2.3");
                c1.add("v2.2");
                c1.add("v2.1");
                c1.add("v2.0");
                //c1.add("v1.5");
                updateXSDTF();

                Button b3 = new Button("Check File Syntax and Semantics");
                ta1 = new TextArea(25, cols + 10);
                ta1.setEditable(false);
                int xPadInt = 5;
                Spring xPad = Spring.constant(xPadInt);
                int yPadInt = 5;
                Spring yPad = Spring.constant(yPadInt);
                



                layout.putConstraint(SpringLayout.WEST, label1,
                        xPadInt, SpringLayout.WEST, contentPane);
                layout.putConstraint(SpringLayout.NORTH, label1,
                        yPadInt, SpringLayout.NORTH, contentPane);

                layout.putConstraint(SpringLayout.WEST, tf1, xPadInt,
                        SpringLayout.EAST, label1);
                layout.putConstraint(SpringLayout.NORTH, tf1, yPadInt,
                        SpringLayout.NORTH, contentPane);

                /*layout.putConstraint(SpringLayout.WEST, b1, xPadInt,
                        SpringLayout.EAST, tf1);
                layout.putConstraint(SpringLayout.NORTH, b1, yPadInt,
                        SpringLayout.NORTH, contentPane);*/
                layout.putConstraint(SpringLayout.WEST, label4, xPadInt,
                        SpringLayout.EAST, tf1);
                layout.putConstraint(SpringLayout.NORTH, label4, yPadInt,
                        SpringLayout.NORTH, contentPane);                        

                layout.putConstraint(SpringLayout.WEST, label2,
                        xPadInt, SpringLayout.WEST, contentPane);
                layout.putConstraint(SpringLayout.NORTH, label2,
                        yPadInt, SpringLayout.SOUTH, label1);

                layout.putConstraint(SpringLayout.WEST, tf2, xPadInt,
                        SpringLayout.EAST, label2);
                layout.putConstraint(SpringLayout.NORTH, tf2, yPadInt,
                        SpringLayout.SOUTH, tf1);

                layout.putConstraint(SpringLayout.WEST, c1, xPadInt,
                        SpringLayout.EAST, tf2);
                layout.putConstraint(SpringLayout.NORTH, c1, yPadInt,
                        SpringLayout.SOUTH, b1);
                layout.putConstraint(SpringLayout.NORTH, c1, yPadInt,
                        SpringLayout.SOUTH, label4);
                        
                layout.putConstraint(SpringLayout.WEST, b3, xPadInt,
                        SpringLayout.WEST, contentPane);
                layout.putConstraint(SpringLayout.NORTH, b3, yPadInt,
                        SpringLayout.SOUTH, label2);

                layout.putConstraint(SpringLayout.WEST, label3,
                        xPadInt, SpringLayout.WEST, contentPane);
                layout.putConstraint(SpringLayout.NORTH, label3,
                        yPadInt, SpringLayout.SOUTH, b3);

                layout.putConstraint(SpringLayout.WEST, ta1, xPadInt,
                        SpringLayout.EAST, label2);
                layout.putConstraint(SpringLayout.NORTH, ta1, yPadInt,
                        SpringLayout.SOUTH, b3);
                
                //geo
                layout.putConstraint(SpringLayout.WEST, labelGC, xPadInt*2,
                        SpringLayout.EAST, b3);
                layout.putConstraint(SpringLayout.NORTH, labelGC, yPadInt,
                        SpringLayout.SOUTH, tf2);
                layout.putConstraint(SpringLayout.WEST, choiceGC, xPadInt/4,
                        SpringLayout.EAST, labelGC);
                layout.putConstraint(SpringLayout.NORTH, choiceGC, yPadInt,
                        SpringLayout.SOUTH, tf2);

                        
                layout.putConstraint(SpringLayout.NORTH, label5, yPadInt,
                        SpringLayout.SOUTH, ta1);

                contentPane.add(label1);
                contentPane.add(tf1);
                //contentPane.add(b1);
                contentPane.add(label4);
                contentPane.add(label2);
                contentPane.add(tf2);
                contentPane.add(c1);
                contentPane.add(label3);
                contentPane.add(ta1);
                contentPane.add(b3);
                contentPane.add(labelGC);
                contentPane.add(choiceGC);
                contentPane.add(label5);



                Spring maxHeightSpring = Spring.constant(0);
                Spring maxWidthSpring = Spring.constant(0);
                Component[] components = contentPane.getComponents();
                for (int i = 0; i < components.length; i++) {
                        SpringLayout.Constraints cons =
                                layout.getConstraints(components[i]);
                        maxWidthSpring = Spring.max(maxWidthSpring,
                                cons.getConstraint("East"));
                        maxHeightSpring = Spring.max(maxHeightSpring,
                                cons.getConstraint("South"));
                }
                SpringLayout.Constraints pCons =
                        layout.getConstraints(contentPane);
                pCons.setConstraint("East", Spring.constant(650));
                pCons.setConstraint("South", Spring.constant(510));


                frame.pack();
                frame.setVisible(true);

                /* Do stuff */
                b1.addActionListener(new ActionListener() {
                             public void actionPerformed(ActionEvent e) {

                                     FileOpenService fos;

                             try {
                                             fos = (FileOpenService) ServiceManager.lookup("javax.jnlp.FileOpenService");
                             } catch (
                                     UnavailableServiceException uexc) {
                                             fos = null;
                                     }

                             if (fos != null) {
                                     try {
                                     // ask user to select a file through this service
                                     String[] exts = new String[1];
                                     exts[0]="xml";
                                     FileContents fc = fos.openFileDialog(null, exts);
                                             if (fc !=  null) {
                                                     tf1.setText(fc.getName());
                                                     localXMLFC = fc;
                                             }

                                     } catch (Exception exc) {
                                                     exc.printStackTrace();
                                             }
                                     }


                             }
                     }
                    );

                c1.addItemListener(new ItemListener() {
                                           public void itemStateChanged(
                                                   ItemEvent e) {
                                                   updateXSDTF();
                                           }
                                   }
                                  );

                b3.addActionListener(new ActionListener() {
                                             public void actionPerformed(
                                                     ActionEvent e) {
                                                     validate();
                                             }
                                     }
                                    );

        }

        private static void updateXSDTF() {
                tf2.setText(
                        "http://election-info-standard.googlecode.com/files/vip_spec_" +
                        c1.getSelectedItem() + (c1.getSelectedItem().equals("v2.2") ? "a" : "") + ".xsd");
        }

        private static void validate() {
                SemVal sv = null;
                boolean go = false;
                //System.out.println(tf1.getText().length());
                if (localXMLFC != null) {
                        sv = new SemVal(localXMLFC, tf2.getText());
                        go = true;
                } else if (tf1.getText().length() > 0) {
                        sv = new SemVal(tf1.getText(), tf2.getText());
                        go = true;
                } else {
                        ta1.setText("Please pick an XML file." + "\n");
                }

                if (go) {
                        sv.setGeocoderType(choiceGC.getSelectedItem());
                        ta1.setText(
                                "Ready for syntactic and semantic validation..." +
                                "\n");
                        toggleCursor(true);
                        String msg = sv.validateSyntax();
                        appendMessage(msg);
                        if (sv.isSynValid()) {
                                appendMessage("\n------------------------------------------------\n");
                                msg = sv.validateSemantics();
                                appendMessage(msg);
                        }
                        toggleCursor(false);
                }
        }

        public static void appendMessage(String msg) {
                ta1.setText(ta1.getText() + msg + "\n");
        }

        private static void toggleCursor(boolean waiting) {
                if (waiting) {
                        frame.setCursor( Cursor.getPredefinedCursor(
                                Cursor.WAIT_CURSOR));
                } else {
                        frame.setCursor(Cursor.getDefaultCursor());
                }
        }

        public static boolean isAlive() {
                return(frame != null);
        }

}

//D:\google\spec\sample feed for v2.0.xml.txt
//D:\google\state data\NC\vipFeed-37-2008-10-05T18-07-18.xml
//file://localhost/D:/google/spec/vip_spec_v2.0.xsd