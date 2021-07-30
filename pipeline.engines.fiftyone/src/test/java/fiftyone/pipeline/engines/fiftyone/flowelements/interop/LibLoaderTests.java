package fiftyone.pipeline.engines.fiftyone.flowelements.interop;

import org.junit.Assert;
import org.junit.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.internal.creation.SuspendMethod;

import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;

public class LibLoaderTests {

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
