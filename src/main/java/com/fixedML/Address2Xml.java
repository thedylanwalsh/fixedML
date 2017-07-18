/*  Copyright 2016 Dylan Walsh.

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.*/
package com.fixedML;

import javax.xml.transform.*;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.File;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Stack;
import javax.xml.transform.sax.TransformerHandler;

import static javax.xml.transform.OutputKeys.INDENT;
import static spark.Spark.*;

//TODO Here is an idea for a UI dev looking for an open source project:
//Make a HTML5 designer that takes a sample data file and colour highlights
//the fields so the alignment shows you've gotten the columns correct.
public class Address2Xml {
    public static void main(String[] args) {
        //Demo 1: By default, opening this URL in your browser will return XML:
        //http://localhost:4567/echoTextWrappedInTags?text=Hello%20World
        //Use view source in your browser to see the XML.
        get("/echoTextWrappedInTags", (req, res) ->
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                        + "<rootElement>"
                        + req.queryParams("text")
                        + "</rootElement>"
        );

        //Demo 2: convert arbitrary XML to JSON using XSLT 1.0. Launch and open this URL in your browser:
        //http://localhost:4567/xml2json?xml=<positions><point3d><x>1</x><y>1</y><z>1</z></point3d><point3d><x>2</x><y>2</y><z>2</z></point3d></positions>
        //Use view source in your browser to see the JSON.
        get("/xml2json", (req, res) ->
                transform(req.queryParams("xml"), new File("src/main/resources/xml2json.xslt"))
        );

        //Demo 3: convert addresses XML to a format similar to ISO 20022 XML.
        get("/addressxml2iso", (req, res) ->
                transform(req.queryParams("xml"), new File("src/main/resources/addressxml2iso.xslt"))
        );

        //Demo 3: convert addresses in fixed width format to a format similar to ISO 20022 XML.
        get("/addressfixedwidth2iso", (req, res) ->
                {
                    try {
                        return transformTextToXml(req.queryParams("text"), new File("src/main/resources/addressxml2iso.xslt"));
                    } catch (Throwable t) {
                        return t.toString();
                    }
                }
        );

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

        //Demo 5: convert addresses in fixed width format to JSON.
        get("/addressfixedwidth2json", (req, res) ->
                {
                    try {
                        return transformTextToXml(req.queryParams("text"), new File("src/main/resources/xml2json.xslt"));
                    } catch (Throwable t) {
                        return t.toString();
                    }
                }
        );

        //Demo 6: convert addresses XML to fixed format text.
        //http://localhost:4567/addressxml2fixedwidth?xml=
        //followed by your XML. Use view source in your browser to see the fixed-width data. If you are using
        //Google chrome, view source is not ideal as it does rawLine-wrapping - instead right-click on the browser
        //view of the data and use the 'Inspect' command, then click on the 'Sources' tab.
        //URL escaping for XML is awkward. There is an encoder here: http://meyerweb.com/eric/tools/dencoder/
        get("/addressxml2fixedwidth", (req, res) ->
                transform(req.queryParams("xml"),
                        new File("src/main/resources/addressxml2fixedwidth.xslt"))
        );
    }

    private static String transformTextToXml(String text, File xslt) {
        TransformerHandler handler;
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
}
