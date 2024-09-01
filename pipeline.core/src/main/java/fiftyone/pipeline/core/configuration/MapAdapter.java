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

package fiftyone.pipeline.core.configuration;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import jakarta.xml.bind.annotation.adapters.XmlAdapter;
import java.util.HashMap;
import java.util.Map;

/**
 * XML adapter used to unmarshal XML elements to a {@link Map}. Note that this
 * only implements the {@link XmlAdapter#unmarshal(Object)}, not the
 * {@link XmlAdapter#marshal(Object)} as this is only ever used to unmarshal.
 */
public class MapAdapter extends XmlAdapter<Object, Map<String, Object>> {

    @Override
    public Map<String, Object> unmarshal(Object v) throws Exception {
        Map<String, Object> map = new HashMap<>();
        Node node = (Node) v;
        Node current = node.getFirstChild();
        while (current != null) {
            NodeList values = current.getChildNodes();
            if (values.getLength() > 1) {
                map.put(current.getNodeName(), unmarshal(current));
            } else if (values.getLength() == 1) {
                map.put(current.getNodeName(), values.item(0).getNodeValue());
            } else if (current.getNodeType() == Node.ELEMENT_NODE){
                // empty element gets empty string
                map.put(current.getNodeName(), "");
            }
            current = current.getNextSibling();
        }
        return map;
    }

    @Override
    public Node marshal(Map<String, Object> v) throws Exception {
        throw new UnsupportedOperationException();
    }
}
