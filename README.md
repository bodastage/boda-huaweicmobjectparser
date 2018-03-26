![Build status](https://travis-ci.org/bodastage/boda-huaweicmobjectparser.svg?branch=master)

# boda-huaweicmobjectparser
Parses Huawei GExport configuration data file XML to csv. It parsers 2G,3G,and 4G configuration management XML files.

Below is the expected format of the input file:

```XML
<?xml version="1.0" encoding="utf-8"?>
<bulkCmConfigDataFile>
    <fileHeader/>
    <configData>
        <class name="BSC6900GSM">
            <object technique="TECHNOLOGY" vendor="Huawei" version="VERSION">
                <class name="MONAME_BSC6900GSM">
                    <object>
                        <parameter name="PARAMETER1" value="VALUE1"/>
                        <parameter name="PARAMETER2" value="VALUE2"/>
                        <parameter name="PARAMETERN" value="VALUEN"/>
                    </object>
                    <object>
			<!-- ... -->
                    </object>
		</class>
		<!-- ... -->
	    </object>
	</class>
    </configData>
</bulkCmConfigDataFile>
```
# Usage
java -jar  huaweicmobjectparser.jar data.xml outputDirectory

# Download and installation
The lastest compiled jar file is availabled in the dist directory. Alternatively, download it directly from [here](https://github.com/bodastage/boda-huaweicmobjectparser/raw/master/dist/boda-huaweicmobjectparser.jar).

# Requirements
To run the jar file, you need Java version 1.6 and above.

# Getting help
To report issues with the application or request new features use the issue [tracker](https://github.com/bodastage/boda-huaweicmobjectparser/issues). For help and customizations send an email to info@bodastage.com.

# Credits
[Bodastage](http://www.bodastage.com) - info@bodastage.com

# Contact
For any other concerns apart from issues and feature requests, send an email to info@bodastage.com.

# Licence
This project is licensed under the Apache 2.0 licence.  See LICENCE file for details.
