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

package fiftyone.pipeline.engines.services;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

public class HttpClientTests {
  private HttpClient client;

  private String testUrl = "https://cloud.51degrees.com/api/v4/accessibleproperties?Resource=AQS5HKcyVj6B8wNG2Ug";

  @Before
  public void Initialize() {
      client = new HttpClientDefault();
  }

  /**
   * Check that a URL returning an error code (401 in this case) will
   * still have it's json detail returned by the 'getResponseString'
   * method.
   */
  @Test
  public void VerifyErrorHandling() {
    String result= "No response";
    String expected = "{ \"status\":\"401\", \"errors\": [\"This Resource Key is not authorized for use with this domain: ''. See http://51degrees.com/documentation/_info__error_messages.html#Resource_key_not_authorized_on_domain for more information.\"] }";

    try{
      HttpURLConnection connection = client.connect(new URL(testUrl));
      result = client.getResponseString(connection);
    } catch (IOException ex) {
      assertTrue("Unexpected exception: " + ex.getMessage(), false);
    }
    
    assertEquals(expected, result);
  }

  /**
   * Check that the origin header is actually sent as expected
   */
  @Test
  public void VerifyHeaders() {
    int code = -1;
    String result = "";

    Map<String, String> headers = new HashMap<>();
    headers.put("Origin", "51degrees.com");
    
    try{
      HttpURLConnection connection = client.connect(new URL(testUrl));
      result = client.getResponseString(connection, headers);
      code = connection.getResponseCode();
    } catch (IOException ex) {
      assertTrue("Unexpected exception: " + ex.getMessage(), false);
    }
    
    assertEquals("Expected status code 200, but was " + 
      code + ". Complete response: " + result, 200, code);
  }
}
