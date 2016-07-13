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

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

import javax.xml.transform.sax.TransformerHandler;
import java.util.Stack;

/** Represents a  line in a specific fixed-width data format. The line encapsulates an XML event handler, and can parse
 * the under lying text data as XML events.*/
public class LineXml {
    private TransformerHandler handler;
    private String rawLine;
    private int pos;
    private Stack<String> elementStack;

    private static final Attributes NO_ATTRIBUTES = new AttributesImpl();

    public LineXml(String rawLine, TransformerHandler handler, Stack<String> elementStack) {
        this.handler = handler;
        this.rawLine = rawLine;
        this.elementStack = elementStack;
    }

    public String type() {
        if(this.pos < 3) this.pos = 3;
        return this.rawLine.substring(0, 3);
    }

    public LineXml trimmedText(int length) {
        return this.text(length, true);
    }

    public LineXml text(int length) {

        return this.text(length, false);
    }

    private LineXml text(int length, boolean trim) {
        String text = this.rawLine.substring(this.pos, this.pos + length).trim();

        try {
            this.handler.characters(text.toCharArray(), 0, text.length());
        } catch (SAXException e) {
            throw new RuntimeException("LineXml cannot start new element.", e);
        }

        this.pos += length;
        return this;
    }

    public LineXml start(String name) {
        this.elementStack.push(name);

        try {
            this.handler.startElement("", name, name, NO_ATTRIBUTES);
        } catch (SAXException e) {
            throw new RuntimeException("LineXml cannot start new element.", e);
        }

        return this;
    }

    public LineXml startRoot(String name) {
        try {
            handler.startDocument();
            this.start(name);
        } catch (SAXException e) {
            throw new RuntimeException("Cannot start document or create root.", e);
        }

        return this;
    }

    public LineXml end() {
        String name = elementStack.pop();

        try {
            handler.endElement("", name, name);
        } catch (SAXException e) {
            throw new RuntimeException("Cannot end element.", e);
        }

        return this;
    }

    public LineXml endRoot() {
        this.end();

        try {
            handler.endDocument();
        } catch (SAXException e) {
            throw new RuntimeException("Cannot end doc.", e);
        }

        return this;
    }
}
