package com.project.distributed_lovable.workspace_service.controller;

import com.project.distributed_lovable.workspace_service.service.ProjectFileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FileControllerTest {

    @Mock
    private ProjectFileService projectFileService;

    private FileController fileController;

    @BeforeEach
    void setUp() {
        fileController = new FileController(projectFileService);
    }

    @Test
    void downloadProjectZip_ReturnsZipResponseEntity() {
        Long projectId = 2L;
        byte[] mockZip = "PKfakezipcontent".getBytes(StandardCharsets.UTF_8);

        when(projectFileService.downloadProjectZip(projectId)).thenReturn(mockZip);

        ResponseEntity<byte[]> response = fileController.downloadProjectZip(projectId);

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertEquals("application/zip", response.getHeaders().getFirst(HttpHeaders.CONTENT_TYPE));
        assertEquals("attachment; filename=\"project-2.zip\"", response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION));
        assertArrayEquals(mockZip, response.getBody());
        verify(projectFileService, times(1)).downloadProjectZip(projectId);
    }
}
