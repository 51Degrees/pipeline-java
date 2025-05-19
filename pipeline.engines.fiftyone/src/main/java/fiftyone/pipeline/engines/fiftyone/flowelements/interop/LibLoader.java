/* *********************************************************************
 * This Original Work is copyright of 51 Degrees Mobile Experts Limited.
 * Copyright 2025 51 Degrees Mobile Experts Limited, Davidson House,
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

import fiftyone.pipeline.engines.flowelements.AspectEngine;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

/**
 * Native library loader class used to load the correct compiled library for an
 * {@link AspectEngine}.
 */
public class LibLoader {

    /**
     * Get the enumeration of the group of Operating Systems to which the system
     * OS belongs to.
     *
     * @return OS group to which the OS belongs
     * @throws UnsupportedOperationException if the OS is not supported
     */
    private static OS getOs() throws UnsupportedOperationException {
        String os = System.getProperty("os.name").toLowerCase();

        // Check for Windows
        if (os.contains("win")) {
            return OS.WINDOWS;
        }

        // Check for Linux or Unix
        if (os.contains("nux") || os.contains("nix")) {
            return OS.UNIX;
        }

        // Check for OSx
        if (os.contains("mac")) {
            return OS.MAC;
        }

        // No supported Operating System could be found.
        throw new UnsupportedOperationException(String.format(
            "Unsupported Operating System '%s'. Please contact " +
                "support@51degrees.com with details of your Operating System.",
            System.getProperty("os.name")));
    }

    /**
     * Get the processor architecture of the current system.
     *
     * @return String to add to the file name
     * @throws UnsupportedOperationException is the architecture is not
     *                                       supported
     */
    private static String getArch() throws UnsupportedOperationException {
        String arch = System.getProperty("os.arch").toLowerCase();

        if (arch.contains("arm")) {
            return "arm";
        }

        if (arch.contains("aarch64")) {
            return "aarch64";
        }

        // Check for 64 bit
        if (arch.contains("64")) {
            return "x64";
        }

        // Check for 32 bit
        if (arch.contains("32") || arch.contains("86")) {
            return "x86";
        }

        // No supported architectures could be found
        throw new UnsupportedOperationException(String.format(
            "Unsupported processor architecture '%s'. Please contact " +
                "support@51degrees.com with details of your architecture.",
            System.getProperty("os.arch")));
    }

    /**
     * Get the file name for the compiled native library.
     *
     * @param engineClass the class of the target engine to get the name for
     * @return String full file name
     */
    public static String getLibName(Class<?> engineClass) {
        return engineClass.getSimpleName() +
            "-" + getOs().toString() +
            "-" + getArch() +
            getOs().getExtension();
    }

    /**
     * Copies the native library for the operating system, CPU architecture and
     * Java version to the file system so that it can be loaded as a library for
     * reference by the JVM. Load the native library file needed for the Engine.
     * If the package does not contain a native library for the target
     * environment an exception is thrown.
     *
     * @param engineClass the class of the target engine to load
     * @throws UnsupportedOperationException if the package does not contain
     *                                       a native library for the environment
     * @throws IOException thrown if there is a problem copying the resource
     */
    public static void load(Class<?> engineClass)
        throws IOException, UnsupportedOperationException {
        File nativeLibraryFile = copyResource(
            engineClass,
            getLibName(engineClass));
        try {
            System.load(nativeLibraryFile.getAbsolutePath());
        } finally {
            nativeLibraryFile.delete();
        }
    }

    /**
     * Copies the resource libName from the package resources in the class
     * engineClass to a temporary file.
     *
     * @param engineClass name of a class that is in the same pacakge as the
     *                    native library resource to be copied
     * @param libName     name of the native library resource in the package
     * @return a File instance to the temporary file which now contains the
     * native library
     * @throws IOException          if the resource can not be copied
     * @throws UnsatisfiedLinkError if the resource can not be found
     */
    private static File copyResource(Class<?> engineClass, String libName)
        throws IOException {
        byte[] buffer = new byte[1024];
        int read;
        File temp = File.createTempFile(libName, "");
        try (InputStream in = Optional
                .ofNullable(engineClass.getResourceAsStream("/" + libName))
                .orElseGet(() -> engineClass.getResourceAsStream("/Debug/" + libName))) {
            if (in == null) {
                throw new UnsatisfiedLinkError(String.format(
                    "Could not find the resource '%s'. Check the resource " +
                        "exists in the '%s' package or is present in the " +
                        "src/main/resources folder.",
                    libName,
                    engineClass.getSimpleName()));
            }
            try (FileOutputStream fos = new FileOutputStream(temp)) {
                while ((read = in.read(buffer)) != -1) {
                    fos.write(buffer, 0, read);
                }
            }
        } catch (Exception ex) {
            throw new IOException(String.format(
                "Could not copy resource '%s' to file '%s'. " +
                    "Check file system permissions.",
                libName,
                temp.getAbsolutePath()), ex);
        }
        return temp;
    }

    /**
     * Enumeration of supported Operating System groups. OS's are grouped by
     * native library support. I.e. all Windows OS's will use the same native
     * library.
     */
    enum OS {
        WINDOWS, /* Any windows OS containing "win" in the name. */
        UNIX, /* Any Linux/Unix OS containing "nix" or "nux" in the name. */
        MAC, /* Any OS X OS containing "mac" in the name. */
        OTHER; /* Any other OS which is not supported by LibLoader. */

        /**
         * Get the lowercase name of the OS group. E.g. 'linux' or 'mac'.
         *
         * @return name of OS
         */
        @Override
        public String toString() {
            switch (this) {
                case UNIX:
                    return "linux";
                case WINDOWS:
                    return "windows";
                case MAC:
                    return "mac";
                case OTHER:
                default:
                    return "unsupported";
            }
        }

        /**
         * Get the file extension for the current operating system.
         * Assumes 'dll' for Windows and 'so' for anything else.
         *
         * @return String file extension including ".".
         */
        String getExtension() {
            switch (this) {
                case WINDOWS:
                    return ".dll";
                case MAC:
                    return ".dylib";
                case UNIX:
                case OTHER:
                default:
                    return ".so";
            }
        }
    }
}
