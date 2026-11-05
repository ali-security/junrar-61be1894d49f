package com.github.junrar;

import com.github.junrar.rarfile.FileHeader;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class LocalFolderExtractorTest {
    private static File tempFolder;

    @BeforeAll
    public static void setupFunctionalTests() throws IOException {
        tempFolder = TestCommons.createTempDir();
    }

    @AfterAll
    public static void tearDownFunctionalTests() throws IOException {
        FileUtils.deleteDirectory(tempFolder);
    }

    @Test
    public void rarWithDirectoriesOutsideTarget_ShouldThrowException() throws IOException {
        File file = TestCommons.createTempDir();
        LocalFolderExtractor localFolderExtractor = new LocalFolderExtractor(file);

        Archive archive = mock(Archive.class);
        FileHeader fileHeader = mock(FileHeader.class);
        when(fileHeader.isFileHeader()).thenReturn(true);
        when(fileHeader.isUnicode()).thenReturn(true);
        when(fileHeader.getFileName()).thenReturn("../../ops");


        File expectedInvalidPath = new File(file.getParentFile().getParentFile(), "ops");

        Throwable thrown = catchThrowable(() -> localFolderExtractor.extract(archive, fileHeader));

        assertThat(thrown).isInstanceOf(IllegalStateException.class);
        assertThat(thrown.getMessage())
            .containsIgnoringCase("Rar contains file with invalid path")
            .containsIgnoringCase(expectedInvalidPath.toString());
    }

    @Test
    public void rarWithFileOutsideTarget_ShouldThrowException() throws IOException {
        File file = TestCommons.createTempDir();
        LocalFolderExtractor localFolderExtractor = new LocalFolderExtractor(file);

        FileHeader fileHeader = mock(FileHeader.class);
        when(fileHeader.isDirectory()).thenReturn(true);
        when(fileHeader.isUnicode()).thenReturn(true);
        when(fileHeader.getFileName()).thenReturn("../../ops/");


        File expectedInvalidPath = new File(file.getParentFile().getParentFile(), "ops");
        Throwable thrown = catchThrowable(() -> localFolderExtractor.createDirectory(fileHeader));

        assertThat(thrown).isInstanceOf(IllegalStateException.class);
        assertThat(thrown.getMessage())
            .containsIgnoringCase("Rar contains invalid path")
            .containsIgnoringCase(expectedInvalidPath.toString());
    }

    @Test
    public void rarWithFileOutsideTarget_ShouldThrowException2() throws Exception {
        File file = TestCommons.writeResourceToFolder(tempFolder, "parent-dir.rar");
        LocalFolderExtractor localFolderExtractor = new LocalFolderExtractor(tempFolder);

        try (Archive archive = new Archive(file)) {
            FileHeader fileHeader = archive.nextFileHeader();

            File expectedInvalidPath = new File(tempFolder.getParentFile().getParentFile(), "tmp");
            Throwable thrown = catchThrowable(() -> localFolderExtractor.extract(archive, fileHeader));

            assertThat(thrown).isInstanceOf(IllegalStateException.class);
            assertThat(thrown.getMessage())
                .containsIgnoringCase("Rar contains file with invalid path")
                .containsIgnoringCase(expectedInvalidPath.toString());
        }
    }

    @Test
    public void rarWithFileOutsideTarget_ShouldThrowException3() throws Exception {
        File file = TestCommons.writeResourceToFolder(tempFolder, "sibling-prefix-traversal.rar");

        File tempDir = new File("/tmp/extract");
        LocalFolderExtractor localFolderExtractor = new LocalFolderExtractor(tempDir);

        try (Archive archive = new Archive(file)) {
            FileHeader fileHeader = archive.nextFileHeader();

            String expectedInvalidPath = "/tmp/extract_evil/pwned.txt";
            Throwable thrown = catchThrowable(() -> localFolderExtractor.extract(archive, fileHeader));

            assertThat(thrown).isInstanceOf(IllegalStateException.class);
            assertThat(thrown.getMessage())
                .containsIgnoringCase("Rar contains file with invalid path")
                .containsIgnoringCase(expectedInvalidPath);
        }
    }

    @Test
    public void rarWithDirectoryInSiblingPrefixFolder_ShouldThrowException() throws IOException {
        File root = new File(tempFolder, "sibling-dir");
        File destination = new File(root, "extract");
        assertThat(destination.mkdirs()).isTrue();
        LocalFolderExtractor localFolderExtractor = new LocalFolderExtractor(destination);

        FileHeader fileHeader = mock(FileHeader.class);
        when(fileHeader.isDirectory()).thenReturn(true);
        when(fileHeader.getFileName()).thenReturn("../extract_evil/");

        File expectedInvalidPath = new File(root, "extract_evil");
        Throwable thrown = catchThrowable(() -> localFolderExtractor.createDirectory(fileHeader));

        assertThat(thrown).isInstanceOf(IllegalStateException.class);
        assertThat(thrown.getMessage())
            .containsIgnoringCase("Rar contains invalid path")
            .containsIgnoringCase(expectedInvalidPath.toString());
    }

    @Test
    public void junrarExtractWithSiblingPrefixTraversal_ShouldNotWriteOutsideTarget() throws Exception {
        File root = new File(tempFolder, "sibling-junrar");
        File destination = new File(root, "extract");
        assertThat(destination.mkdirs()).isTrue();
        File rar = TestCommons.writeResourceToFolder(root, "sibling-prefix-traversal.rar");

        Throwable thrown = catchThrowable(() -> Junrar.extract(rar, destination));

        assertThat(thrown).isInstanceOf(IllegalStateException.class);
        assertThat(thrown.getMessage()).containsIgnoringCase("Rar contains file with invalid path");
        assertThat(new File(new File(root, "extract_evil"), "pwned.txt")).doesNotExist();
    }

    @Test
    public void rarWithBackslashFileOutsideTarget_ShouldThrowException() throws IOException {
        File root = new File(tempFolder, "backslash");
        File destination = new File(root, "a" + File.separator + "b");
        assertThat(destination.mkdirs()).isTrue();
        LocalFolderExtractor localFolderExtractor = new LocalFolderExtractor(destination);

        Archive archive = mock(Archive.class);
        FileHeader fileHeader = mock(FileHeader.class);
        when(fileHeader.isFileHeader()).thenReturn(true);
        when(fileHeader.getFileName()).thenReturn("..\\..\\ops");

        File expectedInvalidPath = new File(root, "ops");
        Throwable thrown = catchThrowable(() -> localFolderExtractor.extract(archive, fileHeader));

        assertThat(thrown).isInstanceOf(IllegalStateException.class);
        assertThat(thrown.getMessage())
            .containsIgnoringCase("Rar contains file with invalid path")
            .containsIgnoringCase(expectedInvalidPath.toString());
        assertThat(expectedInvalidPath).doesNotExist();
    }

    @Test
    public void rarWithMixedSeparatorsFileOutsideTarget_ShouldThrowException() throws IOException {
        File root = new File(tempFolder, "mixed");
        File destination = new File(root, "a" + File.separator + "b");
        assertThat(destination.mkdirs()).isTrue();
        LocalFolderExtractor localFolderExtractor = new LocalFolderExtractor(destination);

        Archive archive = mock(Archive.class);
        FileHeader fileHeader = mock(FileHeader.class);
        when(fileHeader.isFileHeader()).thenReturn(true);
        when(fileHeader.getFileName()).thenReturn("sub/..\\..\\..\\ops");

        File expectedInvalidPath = new File(root, "ops");
        Throwable thrown = catchThrowable(() -> localFolderExtractor.extract(archive, fileHeader));

        assertThat(thrown).isInstanceOf(IllegalStateException.class);
        assertThat(thrown.getMessage())
            .containsIgnoringCase("Rar contains file with invalid path")
            .containsIgnoringCase(expectedInvalidPath.toString());
        assertThat(expectedInvalidPath).doesNotExist();
    }

    @Test
    public void rarWithBackslashFileInsideTarget_ShouldExtractInSubdirectory() throws Exception {
        File destination = new File(tempFolder, "inside");
        assertThat(destination.mkdirs()).isTrue();
        LocalFolderExtractor localFolderExtractor = new LocalFolderExtractor(destination);

        Archive archive = mock(Archive.class);
        FileHeader fileHeader = mock(FileHeader.class);
        when(fileHeader.isFileHeader()).thenReturn(true);
        when(fileHeader.getFileName()).thenReturn("sub\\inner.txt");

        File extracted = localFolderExtractor.extract(archive, fileHeader);

        File expected = new File(new File(destination, "sub"), "inner.txt");
        assertThat(extracted).isFile();
        assertThat(extracted.getCanonicalPath()).isEqualTo(expected.getCanonicalPath());
    }

    @Test
    public void junrarExtractWithBackslashParentDir_ShouldNotWriteOutsideTarget() throws Exception {
        File root = new File(tempFolder, "junrar");
        File destination = new File(root, "a" + File.separator + "b");
        assertThat(destination.mkdirs()).isTrue();
        File rar = TestCommons.writeResourceToFolder(root, "parent-dir.rar");

        Throwable thrown = catchThrowable(() -> Junrar.extract(rar, destination));

        assertThat(thrown).isInstanceOf(IllegalStateException.class);
        assertThat(thrown.getMessage()).containsIgnoringCase("Rar contains file with invalid path");
        assertThat(new File(new File(root, "tmp"), "existing-file")).doesNotExist();
    }
}
