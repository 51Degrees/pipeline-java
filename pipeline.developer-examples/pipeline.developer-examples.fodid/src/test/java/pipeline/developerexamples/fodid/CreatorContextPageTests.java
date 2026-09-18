/* *********************************************************************
 * This Original Work is copyright of 51 Degrees Mobile Experts Limited.
 * Copyright 2026 51 Degrees Mobile Experts Limited, Davidson House,
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

package pipeline.developerexamples.fodid;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * What the creator context demo page does in a browser, checked by
 * running its script in Node against a small stand in for a browser
 * (creator-context-page-harness.js). No browser is started and nothing
 * here reaches a network.
 * <p>
 * The service creates a 51Did only once the page has run the snippets it
 * asks for and sent what they collected, so a page that asks for one
 * directly is told the page has not finished and is given nothing. The
 * 51Degrees client script is what runs those snippets, so the page has
 * to create through the script and then send what the snippets collected
 * with its verification call as well. These tests pin both, because
 * neither can be seen from the Java side of the demo and neither shows
 * up in a unit test of the redeem route.
 * <p>
 * Node runs the page's script, and every GitHub hosted runner has it.
 * Where it is absent these tests say so and stop rather than passing on
 * nothing.
 */
public class CreatorContextPageTests {

    /**
     * The licensed probabilistic identifier the stand in client script
     * reports, once the page has made it safe for a URL.
     */
    private static final String CREATED_URL_SAFE = "prob-lic_value";

    /**
     * Both files are copied into the build output, the page from the
     * module's own resources and the harness from its test resources, so
     * each is a real file on disk by the time a test runs.
     */
    private static Path onDisk(String resource) throws Exception {
        java.net.URL found =
            CreatorContextPageTests.class.getClassLoader()
                .getResource(resource);
        assertTrue(found != null, resource + " is on the class path");
        return Paths.get(found.toURI());
    }

    private static Path page() throws Exception {
        return onDisk("fodid/creator-context/page.html");
    }

    private static Path harness() throws Exception {
        return onDisk("fodid/creator-context/creator-context-page-harness.js");
    }

    private static String pageText() throws Exception {
        return new String(Files.readAllBytes(page()), StandardCharsets.UTF_8);
    }

    /**
     * Runs a program and returns everything it printed, failing the test
     * where it does not succeed. A missing Node is reported as an
     * assumption that did not hold, so the run says the page was never
     * checked rather than reporting a pass.
     */
    private static String run(List<String> command) throws Exception {
        ProcessBuilder builder = new ProcessBuilder(command);
        builder.redirectErrorStream(true);
        Process process;
        try {
            process = builder.start();
        } catch (IOException notThere) {
            Assumptions.assumeTrue(false,
                "node is not on the path, so the page was not run: "
                    + notThere.getMessage());
            throw notThere;
        }
        String printed = new String(
            readAll(process), StandardCharsets.UTF_8);
        assertTrue(
            process.waitFor(120, TimeUnit.SECONDS),
            "the process did not finish");
        assertEquals(0, process.exitValue(), "the process failed: " + printed);
        return printed;
    }

    private static byte[] readAll(Process process) throws IOException {
        java.io.ByteArrayOutputStream collected =
            new java.io.ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int read;
        while ((read = process.getInputStream().read(buffer)) != -1) {
            collected.write(buffer, 0, read);
        }
        return collected.toByteArray();
    }

    /**
     * Runs the page's script and returns what the harness recorded.
     *
     * @param given the identifier the page is opened with, which is the
     *              transplant path, or null to create one instead
     */
    private static JSONObject runPage(String given) throws Exception {
        List<String> command = new ArrayList<>(Arrays.asList(
            "node", harness().toString(), page().toString()));
        if (given != null) {
            command.add(given);
        }
        String[] lines = run(command).trim().split("\\r?\\n");
        return new JSONObject(lines[lines.length - 1].trim());
    }

    private static List<String> strings(JSONObject record, String name) {
        JSONArray values = record.getJSONArray(name);
        List<String> collected = new ArrayList<>();
        for (int index = 0; index < values.length(); index++) {
            collected.add(values.getString(index));
        }
        return collected;
    }

    private static List<String> containing(List<String> values, String text) {
        List<String> found = new ArrayList<>();
        for (String value : values) {
            if (value.contains(text)) {
                found.add(value);
            }
        }
        return found;
    }

    private static String row(JSONObject record, String name) {
        return record.getJSONObject("rows").getString(name);
    }

    /**
     * A page whose script does not parse defines nothing and reports
     * nothing, and the browser says so only in its console. The
     * <code>node --check</code> reader takes JavaScript rather than HTML,
     * so the page's script block is written out on its own and checked.
     */
    @Test
    public void The_Page_Script_Parses() throws Exception {
        // A checkout on Windows has carriage returns in it, so the ends
        // of lines are matched either way.
        Matcher block = Pattern
            .compile("<script>\\r?\\n(.*?)\\r?\\n</script>", Pattern.DOTALL)
            .matcher(pageText());
        assertTrue(block.find(), "the page has a script block");
        File written = File.createTempFile("page-script", ".js");
        try {
            Files.write(
                written.toPath(), block.group(1).getBytes(StandardCharsets.UTF_8));
            run(Arrays.asList("node", "--check", written.getAbsolutePath()));
        } finally {
            assertTrue(written.delete() || !written.exists());
        }
        assertEquals(0, strings(runPage(null), "errors").size());
    }

    /**
     * The page asks the cloud for the client script, with the usage and
     * the email address on its address, and takes the identifier from
     * what the script reports.
     */
    @Test
    public void The_Identifier_Comes_From_The_Client_Script() throws Exception {
        JSONObject record = runPage(null);
        List<String> scripts = strings(record, "scripts");
        assertEquals(1, scripts.size(),
            "the client script is loaded exactly once");
        assertTrue(scripts.get(0).contains("TEST-RESOURCE-KEY.js"),
            scripts.get(0));
        assertTrue(scripts.get(0).contains("id.usage=non-marketing"),
            scripts.get(0));
        assertTrue(scripts.get(0).contains("id.email="), scripts.get(0));
        assertEquals("created for this browser", row(record, "s-create"));
    }

    /**
     * A request of the page's own would be made before the snippets had
     * run, and the service would answer it with no identifier at all.
     */
    @Test
    public void The_Page_Does_Not_Ask_For_An_Identifier_Itself()
        throws Exception {
        for (String address : strings(runPage(null), "fetches")) {
            assertFalse(address.contains("json?resource="), address);
            assertFalse(address.contains("/json"), address);
        }
    }

    /**
     * The identifier the script reported is verified, made safe for a
     * URL, and what the snippets collected goes with it, because the
     * service compares this browser against the creator from those
     * values.
     */
    @Test
    public void The_Verification_Carries_The_Identifier_And_The_Snippets()
        throws Exception {
        List<String> verify = containing(
            strings(runPage(null), "fetches"), "id/verify-full");
        assertEquals(1, verify.size());
        assertTrue(verify.get(0).contains(CREATED_URL_SAFE), verify.get(0));
        assertFalse(verify.get(0).split("\\?")[0].contains("+"),
            "the identifier is made safe for a URL");
        assertTrue(verify.get(0).contains("51D_ScreenPixelsHeight=1080"),
            verify.get(0));
        assertTrue(verify.get(0).contains("51D_ProfileIds=1-2-3"),
            verify.get(0));
        assertFalse(verify.get(0).contains("unrelated=ignored"),
            "only the snippet values are forwarded");
    }

    /**
     * The licence key lives on the server, so the sealed result goes
     * there and the verdict comes back from there.
     */
    @Test
    public void The_Result_Is_Redeemed_On_The_Pages_Own_Server()
        throws Exception {
        JSONObject record = runPage(null);
        List<String> redeem = new ArrayList<>();
        for (String address : strings(record, "fetches")) {
            if (address.startsWith("/redeem?")) {
                redeem.add(address);
            }
        }
        assertEquals(1, redeem.size());
        assertTrue(redeem.get(0).contains("result=sealed-result"),
            redeem.get(0));
        assertEquals("verified", row(record, "s-signature"));
        assertEquals("verified", row(record, "s-context"));
    }

    /**
     * The page opened with an identifier from another browser checks that
     * identifier, and still needs this browser's snippet values for the
     * comparison, so the script runs on that path too.
     */
    @Test
    public void A_Transplanted_Identifier_Still_Runs_The_Client_Script()
        throws Exception {
        JSONObject record = runPage("given-value");
        assertEquals(1, strings(record, "scripts").size());
        List<String> verify = containing(
            strings(record, "fetches"), "id/verify-full");
        assertEquals(1, verify.size());
        assertTrue(verify.get(0).contains("given-value"), verify.get(0));
        assertTrue(verify.get(0).contains("51D_ProfileIds=1-2-3"),
            verify.get(0));
    }

    /**
     * Someone running the demo is interested in the subject, so the page
     * ends with somewhere to go next. The links to 51degrees.com carry
     * the campaign tags the link lint asks for and the source
     * repositories are given as plain addresses.
     */
    @Test
    public void The_Page_Ends_With_Find_Out_More() throws Exception {
        String page = pageText();
        assertTrue(page.contains("Find out more"), "the page says where next");
        assertTrue(page.contains("utm_campaign=pipeline-java"), "campaign tag");
        assertTrue(page.contains("https://github.com/51Degrees/pipeline-java"),
            "the repository is linked");
    }
}
