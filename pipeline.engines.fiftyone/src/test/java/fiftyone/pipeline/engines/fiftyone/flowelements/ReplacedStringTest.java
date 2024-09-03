/* *********************************************************************
 * This Original Work is copyright of 51 Degrees Mobile Experts Limited.
 * Copyright 2023 51 Degrees Mobile Experts Limited, Davidson House,
 * Forbury Square, Reading, Berkshire, United Kingdom RG1 3EU.
 *
 * This Original Work is licensed under the European Union Public Licence
 * (EUPL) v.1.2 and is subject to its terms as set out below.
 *
 * If a copy of the EUPL was not distributed with this file, You can obtain
 * one at https://opensource.org/licenses/EUPL-1.2.
 *
 * The 'Compatible Licences' set out in the Appendix to the EUPL (as may be
 * amended by the European Commission) shall be deemed incompatible for
 * the purposes of the Work and the provisions of the compatibility
 * clause in Article 5 of the EUPL shall not apply.
 *
 * If using the Work as, or as part of, a network application, by
 * including the attribution notice(s) required under Article 5 of the EUPL
 * in the end user terms of the application under an appropriate heading,
 * such notice(s) shall fulfill the requirements of that article.
 * ********************************************************************* */

package fiftyone.pipeline.engines.fiftyone.flowelements;

import fiftyone.pipeline.engines.fiftyone.flowelements.ShareUsageElement.ReplacedString;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import javax.xml.stream.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.joining;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the proprietary evidence string treatment class ...
 */
class ReplacedStringTest {

    // is transparent to legal characters
    @Test
    public void testNormal(){
        String aString = IntStream.of(0x9, 0xA, 0xD)
                .mapToObj(i -> Character.toString((char)i))
                .collect(joining());
        ReplacedString replacedString = new ReplacedString(aString);
        assertFalse(replacedString.isTruncated());
        assertTrue(replacedString.isReplaced());
        assertEquals(3, replacedString.toString().length());
    }
    // is transparent to legal characters except \
    @Test
    public void testNormal2(){
        String aString =
                IntStream.range(0x20, 0x7f)
                        .mapToObj(i -> Character.toString((char)i))
                        .collect(joining());
        ReplacedString replacedString = new ReplacedString(aString);
        assertFalse(replacedString.isTruncated());
        assertFalse(replacedString.isReplaced());
        assertEquals(0x5f, replacedString.toString().length());
        assertEquals(aString, replacedString.toString());
    }
    // works with empty string
    @Test
    public void testEmpty(){
        String aString = "";
        ReplacedString replacedString = new ReplacedString(aString);
        assertFalse(replacedString.isTruncated());
        assertFalse(replacedString.isReplaced());
        assertEquals(0, replacedString.toString().length());
        assertEquals(aString, replacedString.toString());
    }
    // works with null string
    @Test
    public void testNull(){
        String aString = null;
        ReplacedString escapedString = new ReplacedString(aString);
        assertFalse(escapedString.isTruncated());
        assertFalse(escapedString.isReplaced());
        assertEquals(0, escapedString.toString().length());
        assertEquals("", escapedString.toString());
    }
    // truncates longer than 512
    @Test
    public void testTruncate(){
        String aString = IntStream.range(0,513).mapToObj(i -> "a").collect(joining());
        ReplacedString replacedString = new ReplacedString(aString);
        assertTrue(replacedString.isTruncated());
        assertFalse(replacedString.isReplaced());
        assertEquals(512, replacedString.toString().length());
    }
    // escapes single digit Hex
    @Test
    public void testEscapedSmall(){
        String aString = IntStream.range(0,5).mapToObj(i -> "\u0001").collect(joining());
        ReplacedString replacedString = new ReplacedString(aString);
        assertFalse(replacedString.isTruncated());
        assertTrue(replacedString.isReplaced());
        System.out.println(replacedString);
        assertEquals(5, replacedString.toString().length());
    }
    // escapes 2 digit hex
    @Test
    public void testEscapedBigger(){
        String aString = IntStream.range(0,5).mapToObj(i -> "\u001F").collect(joining());
        ReplacedString replacedString = new ReplacedString(aString);
        assertFalse(replacedString.isTruncated());
        assertTrue(replacedString.isReplaced());
        System.out.println(replacedString);
        assertEquals(5, replacedString.toString().length());
    }
    // does not escape valid chars
    @Test
    public void testEscapedBiggerStill(){
        String aString = IntStream.range(0,5).mapToObj(i -> "\u0020").collect(joining());
        ReplacedString replacedString = new ReplacedString(aString);
        assertFalse(replacedString.isTruncated());
        assertFalse(replacedString.isReplaced());
        System.out.println(replacedString);
        assertEquals(5, replacedString.toString().length());
    }

    @Test ()
    public void testEscapedEvenBigger(){
        String aString = IntStream.range(0,5).mapToObj(i -> "\u0100").collect(joining());
        ReplacedString replacedString = new ReplacedString(aString);
        assertFalse(replacedString.isTruncated());
        assertTrue(replacedString.isReplaced());
        System.out.println(replacedString);
        assertEquals(5, replacedString.toString().length());

    }

    private final XMLOutputFactory xmlOutputFactory = XMLOutputFactory.newInstance();
    private final XMLInputFactory xmlInputFactory =XMLInputFactory.newInstance();

    @Test
    public void testXmlSerialise() throws XMLStreamException {
        xmlOutputFactory.setProperty("escapeCharacters", true);
        String text = ReplacedString.VALID_XML_CHARS.stream()
                .map(i->Character.toString((char)i.intValue()))
                .collect(joining());
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        XMLStreamWriter writer = xmlOutputFactory.createXMLStreamWriter(os, "UTF-8");
        writer.writeStartDocument("UTF-8", "1.1");
        writer.writeStartElement("test");
        writer.writeCharacters(text);
        writer.writeEndElement();
        writer.close();

        ByteArrayInputStream is = new ByteArrayInputStream(os.toByteArray());
        XMLStreamReader reader = xmlInputFactory.createXMLStreamReader(is);
        reader.next();
        String readText = reader.getElementText();
        System.out.println(text);
        System.out.println(readText);
        reader.close();

        assertEquals(readText, text);
    }
}