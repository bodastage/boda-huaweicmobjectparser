/*
 * Parses Huawei GExport configuration data file XML to csv.
 * @version 1.0.0
 * @since 1.0.0
 */
package com.bodastage.boda_huaweicmobjectparser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Stack;
import java.util.logging.Level;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.Characters;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;


/**
 *
 * @author info@bodastage.com
 */
public class HuaweiCMObjectParser {

    final static String VERSION = "1.0.10";
    
    Logger logger = LoggerFactory.getLogger(HuaweiCMObjectParser.class);
        
    /**
     * Tracks Managed Object attributes to write to file. This is dictated by
     * the first instance of the MO found.
     *
     * @TODO: Handle this better.
     *
     * @since 1.0.0
     */
    private Map<String, Stack> moColumns = new LinkedHashMap<String, Stack>();

    /**
     *
     * Track parameter with children.
     *
     * @since 1.0.0
     */
    private Map<String, Stack> parameterChildMap = new LinkedHashMap<String, Stack>();

    /**
     * This holds a map of the Managed Object Instances (MOIs) to the respective
     * csv print writers.
     *
     * @since 1.0.0
     */
    private Map<String, PrintWriter> moiPrintWriters
            = new LinkedHashMap<String, PrintWriter>();

    /**
     * Tag data.
     *
     * @since 1.0.0
     * @version 1.0.0
     */
    private String tagData = "";

    /**
     * This is used when subsituting a parameter value with the value indicated
     * in comments.
     *
     * @since 1.0.0
     */
    private String previousTag;

    /**
     * Output directory.
     *
     * @since 1.0.0
     */
    private String outputDirectory = "/tmp";

    /**
     *
     * @since 1.0.0
     */
    private String nodeTypeVersion = "";

    /**
     * Parser start time.
     *
     * @since 1.0.4
     * @version 1.0.0
     */
    final long startTime = System.currentTimeMillis();

    /**
     * Tracks how deep a class tag is in the hierarchy.
     *
     * @since 1.0.0
     * @version 1.0.0
     */
    private int classDepth = 0;

    /**
     * Tracks how deep a class tag is in the XML hierarchy.
     *
     * @since 1.0.0
     * @version 1.0.0
     */
    private int objectDepth = 0;

    /**
     * The base file name of the file being parsed.
     *
     * @since 1.0.0
     * @version 1.0.0
     */
    private String baseFileName = "";

    /**
     * The file to be parsed.
     *
     * @since 1.0.0
     * @version 1.0.0
     */
    private String dataFile;

    /**
     * File or directory
     */
    private String dataSource;

    /**
     * The nodename.
     *
     * @since 1.0.0
     * @version 1.0.0
     */
    private String nodeName;

    /**
     * The holds the parameters and corresponding values for the moi tag
     * currently being processed.
     *
     * @since 1.0.0
     * @version 1.0.0
     */
    private Map<String, String> moiParameterValueMap
            = new LinkedHashMap<String, String>();

    /**
     * The holds the parameters and corresponding values for the moi tag
     * currently being processed.
     *
     * @since 1.0.0
     * @version 1.0.0
     */
//    private Map<String, LinkedHashMap<String, String>> classNameAttrsMap
//            = new LinkedHashMap<String, LinkedHashMap<String, String>>();
    LinkedHashMap<String, String> classNameAttrsMap = new LinkedHashMap<String, String>();
    
    /**
     * ClassName tag stack.
     *
     * @version 1.0.0
     * @since 1.0.0
     */
    private Stack classNameStack = new Stack();

    /**
     * Current className MO attribute.
     *
     * @since 1.0.0
     * @version 1.0.0
     */
    private String className = null;

    /**
     * Current object tag's name attribute value.
     *
     * @since 1.0.0
     * @version 1.0.0
     */
    private String moParamName = null;

    /**
     * The technology value of the first object tag.
     *
     * @since 1.0.0
     * @version 1.0.0
     */
    private String technology = null;

    /**
     * The vendor value of the first object tag.
     *
     * @since 1.0.0
     * @version 1.0.0
     */
    private String vendor = null;

    /**
     * The version value of the first object tag.
     *
     * @since 1.0.0
     * @version 1.0.0
     */
    private String version = null;

    /**
     * Parameter file
     */
    private String parameterFile = null;

    /**
     * Current parameter name attribute 
     */
    private String parameterNameAttr = null;
    
    /**
     * File extraction time
     * 
     * This is extract as the last digits before the file extension. e.g.
     * GExport_RNCNAME_10.22.111.88_20171211060843.xml.
     * It is required that the Gexport CM dump be of the format above.
     */
    private String dateTime = "";

    /**
     * Extract managed objects and their parameters
     */
    private Boolean extractParametersOnly = false;
    
    /**
     * Add meta fields to each MO.
     * FILENAME,DATETIME,NE_TECHNOLOGY,NE_VENDOR,NE_VERSION,NE_TYPE 
     */
    private Boolean extractMetaFields = false;
    
    /**
     * Parsing state
     *
     */
    private int parserState = ParserStates.EXTRACTING_PARAMETERS;

    public void setExtractParametersOnly(Boolean bool){
        extractParametersOnly = bool;
    }
    
    public void setExtractMetaFields(Boolean bool){
        extractMetaFields = bool;
    }
    
    /**
     * Extract parameter list from parameter file
     *
     * @param filename
     */
    public void getParametersToExtract(String filename) throws FileNotFoundException, IOException, Exception {
        BufferedReader br = new BufferedReader(new FileReader(filename));
        for (String line; (line = br.readLine()) != null;) {
            if(!line.contains(":")){
                throw new Exception("Wrong format. Contents:" + line);
            }
            
            String[] moAndParameters = line.split(":");
            String mo = moAndParameters[0];
            String[] parameters = moAndParameters[1].split(",");

            Stack parameterStack = new Stack();
            for (int i = 0; i < parameters.length; i++) {
                parameterStack.push(parameters[i]);
            }
            moColumns.put(mo, parameterStack);
            //domainHeaderAdded.put(mo, Boolean.TRUE);
        }

        parserState = ParserStates.EXTRACTING_VALUES;
    }

    /**
     * Reset some variables before parsing next file
     *
     */
    public void resetInternalVariables() {
        tagData = "";
        nodeTypeVersion = "";
        objectDepth = 0;
        classDepth = 0;
    }

    /**
     * Set parameter file
     *
     * @param filename
     */
    public void setParameterFile(String filename) {
        //logger.info("Setting parameter file" + filename);
        parameterFile = filename;
    }

    /**
     * Determines if the source data file is a regular file or a directory and
     * parses it accordingly
     *
     * @since 1.1.0
     * @version 1.0.0
     * @throws XMLStreamException
     * @throws FileNotFoundException
     * @throws UnsupportedEncodingException
     */
    public void processFileOrDirectory()
            throws XMLStreamException, FileNotFoundException, UnsupportedEncodingException {
        //this.dataFILe;
        Path file = Paths.get(this.dataSource);
        boolean isRegularExecutableFile = Files.isRegularFile(file)
                & Files.isReadable(file);

        boolean isReadableDirectory = Files.isDirectory(file)
                & Files.isReadable(file);

        if (isRegularExecutableFile) {
            this.setFileName(this.dataSource);
            baseFileName = getFileBasename(this.dataFile);
            if (parserState == ParserStates.EXTRACTING_PARAMETERS && extractParametersOnly == false) {
                System.out.print("Extracting parameters from " + this.baseFileName + "...");
             } else if(parserState == ParserStates.EXTRACTING_VALUES && extractParametersOnly == false) {
                System.out.print("Parsing " + this.baseFileName + "...");
            }
            this.parseFile(this.dataSource);
            if (parserState == ParserStates.EXTRACTING_PARAMETERS && extractParametersOnly == false) {
                System.out.println("Done.");
            } else if(parserState == ParserStates.EXTRACTING_VALUES && extractParametersOnly == false) {
                System.out.println("Done.");
                //System.out.println(this.baseFileName + " successfully parsed.\n");
            }
        }
        
        
        if (isReadableDirectory) {

            File directory = new File(this.dataSource);

            //get all the files from a directory
            File[] fList = directory.listFiles();

            for (File f : fList) {
                this.setFileName(f.getAbsolutePath());
                try {
                    baseFileName = getFileBasename(this.dataFile);
                    if (parserState == ParserStates.EXTRACTING_PARAMETERS && extractParametersOnly == false) {
                        System.out.print("Extracting parameters from " + this.baseFileName + "...");
                    } else if(parserState == ParserStates.EXTRACTING_VALUES && extractParametersOnly == false) {
                        System.out.print("Parsing " + this.baseFileName + "...");
                    }

                    //Parse
                    this.parseFile(f.getAbsolutePath());
                    if (parserState == ParserStates.EXTRACTING_PARAMETERS && extractParametersOnly == false) {
                        System.out.println("Done.");
                     } else if(parserState == ParserStates.EXTRACTING_VALUES && extractParametersOnly == false) {
                        System.out.println("Done.");
                        //System.out.println(this.baseFileName + " successfully parsed.\n");
                    }

                } catch (Exception e) {
                    System.out.println("Error");
                    System.out.println(e.getMessage());
                    logger.error("class name:" + className);
//                    logger.error("MO Parameter List:" + moColumns.get(className).toString());
//                    logger.error("MO parameter values:" + classNameAttrsMap.toString());
                    System.out.println("Skipping file: " + this.baseFileName + "\n");

                    resetInternalVariables();
                }
            }
        }

    }

    /**
     * Parser entry point
     *
     * @throws XMLStreamException
     * @throws FileNotFoundException
     * @throws UnsupportedEncodingException
     */
    public void parse() throws XMLStreamException, FileNotFoundException, UnsupportedEncodingException {
        //Extract parameters
        if (parserState == ParserStates.EXTRACTING_PARAMETERS) {
            processFileOrDirectory();

            parserState = ParserStates.EXTRACTING_VALUES;
        }


        //Reset variables
        resetInternalVariables();
        
        if(this.extractParametersOnly == true ){
            displayMOsAndParameters();
        }

        //Extracting values
        if (parserState == ParserStates.EXTRACTING_VALUES) {
            processFileOrDirectory();
            parserState = ParserStates.EXTRACTING_DONE;
        }

        closeMOPWMap();

        if(extractParametersOnly == false ){
            printExecutionTime();
        }
        
        
    }

    /*
    Print list of managed objects and their parameters 
    */
    public void displayMOsAndParameters(){
        for (Map.Entry<String, Stack> entry : moColumns.entrySet()) {
            String moName = entry.getKey();
            Stack moParameterList = moColumns.get(moName);

            String moParameterListString = moName + ":";

            if( extractMetaFields == true ){
                moParameterListString += "FILENAME,DATETIME,NE_TECHNOLOGY,NE_VENDOR,NE_VERSION,NE_TYPE";
            }
            
            int size = moParameterList.size();
            if(extractMetaFields == true && size > 0 ){
                    moParameterListString += ",";
            }
            
            for(int i = 0; i < size; i++){
                String param = (String)moParameterList.get(i);
                if( i == size -1) {
                    moParameterListString += param;
                } else{
                    moParameterListString += param + ",";
                }
            }
            
            System.out.println(moParameterListString);
        }
    }
    
    /**
     * The parser's entry point.
     *
     */
    public void parseFile(String inputFilename)
            throws XMLStreamException, FileNotFoundException, UnsupportedEncodingException {
        XMLInputFactory factory = XMLInputFactory.newInstance();

        XMLEventReader eventReader = factory.createXMLEventReader(
                new FileReader(inputFilename));
        baseFileName = getFileBasename(inputFilename);
        
        //Extract date from timestamp
        dateTime = inputFilename.replaceFirst(".*_(\\d+)\\.\\D{3}", "$1");
        dateTime = dateTime.replaceFirst("(\\d{4})(\\d{2})(\\d{2})(\\d{2})(\\d{2})(\\d{2})", "$1-$2-$3 $4:$5:$6");

        while (eventReader.hasNext()) {
            XMLEvent event = eventReader.nextEvent();

            switch (event.getEventType()) {
                case XMLStreamConstants.START_ELEMENT:
                    startElementEvent(event);
                    break;
                case XMLStreamConstants.SPACE:
                case XMLStreamConstants.CHARACTERS:
                    characterEvent(event);
                    break;
                case XMLStreamConstants.END_ELEMENT:
                    endELementEvent(event);
                    break;
                case XMLStreamConstants.COMMENT:
                    if (moiParameterValueMap.containsKey(this.previousTag)) {
                        String comment
                                = ((javax.xml.stream.events.Comment) event).getText();
                        moiParameterValueMap.put(previousTag, comment);
                    }
                    break;
            }
        }

    }

    /**
     * Handle start element event.
     *
     * @param xmlEvent
     *
     * @since 1.0.0
     * @version 1.0.0
     *
     */
    public void startElementEvent(XMLEvent xmlEvent) throws FileNotFoundException {

        StartElement startElement = xmlEvent.asStartElement();
        String qName = startElement.getName().getLocalPart();
        String prefix = startElement.getName().getPrefix();

        Iterator<Attribute> attributes = startElement.getAttributes();
        if (qName.equals("class")) {
            classDepth++;

            while (attributes.hasNext()) {
                Attribute attribute = attributes.next();
                String attrName = attribute.getName().getLocalPart();
                String attrValue = attribute.getValue();
                if (attrName.equals("name")) {
                    className = attrValue;
                    //className = attrValue;
                    LinkedHashMap<String, String> Lhm = new LinkedHashMap<String, String>();
//                    classNameAttrsMap.put(className, Lhm);

                    if (classDepth == 1) {
                        nodeTypeVersion = attrValue;
                    }
                }
            }
        }

        //parameter
        if (qName.equals("parameter")) {
            String paramName = null;
            String paramValue = null;
            while (attributes.hasNext()) {
                Attribute attribute = attributes.next();
                String attrName = attribute.getName().getLocalPart();
                String attrValue = attribute.getValue();
                if (attrName.equals("name")) {
                    paramName = attrValue.toUpperCase();
                }

                if (attrName.equals("value")) {
                    paramValue = attrValue;
                    
                    String tempValue = paramValue;
                    if (tempValue.matches("^([^-]+-[^-]+)(?:&[^-]+-[^-]+)+$") && !paramName.equals("ACTION")) {
                        

                        String mvParameter = className + "_" + paramName;
                        Stack children = new Stack();
                        
                        if( parserState == ParserStates.EXTRACTING_PARAMETERS){
                            parameterChildMap.put(mvParameter, null);
                        }
                        
                        String[] valueArray = tempValue.split("&");

                        for (int j = 0; j < valueArray.length; j++) {
                            String v = valueArray[j];
                            String[] vArray = v.split("-");
                            String childParameter = vArray[0];
                            String childParameterValue = vArray[1];
                            String child =  paramName + "_" + childParameter;
                            
                            classNameAttrsMap.put(child, childParameterValue);
                            
                            if( parserState == ParserStates.EXTRACTING_PARAMETERS){
                                 children.push(childParameter);
                            }
                        }
                        
                        if( parserState == ParserStates.EXTRACTING_PARAMETERS){
                             parameterChildMap.put(mvParameter, children);
                        }
                        
                    }else{
                       classNameAttrsMap.put(paramName, paramValue);
                    }
                    
                }
            }

            return;
        }

        //object
        if (qName.equals("object")) {
            objectDepth++;

            //Get the technology, vendor and version
            if (objectDepth == 1) {
                while (attributes.hasNext()) {
                    Attribute attribute = attributes.next();
                    String attrName = attribute.getName().getLocalPart();
                    String attrValue = attribute.getValue();
                    if (attrName.equals("vendor")) {
                        this.vendor = attrValue;
                    }

                    if (attrName.equals("technique")) {
                        this.technology = attrValue;
                    }

                    if (attrName.equals("version")) {
                        this.version = attrValue;
                    }
                }
            }

            return;
        }
    }

    /**
     * Handle character events.
     *
     * @param xmlEvent
     *
     * @version 1.0.0
     * @since 1.0.0
     */
    public void characterEvent(XMLEvent xmlEvent) {
        Characters characters = xmlEvent.asCharacters();
        if (!characters.isWhiteSpace()) {
            tagData = characters.getData();
        }
    }

    /**
     * Get file base name.
     *
     * @param filename String The base name of the input data file.
     *
     * @since 1.0.0
     * @version 1.0.0
     */
    public String getFileBasename(String filename) {
        try {
            return new File(filename).getName();
        } catch (Exception e) {
            return filename;
        }
    }

    /**
     * Processes the end tags.
     *
     * @param xmlEvent
     *
     * @since 1.0.0
     * @version 1.0.0
     * @throws FileNotFoundException
     * @throws UnsupportedEncodingException
     */
    public void endELementEvent(XMLEvent xmlEvent)
            throws FileNotFoundException, UnsupportedEncodingException {
        EndElement endElement = xmlEvent.asEndElement();
        String prefix = endElement.getName().getPrefix();
        String qName = endElement.getName().getLocalPart();

        if (qName.equals("class")) {
            classDepth--;
            return;
        }

        //Extract parameters
        if(qName.equals("object") 
                && parserState == ParserStates.EXTRACTING_PARAMETERS 
                && parameterFile == null){
            objectDepth--;
            
            Stack moiAttributes = new Stack();
            
            if(moColumns.containsKey(className)){
                moiAttributes = moColumns.get(className);
            }
            
            moiParameterValueMap = classNameAttrsMap;
            Iterator<Map.Entry<String, String>> iter
                        = moiParameterValueMap.entrySet().iterator();
            
            while (iter.hasNext()) {
                    Map.Entry<String, String> me = iter.next();
                    String parameterName = me.getKey();

                    String tempValue = classNameAttrsMap.get(parameterName);
//                    if (tempValue.matches("([^-]+-[^-]+&).*") && !parameterName.equals("ACTION") ) {
                    if (tempValue.matches("^([^-]+-[^-]+)(?:&[^-]+-[^-]+)+") && !parameterName.equals("ACTION") ) {
                        String mvParameter = className + "_" + parameterName;
                        parameterChildMap.put(mvParameter, null);
                        Stack children = new Stack();

                        String[] valueArray = tempValue.split("&");

                        for (int j = 0; j < valueArray.length; j++) {
                            String v = valueArray[j];
                            String[] vArray = v.split("-");
                            String childParameter = vArray[0];
                            String child =  parameterName + "_" + childParameter;
                            if(!moiAttributes.contains(child)){
                                moiAttributes.push(child);
                            }
                            children.push(childParameter);
                        }
                        parameterChildMap.put(mvParameter, children);

                        continue;
                    }
                    
                    if(!moiAttributes.contains(parameterName)){
                        moiAttributes.push(parameterName);
                    }
                    
            }

            moColumns.put(className, moiAttributes);
            
            moiParameterValueMap.clear();
            classNameAttrsMap.clear();
            return;
        }
        
        //Extract parameter values when paramete rfile is not provided
        if (qName.equals("object") && parameterFile == null) {
            objectDepth--;
            String paramNames = "FILENAME,DATETIME,NE_TECHNOLOGY,NE_VENDOR,NE_VERSION,NE_TYPE";
            String paramValues = baseFileName + "," + dateTime + "," + technology + "," + vendor + "," + version + "," + nodeTypeVersion;

            if (!moiPrintWriters.containsKey(className) ) {
                String moiFile = outputDirectory + File.separatorChar + className + ".csv";
                moiPrintWriters.put(className, new PrintWriter(moiFile));

                String pName = paramNames;

                Stack moiAttributes = moColumns.get(className);
                for (int i = 0; i < moiAttributes.size(); i++) {
                    String parameterName = moiAttributes.get(i).toString();
                    pName += "," + parameterName;
                }

                moiPrintWriters.get(className).println(pName);
                moiPrintWriters.get(className).flush();
            }

            Stack moiAttributes = moColumns.get(className);
            moiParameterValueMap = classNameAttrsMap;

            for (int i = 0; i < moiAttributes.size(); i++) {
                String moiName = moiAttributes.get(i).toString();
                String mvParameter = className + "_" + moiName;

                if (moiParameterValueMap.containsKey(moiName)) {
                    paramValues += "," + toCSVFormat(moiParameterValueMap.get(moiName));
                } else {
                    paramValues += ",";
                }
            }

            PrintWriter pw = moiPrintWriters.get(className);
            pw.println(paramValues);
            pw.flush();

            moiParameterValueMap.clear();
            classNameAttrsMap.clear();
            return;
        }
        
        //Extract values when parameter file is provided 
        if (qName.equals("object") && parameterFile != null) {
            objectDepth--;
            
            //Skip mo if it is not in the parameter file 
            //String className  = className.replaceAll("_.*$" ,"");
            
            if (!moColumns.containsKey(className)) return;
            
            //logger.info("className : " + className);
            
            //Get the parameter listed in the parameter file for the managed object
            Stack<String> parameterList  = moColumns.get(className);

            String paramNames = "";
            String paramValues = "";

            
            //Create file if it does not already exist
            if (!moiPrintWriters.containsKey(className)) {
                String moiFile = outputDirectory + File.separatorChar + className + ".csv";
                moiPrintWriters.put(className, new PrintWriter(moiFile));

                Stack moiAttributes = moColumns.get(className);
    
                String pName = paramNames;
                 for(int i = 0; i< moiAttributes.size(); i++){
                     String p = moiAttributes.get(i).toString();
                     pName += "," + p;
                 }
                 
                pName = pName.replaceFirst(",", "");
                moiPrintWriters.get(className).println(pName);
                moiPrintWriters.get(className).flush();
            }

            Stack moiAttributes = moColumns.get(className);
            moiParameterValueMap = classNameAttrsMap;
//            moiParameterValueMap = classNameAttrsMap.get(className);

            for (int i = 0; i < moiAttributes.size(); i++) {
                String moiName = moiAttributes.get(i).toString();
                
                //String mvParameter = className + "_" + moiName;
                String mvParameter = className + "_" + moiName;

                if (moiParameterValueMap.containsKey(moiName)) {
                    //Handle multivalued parameters
                    if (parameterChildMap.containsKey(mvParameter)) {
                        String tempValue = moiParameterValueMap.get(moiName);
                        String[] valueArray = tempValue.split("&");

                        for (int j = 0; j < valueArray.length; j++) {
                            String v = valueArray[j];
                            String[] vArray = v.split("-");
                            paramValues += "," + toCSVFormat(vArray[1]);
                        }
                        continue;
                    }

                    paramValues += "," + toCSVFormat(moiParameterValueMap.get(moiName));
                } else {
                    
                    if(moiName.equals("FILENAME")){
                        paramValues += "," + baseFileName;
                    }else if(moiName.equals("NE_TECHNOLOGY")){
                        paramValues += "," + technology;
                    }else if(moiName.equals("NE_VENDOR")){
                        paramValues += "," + vendor;
                    }else if(moiName.equals("NE_VERSION")){
                        paramValues += "," + version;
                    }else if(moiName.equals("NE_TYPE")){
                        paramValues += "," + nodeTypeVersion;
                    }else if(moiName.equals("DATETIME")){
                        paramValues += "," + dateTime;
                    }else{
                        paramValues += ",";
                    }

                }
                
            }
            
            paramValues = paramValues.replaceFirst(",", "");
            
            PrintWriter pw = moiPrintWriters.get(className);
            pw.println(paramValues);
            pw.flush();

            moiParameterValueMap.clear();
//            classNameAttrsMap.get(className).clear();
            classNameAttrsMap.clear();
            return;
        }
    }

    /**
     * Print program's execution time.
     *
     * @since 1.0.0
     */
    public void printExecutionTime() {
        float runningTime = System.currentTimeMillis() - startTime;

        String s = "Parsing completed. ";
        s = s + "Total time:";

        //Get hours
        if (runningTime > 1000 * 60 * 60) {
            int hrs = (int) Math.floor(runningTime / (1000 * 60 * 60));
            s = s + hrs + " hours ";
            runningTime = runningTime - (hrs * 1000 * 60 * 60);
        }

        //Get minutes
        if (runningTime > 1000 * 60) {
            int mins = (int) Math.floor(runningTime / (1000 * 60));
            s = s + mins + " minutes ";
            runningTime = runningTime - (mins * 1000 * 60);
        }

        //Get seconds
        if (runningTime > 1000) {
            int secs = (int) Math.floor(runningTime / (1000));
            s = s + secs + " seconds ";
            runningTime = runningTime - (secs / 1000);
        }

        //Get milliseconds
        if (runningTime > 0) {
            int msecs = (int) Math.floor(runningTime / (1000));
            s = s + msecs + " milliseconds ";
            runningTime = runningTime - (msecs / 1000);
        }

        System.out.println(s);
    }

    /**
     * Close file print writers.
     *
     * @since 1.0.0
     * @version 1.0.0
     */
    public void closeMOPWMap() {
        Iterator<Map.Entry<String, PrintWriter>> iter
                = moiPrintWriters.entrySet().iterator();
        while (iter.hasNext()) {
            iter.next().getValue().close();
        }
        moiPrintWriters.clear();
    }

    /**
     * Process given string into a format acceptable for CSV format.
     *
     * @since 1.0.0
     * @param s String
     * @return String Formated version of input string
     */
    public String toCSVFormat(String s) {
        String csvValue = s;

        //Check if value contains comma
        if (s.contains(",")) {
            csvValue = "\"" + s + "\"";
        }

        if (s.contains("\"")) {
            csvValue = "\"" + s.replace("\"", "\"\"") + "\"";
        }

        return csvValue;
    }

    /**
     * Set the output directory.
     *
     * @since 1.0.0
     * @version 1.0.0
     * @param String directoryName
     */
    public void setOutputDirectory(String directoryName) {
        this.outputDirectory = directoryName;
    }

    /**
     * Set name of file to parser.
     *
     * @since 1.0.0
     * @version 1.0.0
     * @param String filename
     */
    public void setFileName(String filename) {
        this.dataFile = filename;
    }

    /**
     * Add "U" to class name attribute value.
     *
     * @param classNameAttrValue
     *
     * @TODO: Remove nodeype from the file name.
     */
    public String addUToUMTSCellMOs(String classNameAttrValue) {
        String newMOName = classNameAttrValue;

        if (classDepth == 1) {
            return newMOName;
        }

        if (this.technology.equals("WCDMA")
                && classNameAttrValue.startsWith("CELL")) {
            newMOName = "U" + classNameAttrValue;
        }
        return newMOName;
    }
    
    /**
     * Set name of file to parser.
     *
     * @since 1.0.1
     * @version 1.0.0
     * @param dataSource
     */
    public void setDataSource(String dataSource) {
        this.dataSource = dataSource;
    }
    
    /**
     * Show parser help.
     * 
     * @since 1.0.0
     * @version 1.0.0
     */
    static public void showHelp(){
        System.out.println("boda-huaweicmobjectparser " + VERSION + " Copyright (c) 2018 Bodastage Solutions(http://www.bodastage.com)");
        System.out.println("Parses Huawei GExport configuration data file XML to csv.");
        System.out.println("Usage: java -jar boda-huaweicmobjectparser.jar <fileToParse.xml|inputDirectory> <outputDirectory> [parser.cfg]");
    }
    
    /**
     * @param args the command line arguments
     *
     * @since 1.0.0
     * @version 1.0.1
     */

    public static void main(String[] args) {

       //Define
       Options options = new Options();
       CommandLine cmd = null;
       String outputDirectory = null;   
       String inputFile = null;
       String parameterConfigFile = null;
       Boolean onlyExtractParameters = false;
       Boolean showHelpMessage = false;
       Boolean showVersion = false;
       Boolean attachMetaFields = false; //Attach mattachMetaFields FILENAME,DATETIME,NE_TECHNOLOGY,NE_VENDOR,NE_VERSION,NE_TYPE
       
       try{ 
            options.addOption( "p", "extract-parameters", false, "extract only the managed objects and parameters" );
            options.addOption( "v", "version", false, "display version" );
            options.addOption( "m", "meta-fields", false, "add meta fields to extracted parameters. FILENAME,DATETIME,NE_TECHNOLOGY,NE_VENDOR,NE_VERSION,NE_TYPE" );
            options.addOption( Option.builder("i")
                    .longOpt( "input-file" )
                    .desc( "input file or directory name")
                    .hasArg()
                    .argName( "INPUT_FILE" ).build());
            options.addOption(Option.builder("o")
                    .longOpt( "output-directory" )
                    .desc( "output directory name")
                    .hasArg()
                    .argName( "OUTPUT_DIRECTORY" ).build());
            options.addOption(Option.builder("c")
                    .longOpt( "parameter-config" )
                    .desc( "parameter configuration file")
                    .hasArg()
                    .argName( "PARAMETER_CONFIG" ).build() );
            options.addOption( "h", "help", false, "show help" );
            
            //Parse command line arguments
            CommandLineParser parser = new DefaultParser();
            cmd = parser.parse( options, args);

            if( cmd.hasOption("h")){
                showHelpMessage = true;
            }

            if( cmd.hasOption("v")){
                showVersion = true;
            }
            
            if(cmd.hasOption('o')){
                outputDirectory = cmd.getOptionValue("o"); 
            }
            
            if(cmd.hasOption('i')){
                inputFile = cmd.getOptionValue("i"); 
            }
            
            if(cmd.hasOption('c')){
                parameterConfigFile = cmd.getOptionValue("c"); 
            }
            
            if(cmd.hasOption('p')){
                onlyExtractParameters  = true;
            }
            
            if(cmd.hasOption('m')){
                attachMetaFields  = true;
            }
            
       }catch(IllegalArgumentException e){
           
       } catch (ParseException ex) {
//            java.util.logging.Logger.getLogger(HuaweiCMObjectParser.class.getName()).log(Level.SEVERE, null, ex);
        }
       
       
        try{
            
            if(showVersion == true ){
                System.out.println(VERSION);
                System.out.println("Copyright (c) 2018 Bodastage Solutions(http://www.bodastage.com)");
                System.exit(0);
            }
            
            //show help
            if( showHelpMessage == true || 
                inputFile == null || 
                ( outputDirectory == null && onlyExtractParameters == false) ){
                     HelpFormatter formatter = new HelpFormatter();
                     String header = "Parses Huawei GExport configuration data file XML to csv\n\n";
                     String footer = "\n";
                     footer += "Examples: \n";
                     footer += "java -jar boda-huaweicmobjectparser.jar -i Gexport_Dump.xml -o out_folder\n";
                     footer += "java -jar boda-huaweicmobjectparser.jar -i input_folder -o out_folder\n";
                     footer += "java -jar boda-huaweicmobjectparser.jar -i input_folder -p\n";
                     footer += "java -jar boda-huaweicmobjectparser.jar -i input_folder -p -m\n";
                     footer += "\nCopyright (c) 2018 Bodastage Solutions(http://www.bodastage.com)";
                     formatter.printHelp( "java -jar boda-huaweicmobjectparser.jar", header, options, footer );
                     System.exit(0);
            }
        
            //Confirm that the output directory is a directory and has write 
            //privileges
            if(outputDirectory != null ){
                File fOutputDir = new File(outputDirectory);
                if (!fOutputDir.isDirectory()) {
                    System.err.println("ERROR: The specified output directory is not a directory!.");
                    System.exit(1);
                }

                if (!fOutputDir.canWrite()) {
                    System.err.println("ERROR: Cannot write to output directory!");
                    System.exit(1);
                }
            }
            
            

            //Get parser instance
            HuaweiCMObjectParser cmParser = new HuaweiCMObjectParser();


            if(onlyExtractParameters == true ){
                cmParser.setExtractParametersOnly(true);
            }
            
            if( attachMetaFields == true ){
                cmParser.setExtractMetaFields(true);
            }
            
            if(  parameterConfigFile != null  ){
                File f = new File(parameterConfigFile);
                if(f.isFile()){
                    cmParser.setParameterFile(parameterConfigFile);
                    cmParser.getParametersToExtract(parameterConfigFile);
                }
            }

            
            cmParser.setDataSource(inputFile);
            if(outputDirectory != null ) cmParser.setOutputDirectory(outputDirectory);
            
            cmParser.parse();
        }catch(Exception e){
            System.out.println(e.getMessage());
            System.exit(1);
        }

    }
}
