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

package fiftyone.pipeline.engines.flowelements;

import fiftyone.pipeline.engines.Constants;
import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@SuppressWarnings("rawtypes")
public class OnPremiseAspectEngineBuilderBaseTest {

    private OnPremiseAspectEngineBuilderBase aspectEngineBuilder;

    @Before
    public void Init() {
        aspectEngineBuilder = createEngineBuilderBase();
        deleteTempDir();
    }

    private OnPremiseAspectEngineBuilderBase createEngineBuilderBase() {
        return new OnPremiseAspectEngineBuilderBase() {
            @Override
            public OnPremiseAspectEngineBuilderBase setPerformanceProfile(Constants.PerformanceProfiles profile) {
                return null;
            }

            @Override
            protected AspectEngine newEngine(List properties) {
                return null;
            }
        };
    }

    @After
    public void Cleanup() {
        deleteTempDir();
    }

    private void deleteTempDir() {
        Path path = Paths.get(aspectEngineBuilder.tempDir);
        File tempDir = path.toFile();
        if (tempDir.exists()) {
            tempDir.delete();
        }
    }

    @Test
    public void CreateTemporaryFolder_VerifyWritePermissionsUnix() throws IOException {
        unixOnly();
        createTempDir();
        Set<PosixFilePermission> permissions = getTempDirectoryPermissions();
        assertFalse(permissions.contains(PosixFilePermission.OTHERS_WRITE));
        assertFalse(permissions.contains(PosixFilePermission.GROUP_WRITE));
        assertTrue(permissions.contains(PosixFilePermission.OWNER_WRITE));
    }

    private void unixOnly() {
        Assume.assumeFalse(System.getProperty("os.name").startsWith("Windows"));
    }

    private File createTempDir() {
        Path path = Paths.get(aspectEngineBuilder.tempDir);
        aspectEngineBuilder.createAndVerifyTempDir(path);
        return path.toFile();
    }

    private Set<PosixFilePermission> getTempDirectoryPermissions() throws IOException {
        Path path = Paths.get(aspectEngineBuilder.tempDir);
        return Files.getPosixFilePermissions(path, LinkOption.NOFOLLOW_LINKS);
    }

    @Test
    public void CreateTemporaryFolder_VerifyReadPermissionsUnix() throws IOException {
        unixOnly();
        createTempDir();
        Set<PosixFilePermission> permissions = getTempDirectoryPermissions();
        assertFalse(permissions.contains(PosixFilePermission.OTHERS_READ));
        assertTrue(permissions.contains(PosixFilePermission.GROUP_READ));
        assertTrue(permissions.contains(PosixFilePermission.OWNER_READ));
    }

    @Test
    public void CreateTemporaryFolder_VerifyExecutePermissionsUnix() throws IOException {
        unixOnly();
        createTempDir();
        Set<PosixFilePermission> permissions = getTempDirectoryPermissions();
        assertFalse(permissions.contains(PosixFilePermission.OTHERS_EXECUTE));
        assertTrue(permissions.contains(PosixFilePermission.GROUP_EXECUTE));
        assertTrue(permissions.contains(PosixFilePermission.OWNER_EXECUTE));
    }

    @Test
    public void CreateTemporaryFolder_VerifyPermissions() {
        File tempDir = createTempDir();
        assertTrue(tempDir.canRead());
        assertTrue(tempDir.canWrite());
        assertTrue(tempDir.canExecute());
    }
}