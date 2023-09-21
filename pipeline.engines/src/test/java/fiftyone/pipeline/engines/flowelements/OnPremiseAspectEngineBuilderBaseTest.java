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