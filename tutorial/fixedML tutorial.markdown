# fixedML
Process legacy data formats in modern ways - XML transformation without source XML

COPYRIGHT 2016, 2017 Dylan Walsh. Code is available at https://github.com/thedylanwalsh/fixedML

# fixedML - RESTful XML transformation with non-XML sources.

## How a programmer may process legacy data formats in a modern way.
Legacy code is code that passed through acceptance testing and provides measurable value to the
client in the production environment.

In dealing with a legacy data format, the programmer may wish the data were in a modern format such as XML or JSON.
It is possible to combine the raw legacy data formats with modern techniques. This supports unit testing, continuous
integration, software quality and programmer serenity.

## This tutorials scope.

The range of topics in this document is large. The following topics are raised here, but will not be demonstrated or coded until a follow up is written:

- concurrent and serial Java 8 map-reduce code samples.
    - code to auto-generate gigabytes of test data to benchmark both approaches.

## What we are going to do today.
This tutorial demonstrates an approach to data integration, providing XML output or transformation from legacy data
sources. While it has the potential to become a framework or a library, it is presented here as a tutorial with sample
code.

To understand this tutorial, the reader needs some familiarity with:
- Java 8
- XML fundamentals, such as well-formedness, and the XML infoset (elements, attributes, text nodes, namespaces etc.)

To apply this approach, the reader also needs skills in:
- XSLT 1.0. Later versions of XSLT (or alternatively XQuery) can be substituted if a relevant library is being used,
such as Saxon by Saxonica.
- XML parsing using event driven or streaming APIs such as SAX ('Simple API for XML'.)

*The design principles presented here are applicable to other platforms such as .NET.*

# History and objective of this approach.
In 2007 I developed a Java framework for interfacing data between text formats (fixed width and delimited), a relational
database and SOAP web services, for a household-name client. One application of that framework was converting very
complex hierarchical fixed width files to and from various XML schemas for use by a partner company.

XML output was generated from the input fixed width
files without the overhead of creating intermediary XML files in memory or on disk.

I have been informed recently that this code is still in production. The idea for this tutorial came from ruminating
about how I would build it today, and to utilise specific Java 8 features in depth.



|Solution|2007| fixedML 2016|
|---|---|---|
|Java|5|8|
|Wiring of objects|Spring framework |There are only two classes, with a direct dependency between the web server class and the XML generator.|
|File format specification|Custom XML specification format with its own parser.|Specified in Java code with a fluent API.
|Execution|Batch|RESTful web services|
|Container|Spring IoC invoked on command line, proprietary interface framework.|None - micro webserver used as library, no inversion of control.|

# Let's get a live web service running as early as possible!
- start a web server.
- return the request text wrapped in an element called `<rootElement>`
- open in browser.

Here is the Java:

    package com.fixedML;

    import static spark.Spark.*;

    public class Address2Xml {
        public static void main(String[] args) {
            spark.Spark.staticFileLocation("/web");

            //Demo 1: By default, opening this URL in your browser will return XML:
            //http://localhost:4567/echoTextWrappedInTags?text=Hello%20World
            //Use view source in your browser to see the XML.
            get("/echoTextWrappedInTags", (req, res) ->
                    "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                            + "<rootElement>"
                            + req.queryParams("text")
                            + "</rootElement>"
            );
        }
    }

The XML declaration, encoding, and root element are not required for an XML document, but are included to
look like typical XML.

Using HTTP request parameters for huge data files would be unwise in production. A more realistic scenario is a batch conversion of
files. If the output format is much smaller than the input files (as it was in my real world project) then a compromise
approach would be viable where a RESTful web service transform big files on the filesystem into outputs returned by HTTP.

# What is XSL Transformation, why use it, and why use it without a source document?
In 1998, the W3C initiated a project to define technology for presenting XML - XML Stylesheet Language. This has
two parts - Transformations and Formatting Objects. The transformation language became incredibly successful outside of
its original scope, and was adopted by developers for XML to XML transformation.

|input|transformation|output|
|---|---|---|
|XML|➡ XSLT ➡ |Unicode|
|Conceptually, a tree of nodes.| Rules which select nodes in the XML, and apply a template to generate output.| XML, HTML or text.|

The relative novelty being presented here is to supply a tree of nodes (in the form of XML parsing events) as the input
to the XSLT, without there being any XML (literally any serialized XML) because we are parsing a flat text format.

## XSLT 3.0 and Streaming
XSLT code can make reference to prior nodes in the document when processing the current node. Ergo, XSLT implementations
maintain an internal object model of the source data, in memory. XSLT 3.0 introduced a streaming feature, where the
code can be written in a streaming manner. This would remove the need to store a representation of source data in
memory.

The benefit of this would be to alleviate memory issues when processing humongous inputs. XSLT 3.0 is not supported in
JDK 8, but is available in third party libraries. There are alternative approaches, such as processing the source data
in appropriate chunks.

# How about doing something with XSLT in our web service now?

Here is an existing transformation to create JSON from arbitrary XML:
https://github.com/doekman/xml2json-xslt/blob/master/xml2json.xslt

It is in the classpath in the resources directory of the code bundle.

We need to add a transform utility method to Address2Xml.java:

    private static String transform(String xml, File transformFile) throws TransformerException {
        try {
            TransformerFactory factory = TransformerFactory.newInstance();
            Source xslt = new StreamSource(transformFile);
            Transformer transformer = factory.newTransformer(xslt);
            Source input = new StreamSource(new StringReader(xml));
            StringWriter stringWriter = new StringWriter();
            transformer.transform(input, new StreamResult(stringWriter));
            return stringWriter.toString();
        } catch(Throwable t) {
            //for tutorial just return any errors.
            return t.toString();
        }
    }

Now we can add it to the web server as a new operation:

        //Demo 2: convert arbitrary XML to JSON using XSLT 1.0. Launch and open this URL in your browser:
        //http://localhost:4567/xml2json?xml=<positions><point3d><x>1</x><y>1</y><z>1</z></point3d><point3d><x>2</x><y>2</y><z>2</z></point3d></positions>
        //Use view source in your browser to see the JSON.
        get("/xml2json", (req, res) ->
                transform(req.queryParams("xml"), new File("src/main/resources/xml2json.xslt"))
        );

Invoking that web service with this XML:

    <positions>
           <point3d><x>1</x><y>1</y><z>1</z></point3d>
           <point3d><x>2</x><y>2</y><z>2</z></point3d>
    </positions>`


... returns the following:

    {"positions":[{"x":1,"y":1,"z":1},{"x":2,"y":2,"z":2}]}

 The XSLT library we chose does not name the point3d nodes, but represents them as array items.

# Production code versus a breezy tutorial - risk mitigation.

The following concerns are not addressed in this tutorial but need to be
for mission-critical production code.

## Unit testing and automated integration testing
... should be mandatory on serious projects. Nothing to
 add to that topic in this tutorial.

## Validation of input and output
The input fixed width or delimited data may not comply with the relevant
specifications. This can cause unexpected behaviour downstream.

W3C XML Schema is an excellent tool for validation and for automated
integration testing. It has a comprehensive data type system.
Once you have basic code for the implied XML schema
for the source data, you can write a schema to validate it using XML
Schema.

After we have defined our fixed width input format, we will
revisit this topic under testing and validation in the next article.

## Security
The sample code does not validate inputs. Black hats and white hats
would have a field day exploring the various injection possibilities if
you ran that publicly.

Fortunately, this is just a tutorial running on your MacBook. The
first piece code uses string concatenation to generate XML.
Enterprise-quality code should use XML APIs to create the XML document for conformance and security.

## Mutual validation of data exchanged with 3rd parties
This minimises finger pointing between teams and organizations during integration testing.

## Logging
Logging the work to be done at the outset, key stages thereafter and the amount of items processed at the end of a
business critical job, will prove invaluable in a production crisis.

# Background - What are flat, fixed width and delimited file formats?

Flat files is a term typically used to refer to text files (e.g ASCII or
Unicode) containing structured data.

Delimited is the most familiar type to consumers - in the form of CSV
files used with spreadsheets. CSV stands for 'comma separated values.' However
the same approach can be used with other delimiter characters, such as bars
('|'). Hence 'delimited' is the general term for similar file formats regardless
of the delimiter used. Delimited files are variable width.

Fixed width files allocate a fixed amount of space in the file for every field,
regardless of whether the field is being used in a record or the length of the
item. The remainder is typically padded with spaces. So if there is a name field
of twenty characters length, John's name will appear as (using full stops for readability to mark where spaces would be):

    'John................'

John may find this satisfactory, but Srimadaddankithirumalavaraahavenkatathaa may deem it
inadequate. The advantages of fixed width include the fact that COBOL coders
like it and unlike delimited files, it does not require you to escape a delimiter character.

## This tutorial focuses on fixed width rather than delimited formats
Formats like CSV are easier to process - various approaches like REGEX can split CSV lines, Java 8 stream tutorials typically use it as an example.
The main gotcha is escaping the delimiter characters, which isn't an issue in many applications where the delimiter character never occurs in the data.

## The term 'flat files' is often a misnomer.
A programmer may think of flat meaning a two dimensional array of items. In
reality, many 'flat' file formats have multiple record types, metadata and
may even be hierarchical. This is not flat data.

Consider a simple CSV file. They often include a header row with the names of the columns. That row
is different from the others and provides metadata. There may be a header or footer with record counts, the creation data, checksums or other data common to the whole file.
Those records have completely different numbers of columns
and data types from the main data. The next level of complexity is where there are multiple rows of different types.

Eventually you will encounter deeply hierarchical delimited or fixed width files, where the vendor ought to have
considered XML or JSON.

## Fixed width files may be sparsely populated.
We can see the fixed width name example does not use space efficiently:

    'John................'

Imagine a middle name field, where the application does not have middle names for 98% of the customers. A design with
many many more optional fields will inevitably be 90% whitespace or worse.

In such cases, conversion to XML may actually compress the data by a multiple e.g. 10 times.

# Example input - a hierarchical fixed width data format for postal addresses.

The fixed with data format is defined with a table of records, fields, positions, data types and field names.

## Format specification

|record id|field name|field column|length|data type|
|---|---|---|---|---|
|000|people|0|0|start|
|001|personCount|3|10|integer|
|001|creationDate|13|10|date|
|100|person|0|0|start|
|101|personId|3|10|integer|
|101|firstName|13|30|string|
|101|lastName|43|30|string|
|200|addresses|0|0|start|
|201|addressId|3|10|integer|
|201|line1|23|50|string|
|201|line|2|73|50|string|
|201|line3|1|23|50|string|
|201|line4|173|50|string|
|201|line5|2|23|50|string|
|201|city|273|30|string|
|201|county|303|30|string|
|201|state|333|30|string|
|201|countryIso|343|2|string|
|201|postalCode|345|30|string|
|201|postalCodeType|375|10|enum{ZIP,POSTCODE,EIRCODE}|
|299|addresses|3|0|end|
301|phoneNumber|3|20|string|
301|phoneNumberType|23|6|enum{HOME,WORK,MOBILE}|
199|person|0|0|end|
999|people|0|0|end|

Each rawLine starts with three-digit record type. This is not a flat file, and the real-world format that inspired it is deeply hierarchical.
This would make it an ideal candidate for XML or even JSON, but fixed width was chosen instead by a software vendor.

We have a root record 'persons'. This is for convenience - your real world problem may require you to wrap it in one in the parsing code to comply with XML rules. The persons record starts with a 000 record and ends
with a 999. It contains a 001 record with the file number of person records and the creationDate.  This is simplified from a real world example. The key point is there are two container records - person and addresses. A person record hold an addresses record and multiple phoneNumber records. It begins with a 100 start record and ends with a 199 end record. An addresses record contains multiple atomic address records.  It begins with a 100 start record and ends with a 199 end record. The person also holds atomic phoneNumber records (type 301).

## Example data.

    000
    001         22016-07-09
    100
    1010000000001                       Richard                         Nixon
    200
    2010000000001                                              1600                               Pennsylvania Ave NW                                                                                                                                                                          Washington                                                          DCUS                      DC 20500       ZIP
    299
    301     +1 202-456-1111  WORK
    199
    100
    1010000000002                         Elvis                       Presley
    200
    2010000000002                                         Graceland                                Elvis Presley Blvd                                                                                                                                                                             Memphis                        Shelby                            TNUS                      TN 38116       ZIP
    299
    301     +1 901-332-3322  HOME
    199
    999

This fictional example is relatively simple. However you will still see how the fixed width format has sparsely populated data, and as a consequence, massive redundant whitespace. I have seen formats where lines could be 2000 characters long. In those extreme cases, the XML version of the data can be five to ten times shorter.

# The XML-less XML transformation.

|Approach|Input|Parsing|Transformation|Output|
|---|---|---|---|---|
|Conventional|XML document (text)|XML parser generates nodes held by transformer.|XSL Transformation|XML, HTML, other Unicode such as JSON.|
|fixedML|fixed or delimited text|fixedML parser generates nodes for the 'implied XML schema', held by transformer.|XSL Transformation|XML, HTML, other Unicode such as JSON.|

The conventional approach means you need XML documents to use XML techniques. If your data is not XML, you have to parse twice e.g. parse the fixed width file and convert to XML, then parse the XML. fixedML removes the need for XML documents, the transformer process a virtual XML document. The structure of that XML is the 'implied XML schema.'


# The implied XML schema of the input.

What is a reasonable design for XML to represent the addresses data model? There is no single correct answer, but most
sensible XML programmers will come up with something similar, with different in choices about using elements vs.
attributes, container elements etc. Here I have chosen only to use elements and not to introduce container elements
around the repeating `address` or `phone` elements.

The same data in XML format:

    <people>
        <personCount>2</personCount>
        <creationDate>2016-07-09</creationDate>
        <person>
            <personId>0000000001</personId>
            <firstName>Richard</firstName>
            <lastName>Nixon</lastName>
            <address>
                <addressId>0000000001</addressId>
                <line1>1600</line1>
                <line2>Pennsylvania Ave NW</line2>
                <line3></line3>
                <line4></line4>
                <line5></line5>
                <city>Washington</city>
                <county></county>
                <state>DC</state>
                <countryIso>US</countryIso>
                <postalCode>DC 20500</postalCode>
                <postalCodeType>ZIP</postalCodeType>
            </address>
            <phone>
                <phoneNumber>+1 202-456-1111</phoneNumber>
                <phoneNumberType>WORK</phoneNumberType>
            </phone>
        </person>
        <person>
            <personId>0000000002</personId>
            <firstName>Elvis</firstName>
            <lastName>Presley</lastName>
            <address>
                <addressId>0000000002</addressId>
                <line1>Graceland</line1>
                <line2>Elvis Presley Blvd</line2>
                <line3></line3>
                <line4></line4>
                <line5></line5>
                <city>Memphis</city>
                <county>Shelby</county>
                <state>TN</state>
                <countryIso>US</countryIso>
                <postalCode>TN 38116</postalCode>
                <postalCodeType>ZIP</postalCodeType>
            </address>
            <phone>
                <phoneNumber>+1 901-332-3322</phoneNumber>
                <phoneNumberType>HOME</phoneNumberType>
            </phone>
        </person>
    </people>

# The desired output XML schema.
We will use a subset of ISO 200022 XML, it doesn't get more enterprise-level or real world than that.
ISO 20022 was created by the International Standards Organization for electronic data interchange between financial
institutions. It is a big deal in banking.

The official schema file is called `pain.001.001.07.xsd`, but here `pain` stands for 'payments in.' For this tutorial
we only have name, address and postal information, so we will just create ISO creditor records for our `person` records in
the source data.

# The transformation.
The code bundle for this tutorial includes the file `addressxml2fixedwidth.xslt`, which outputs the following partial
ISO 20022 XML:

    <?xml version="1.0" encoding="UTF-8"?>
    <dw:Cdtrs xmlns:dw="http://dylanwalsh.net/fixedML" xmlns="urn:iso:std:iso:20022:tech:xsd:pain.001.001.07">
        <Cdtr>
            <Nm>Richard Nixon</Nm>
            <PstlAdr>
                <StrtNm>Pennsylvania Ave NW</StrtNm>
                <BldgNb>1600</BldgNb>
                <PstCd>DC 20500</PstCd>
                <TwnNm>Washington</TwnNm>
                <Ctry>US</Ctry>
            </PstlAdr>
            <CtctDtls>
                <PhneNb>+1 202-456-1111WORK</PhneNb>
            </CtctDtls>
        </Cdtr>
        <Cdtr>
            <Nm>Elvis Presley</Nm>
            <PstlAdr>
                <StrtNm>Elvis Presley Blvd</StrtNm>
                <BldgNb>Graceland</BldgNb>
                <PstCd>TN 38116</PstCd>
                <TwnNm>Memphis</TwnNm>
                <Ctry>US</Ctry>
            </PstlAdr>
            <CtctDtls>
                <PhneNb>+1 901-332-3322HOME</PhneNb>
            </CtctDtls>
        </Cdtr>
    </dw:Cdtrs>

# Hold up! We do not have input XML - the input data is fixed width.
Time to combine XSLT with a handler that fires SAX XML parsing events in response to fixed with data nodes:


    private static String transformTextToXml(String text, File xslt) {
        TransformerHandler handler = null;
        SAXTransformerFactory tf = (SAXTransformerFactory)
                SAXTransformerFactory.newInstance();
        try {
            Source xsltSource = new StreamSource(xslt);
            handler = tf.newTransformerHandler(xsltSource);
            handler.getTransformer().setOutputProperty(INDENT, "yes");
        } catch (TransformerConfigurationException e) {
            throw new RuntimeException("Exception occurred creating transformer or handler.", e);
        }

        StringWriter stringWriter = new StringWriter();
        handler.setResult(new StreamResult(stringWriter));

This first part hooks up a transformation to the handler we need to feed events to.

The next part is splitting and parsing the fixed width lines. A REGEX will take care of the former.

The LineXml class is the only framework we have or need so far. It encapsulates one line of data and the SAX (JAXP) handler. It is written in a fluent API style - a little like StringBuilder in that the methods return itself and allow chaining.

This first approach uses Java 7 switch-on-strings:

        String[] lines = text.split("\\r?\\n");
        Stack<String> elementStack = new Stack<>();

        for (String rawLine : lines) {
            LineXml line = new LineXml(rawLine, handler, elementStack);

            String type = line.type().intern();

            switch (type) {
                case "000":
                    line.startRoot("people");
                    break;
                case "001":
                    line.start("personCount").trimmedText(10).end()
                            .start("creationDate").text(10).end();
                    break;
                case "100":
                    line.start("person");
                    break;
                case "101":
                    line.start("personId").trimmedText(10).end()
                            .start("firstName").trimmedText(30).end()
                            .start("lastName").trimmedText(30).end();
                    break;
                //case "200": - we don't need an 'addresses' container element.
                case "201":
                    line.start("address")
                            .start("addressId").trimmedText(10).end()
                            .start("line1").trimmedText(50).end().start("line2").trimmedText(50).end()
                            .start("line3").trimmedText(50).end().start("line4").trimmedText(50).end()
                            .start("line5").trimmedText(50).end().start("city").trimmedText(30).end()
                            .start("county").trimmedText(30).end().start("state").trimmedText(30).end()
                            .start("countryIso").text(2).end()
                            .start("postalCode").text(30).end().start("postalCodeType").text(10).end()
                            .end();
                    break;
                //case "299": : - we don't need an 'addresses' container element.
                case "301":
                    line.start("phone")
                            .start("phoneNumber").trimmedText(20).end()
                            .start("phoneNumberType").trimmedText(6).end()
                            .end();
                    break;
                case "199":
                    line.end();
                    break;
                case "999":
                    line.endRoot();
            }
        }

        return stringWriter.toString();
    }

All that remains is to add a method to create the web service in the 'main()' method.

# Transform to HTML

        //Demo 4: convert addresses in fixed width format to HTML.
        get("/addressfixedwidth2html", (req, res) ->
                {
                    try {
                        return transformTextToXml(req.queryParams("text"), new File("src/main/resources/addressxml2html.xslt"));
                    } catch (Throwable t) {
                        return t.toString();
                    }
                }
        );
        
Here is the first view of some XSL tranformation code. There is more in the code bundle.

    <xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
        <xsl:output method="html" indent="yes" doctype-system="about:legacy-compat"/>
        <xsl:strip-space elements="*"/>
    
        <xsl:template match="/people">
            <html>
                <head><title>fixedML - fixed width - 2 records as XHTML using XSLT.</title></head>
                <body><xsl:apply-templates select="person"/></body>
            </html>
        </xsl:template>
    
        <xsl:template match="person">
            <div id="person_{personId}">
                <div class="name"><xsl:value-of select="firstName"/>&#xA0;<xsl:value-of select="lastName"/></div>
                <xsl:for-each select="phone">
                    <div class="phone"><xsl:value-of select="phoneNumber"/></div>
                </xsl:for-each>
                <xsl:for-each select="address">
                    <div class="addressline"><xsl:value-of select="line1"/></div>
                    <div class="addressline"><xsl:value-of select="line2"/></div>
                    <div class="addressline"><xsl:value-of select="line3"/></div>
                    <div class="addressline"><xsl:value-of select="line4"/></div>
                    <div class="addressline"><xsl:value-of select="line5"/></div>
                    <div class="postalCode"><xsl:value-of select="postalCode"/></div>
                    <div class="city"><xsl:value-of select="city"/></div>
                    <div class="countryIso"><xsl:value-of select="countryIso"/></div>
                </xsl:for-each>
            </div>
        </xsl:template>
    </xsl:stylesheet>

Here is the HTML output:

    <!DOCTYPE html SYSTEM "about:legacy-compat">
    <html>
        <head>
            <META http-equiv="Content-Type" content="text/html; charset=UTF-8">
            <title>fixedML - fixed width - 2 records as XHTML using XSLT.</title>
        </head>
        <body>
            <div id="person_0000000001">
                <div class="name">Richard&nbsp;Nixon</div>
                <div class="phone">+1 202-456-1111</div>
                <div class="addressline">1600</div>
                <div class="addressline">Pennsylvania Ave NW</div>
                <div class="addressline"></div>
                <div class="addressline"></div>
                <div class="addressline"></div>
                <div class="postalCode">DC 20500</div>
                <div classl="city">Washington</div>
                <div class="countryIso">US</div>
            </div>
            <div id="person_0000000002">
                <div class="name">Elvis&nbsp;Presley</div>
                <div class="phone">+1 901-332-3322</div>
                <div class="addressline">Graceland</div>
                <div class="addressline">Elvis Presley Blvd</div>
                <div class="addressline"></div>
                <div class="addressline"></div>
                <div class="addressline"></div>
                <div class="postalCode">TN 38116</div>
                <div class="city">Memphis</div>
                <div class="countryIso">US</div>
            </div>
        </body>
    </html>     

Now it is easy to read.

Note: With certain browsers, it is possible to perform the transform client-side, thereby preserving server resources.

# Transform to JSON

        get("/addressfixedwidth2json", (req, res) ->
                {
                    try {
                        return transformTextToXml(req.queryParams("text"), new File("src/main/resources/xml2json.xslt"));
                    } catch (Throwable t) {
                        return t.toString();
                    }
                }
        );
    
Here is the output (formatted for readibility):
        
    {
      "people": {
        "personCount": 2,
        "creationDate": "2016-07-09",
        "person": {
          "personId": "0000000001",
          "firstName": "Richard",
          "lastName": "Nixon",
          "address": {
            "addressId": "0000000001",
            "line1": 1600,
            "line2": "Pennsylvania Ave NW",
            "line3": null,
            "line4": null,
            "line5": null,
            "city": "Washington",
            "county": null,
            "state": "DC",
            "countryIso": "US",
            "postalCode": "DC 20500",
            "postalCodeType": "ZIP"
          },
          "phone": {
            "phoneNumber": "+1 202-456-1111",
            "phoneNumberType": "WORK"
          }
        },
        "person": {
          "personId": "0000000002",
          "firstName": "Elvis",
          "lastName": "Presley",
          "address": {
            "addressId": "0000000002",
            "line1": "Graceland",
            "line2": "Elvis Presley Blvd",
            "line3": null,
            "line4": null,
            "line5": null,
            "city": "Memphis",
            "county": "Shelby",
            "state": "TN",
            "countryIso": "US",
            "postalCode": "TN 38116",
            "postalCodeType": "ZIP"
          },
          "phone": {
            "phoneNumber": "+1 901-332-3322",
            "phoneNumberType": "HOME"
          }
        }
      }
    }

JSON is ideal for client side Javascript consumption.

# To develop this idea into a framework
Some ideas of features that build on the ideas present so far:

- Changes in input fixed width data due to bugs or undocumented changes can throw off the alignment in the parsing. This is a shortcoming of fixed width formats. Mitigation: Ease the use of XML schema to 'sanity' check the inputs. Support easy validation and the generation of the 'implied XML schema' from the parsing code.
- Support more of the XML infoset e.g. attributes, namespaces etc. Some users may not then require any XSL transformation, when the project they are in can except a new XML format rather than a predefined one.
- Beef up exception handling.
- Logging.
- Support and encourage automated integration testing and unit testing by providing mock objects and other supporting test tools.
- Built-in general transformations to JSON, HTML, CSV.

The most ambitious goal would be to evolve this to the point where it would provide a programmer-centric alternative to monolithic and proprietary ETL (Extract, Transform and Load), EAI (Enterprise Application Integration) or ESB (Enterprise Service Bus) products. This would be analogous to the relationship of micro web frameworks (such as the Java Spark library used in this tutorial) to heavyweight Enterprise Java servers.

## Map Reduce
Map Reduce is a strategy to process large amounts of work in parallel, even on separate machines (a cluster).

There is no guarantee such an approach will yield better performance depending on the input/output infrastructure
(parsing input fixed width data will likely be I/O intensive, not CPU intensive.)

There are limits to parallelization and XML parsing or transformation. The order of nodes is significant at a syntax level in XML - this is 'document order.' The good news is that it may not matter in specific parts of your XML application. For example, in our addresses XML format, the order of the persons is not significant, and neither is the order of their addresses. Each person has a primary key, each address has a primary key. Therefore there are two levels at which we may divide the data into chunks, process them in parallel and collate the individual results. In more complex real-world examples, the parallel chunks can be (and were in my experience) much larger and hence even more amenable to this approach.

Research in this area will be the next step.