package com.project.distributed_lovable.workspace_service.service.impl;

import com.project.distributed_lovable.workspace_service.entity.ProjectFile;
import com.project.distributed_lovable.workspace_service.repository.ProjectFileRepository;
import io.minio.GetObjectArgs;
import io.minio.GetObjectResponse;
import io.minio.MinioClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjectFileServiceImplTest {

    @Mock
    private ProjectFileRepository projectFileRepository;

    @Mock
    private MinioClient minioClient;

    private ProjectFileServiceImpl projectFileService;

    @BeforeEach
    void setUp() {
        projectFileService = new ProjectFileServiceImpl(
                null,
                projectFileRepository,
                minioClient,
                null
        );
        ReflectionTestUtils.setField(projectFileService, "projectBucket", "lovable-ai");
    }

    @Test
    void downloadProjectZip_ValidFiles_ReturnsValidZipArchive() throws Exception {
        Long projectId = 2L;

        ProjectFile file1 = ProjectFile.builder()
                .path("src/App.tsx")
                .minioObjectKey("2/src/App.tsx")
                .build();

        when(projectFileRepository.findByProjectId(projectId)).thenReturn(List.of(file1));

        byte[] fakeContent = "export default function App() { return <h1>Hello</h1>; }".getBytes(StandardCharsets.UTF_8);
        GetObjectResponse realResponse = new GetObjectResponse(
                okhttp3.Headers.of("Content-Type", "text/plain"),
                "lovable-ai",
                "region",
                "2/src/App.tsx",
                new ByteArrayInputStream(fakeContent)
        );

        when(minioClient.getObject(any(GetObjectArgs.class))).thenReturn(realResponse);

        byte[] zipBytes = projectFileService.downloadProjectZip(projectId);

        assertNotNull(zipBytes);
        assertTrue(zipBytes.length > 0);

        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry entry = zis.getNextEntry();
            assertNotNull(entry);
            assertEquals("src/App.tsx", entry.getName());
            byte[] readBytes = zis.readAllBytes();
            assertEquals("export default function App() { return <h1>Hello</h1>; }", new String(readBytes, StandardCharsets.UTF_8));
            assertNull(zis.getNextEntry());
        }
    }
}
