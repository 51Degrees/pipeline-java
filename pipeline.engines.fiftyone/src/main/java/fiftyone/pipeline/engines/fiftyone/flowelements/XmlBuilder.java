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

public class XmlBuilder {
    public static final String XML_HEADER = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";

    private static final Pattern invalidChars =
        Pattern.compile(
        "[^\\u0009\\u000A\\u000D\\u0020-\\uD7FF\\uE000-\\uFFFD\\x{10000}-\\x{10FFFF}]");

    private StringBuilder builder;

    public XmlBuilder() {
        builder = new StringBuilder();
        builder.append(XML_HEADER);
    }

    public static boolean verifyXmlChars(String s) {
        return invalidChars.matcher(s).find() == false;
    }

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

    public static boolean isValidChar(char c) {
        return ((c == 0x9) ||
            (c == 0xA) ||
            (c == 0xD) ||
            ((c >= 0x20) && (c <= 0xD7FF)) ||
            ((c >= 0xE000) && (c <= 0xFFFD)) ||
            ((c >= 0x10000) && (c <= 0x10FFFF)));
    }

    @Override
    public String toString() {
        return builder.toString();
    }

    public void writeStartElement(String key) {
        writeStartElement(key, new ArrayList<Map.Entry<String, String>>());
    }

    public void writeEndElement(String key) {
        builder.append("</");
        builder.append(key);
        builder.append('>');
        builder.append('\n');
    }

    public void writeStartElement(
        String key,
        Map.Entry<String, String> attribute) {
        List<Map.Entry<String, String>> list = new ArrayList<>();
        list.add(attribute);
        writeStartElement(key, list);
    }

    public void writeStartElement(
        String key,
        List<Map.Entry<String, String>> attributes) {
        builder.append('<');
        builder.append(key);
        for (Map.Entry<String, String> attribute : attributes) {
            builder.append(" " + attribute.getKey() + "=\"");
            builder.append(attribute.getValue());
            builder.append("\"");
        }
        builder.append('>');
        builder.append('\n');

    }

    public void writeCData(String value) {
        builder.append("<![CDATA[");
        builder.append(value);
        builder.append("]]>");
    }

    public void writeElement(String key, String value) {
        writeStartElement(key);
        builder.append(value);
        writeEndElement(key);
    }
}
