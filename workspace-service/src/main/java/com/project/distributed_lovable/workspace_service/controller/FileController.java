package com.project.distributed_lovable.workspace_service.controller;

import com.project.distributed_lovable.common_lib.dto.FileTreeDto;
import com.project.distributed_lovable.workspace_service.service.ProjectFileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/projects/{projectId}/files")
public class FileController {

    private final ProjectFileService projectFileService;

    @GetMapping
    public ResponseEntity<FileTreeDto> getFileTree(@PathVariable Long projectId) {
        return ResponseEntity.ok(projectFileService.getFileTree(projectId));
    }

    @GetMapping("/content") // /src/hooks/get-user-hook.jsx
    public ResponseEntity<String> getFile(
            @PathVariable Long projectId,
            @RequestParam String path) {
        return ResponseEntity.ok(projectFileService.getFileContent(projectId, path));
    }

    @GetMapping("/download-zip")
    @org.springframework.security.access.prepost.PreAuthorize("@security.canViewProject(#projectId)")
    public ResponseEntity<byte[]> downloadProjectZip(@PathVariable Long projectId) {
        byte[] zipBytes = projectFileService.downloadProjectZip(projectId);
        return ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"project-" + projectId + ".zip\"")
                .contentType(org.springframework.http.MediaType.parseMediaType("application/zip"))
                .body(zipBytes);
    }

}
