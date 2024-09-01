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

package fiftyone.pipeline.engines.fiftyone.flowelements.interop;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.LoggerFactory;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class LibLoaderTests {

    private static String actualOs;
    private static String actualArch;

    @BeforeAll
    public static void before(){
        actualOs = System.getProperty("os.name");
        actualArch = System.getProperty("os.arch");
    }

    @AfterAll
    public static void after(){
        System.setProperty("os.name", actualOs);
        System.setProperty("os.arch", actualArch);
        LoggerFactory.getLogger(LibLoaderTests.class).info("resetting properties");
    }

    public static class TestName {}

    private static Stream<Arguments> getLibNames() {
        return Stream.of(
                // 64 bit linux intel
                Arguments.of("TestName-linux-x64.so", "linux", "x64"),
                Arguments.of("TestName-linux-x64.so", "linux", "x86_64"),
                Arguments.of("TestName-linux-x64.so", "linux", "amd64"),
                // 32 bit linux intel
                Arguments.of("TestName-linux-x86.so", "linux", "x86"),
                Arguments.of("TestName-linux-x86.so", "linux", "i386"),
                // 64 bit windows intel
                Arguments.of("TestName-windows-x64.dll", "windows", "x64"),
                Arguments.of("TestName-windows-x64.dll", "windows", "x86_64"),
                Arguments.of("TestName-windows-x64.dll", "windows", "amd64"),
                // 32 bit windows intel
                Arguments.of("TestName-windows-x86.dll", "windows", "x86"),
                // 64 bit mac intel
                Arguments.of("TestName-mac-x64.dylib", "mac", "x64"),
                // 32 bit mac intel
                Arguments.of("TestName-mac-x86.dylib", "mac", "x86"),
                Arguments.of("TestName-mac-x86.dylib", "mac", "i386"),
                // 32 bit linux arm
                Arguments.of("TestName-linux-arm.so", "linux", "armhf"),
                Arguments.of("TestName-linux-arm.so", "linux", "arm32"),
                // 64 bit linux arm
                Arguments.of("TestName-linux-aarch64.so", "linux", "aarch64")
                );
    }

    @ParameterizedTest
    @MethodSource("getLibNames")
    public void LibName(String expected, String os, String arch) {
        System.setProperty("os.name", os);
        System.setProperty("os.arch", arch);
        String name = LibLoader.getLibName(TestName.class);
        assertEquals(expected, name);
    }
}
