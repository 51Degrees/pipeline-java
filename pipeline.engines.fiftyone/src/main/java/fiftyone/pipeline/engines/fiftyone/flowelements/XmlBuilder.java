/* *********************************************************************
 * This Original Work is copyright of 51 Degrees Mobile Experts Limited.
 * Copyright 2019 51 Degrees Mobile Experts Limited, 5 Charlotte Close,
 * Caversham, Reading, Berkshire, United Kingdom RG4 7BY.
 *
 * This Original Work is licensed under the European Union Public Licence (EUPL) 
 * v.1.2 and is subject to its terms as set out below.
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Builder used to construct an XML string
 */
public class XmlBuilder {

    /**
     * Header to add to an XML string.
     */
    public static final String XML_HEADER = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";

    /**
     * Regex pattern matching characters which are invalid in as XML string.
     */
    private static final Pattern invalidChars =
        Pattern.compile(
        "[^\\u0009\\u000A\\u000D\\u0020-\\uD7FF\\uE000-\\uFFFD\\x{10000}-\\x{10FFFF}]");

    private final StringBuilder builder;

    /**
     * Default constructor.
     */
    public XmlBuilder() {
        builder = new StringBuilder();
        builder.append(XML_HEADER);
    }

    /**
     * Verify that there are no invalid characters in the string.
     * @param s string to check
     * @return true if there are no invalid characters
     */
    public static boolean verifyXmlChars(String s) {
        return invalidChars.matcher(s).find() == false;
    }

    /**
     * Get the escaped unicode for a character which cannot be printed in an XML
     * string
     * @param c character top escape
     * @return escaped unicode
     */
    public static String escapeUnicode(char c) {
        if (c < 0x10) {
            return "\\x000" + Integer.toHexString(c);
        } else if (c < 0x100) {
            return "\\x00" + Integer.toHexString(c);
        } else if (c < 0x1000) {
            return "\\x0" + Integer.toHexString(c);
        }
        return "\\x" + Integer.toHexString(c);
    }

    /**
     * Check if the character is valid.
     * @param c character to check
     * @return true if the character is valid
     */
    public static boolean isValidChar(char c) {
        return ((c == 0x9) ||
            (c == 0xA) ||
            (c == 0xD) ||
            ((c >= 0x20) && (c <= 0xD7FF)) ||
            ((c >= 0xE000) && (c <= 0xFFFD)) ||
            ((c >= 0x10000) && (c <= 0x10FFFF)));
    }

    /**
     * Build the XML string.
     * @return XML string
     */
    @Override
    public String toString() {
        return builder.toString();
    }

    /**
     * Write an XML element which starts a block i.e `<key>`.
     * @param key name of the element
     */
    public void writeStartElement(String key) {
        writeStartElement(key, new ArrayList<Map.Entry<String, String>>());
    }

    /**
     * Write an XML element which ends a block i.e. `</key>`.
     * @param key name of the element
     */
    public void writeEndElement(String key) {
        builder.append("</");
        builder.append(key);
        builder.append('>');
        builder.append('\n');
    }

    /**
     * Write an XML element which starts a block with an attribute defined
     * i.e. `<key attribute="value">`.
     * @param key name of the element
     * @param attribute the attribute and value
     */
    public void writeStartElement(
        String key,
        Map.Entry<String, String> attribute) {
        List<Map.Entry<String, String>> list = new ArrayList<>();
        list.add(attribute);
        writeStartElement(key, list);
    }

    /**
     * Write an XML element which starts a block with the attributes defined
     * i.e. `<key attribute1="value1" attribute2="value2">`.
     * @param key name of the element
     * @param attributes the attributes and their values
     */
    public void writeStartElement(
        String key,
        List<Map.Entry<String, String>> attributes) {
        builder.append('<')
            .append(key);
        for (Map.Entry<String, String> attribute : attributes) {
            builder.append(" ")
                .append(attribute.getKey()).append("=\"")
                .append(attribute.getValue())
                .append("\"");
        }
        builder.append('>')
            .append('\n');

    }

    /**
     * Write data in a CDATA block.
     * @param value value to write in the block
     */
    public void writeCData(String value) {
        builder.append("<![CDATA[");
        builder.append(value);
        builder.append("]]>");
    }

    /**
     * Write a value surrounded by a start and end element.
     * @param key name of the element
     * @param value the value
     */
    public void writeElement(String key, String value) {
        writeStartElement(key);
        builder.append(value);
        writeEndElement(key);
    }
}
