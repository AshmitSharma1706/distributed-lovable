package com.project.distributed_lovable.workspace_service.service.impl;

import com.project.distributed_lovable.common_lib.dto.FileNode;
import com.project.distributed_lovable.common_lib.dto.FileTreeDto;
import com.project.distributed_lovable.common_lib.error.ResourceNotFoundException;
import com.project.distributed_lovable.workspace_service.entity.Project;
import com.project.distributed_lovable.workspace_service.entity.ProjectFile;
import com.project.distributed_lovable.workspace_service.mapper.ProjectFileMapper;
import com.project.distributed_lovable.workspace_service.repository.ProjectFileRepository;
import com.project.distributed_lovable.workspace_service.repository.ProjectRepository;
import com.project.distributed_lovable.workspace_service.service.ProjectFileService;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProjectFileServiceImpl implements ProjectFileService {

    private final ProjectRepository projectRepository;
    private final ProjectFileRepository projectFileRepository;
    private final MinioClient minioClient;
    private final ProjectFileMapper projectFileMapper;

    @Value("${minio.project-bucket}")
    private String projectBucket;

    private static final String BUCKET_NAME = "lovable-ai";

    @Override
    public FileTreeDto getFileTree(Long projectId) {
        List<ProjectFile> projectFileList = projectFileRepository.findByProjectId(projectId);
        List<FileNode> projectFileNodes = projectFileMapper.toListOfFileNode(projectFileList);
        return new FileTreeDto(projectFileNodes);
    }

    @Override
    public String getFileContent(Long projectId, String path) {
        String objectName = projectId + "/" + path;
        try (
                InputStream is = minioClient.getObject(
                        GetObjectArgs.builder()
                                .bucket(BUCKET_NAME)
                                .object(objectName)
                                .build())) {

            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("Failed to read file: {}/{}", projectId, path, e);
            throw new RuntimeException("Failed to read file content", e);
        }
    }

    @Override
    public void saveFile(Long projectId, String filePath, String fileContent) {
        log.info("Saving file: {}", filePath);
        Project project = projectRepository.findById(projectId).orElseThrow(
                () -> new ResourceNotFoundException("Project", projectId.toString()));

        String cleanPath = filePath.startsWith("/") ? filePath.substring(1) : filePath;
        String objectKey = projectId + "/" + cleanPath;

        try {
            byte[] contentBytes = fileContent.getBytes(StandardCharsets.UTF_8);
            InputStream inputStream = new ByteArrayInputStream(contentBytes);
            // saving the file content
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(projectBucket)
                            .object(objectKey)
                            .stream(inputStream, contentBytes.length, -1)
                            .contentType(determineContentType(filePath))
                            .build());

            // Saving the metaData
            ProjectFile file = projectFileRepository.findByProjectIdAndPath(projectId, cleanPath)
                    .orElseGet(() -> ProjectFile.builder()
                            .project(project)
                            .path(cleanPath)
                            .minioObjectKey(objectKey) // Use the key we generated
                            .createdAt(Instant.now())
                            .build());

            file.setUpdatedAt(Instant.now());
            projectFileRepository.save(file);
            log.info("File saved: {}", objectKey);
        } catch (Exception e) {
            log.error("Failed to save file {}/{}", projectId, cleanPath, e);
            throw new RuntimeException("File save failed", e);
        }

    }

    @Override
    public byte[] downloadProjectZip(Long projectId) {
        List<ProjectFile> files = projectFileRepository.findByProjectId(projectId);
        try (java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
                java.util.zip.ZipOutputStream zos = new java.util.zip.ZipOutputStream(baos)) {

            for (ProjectFile file : files) {
                String path = file.getPath();
                String objectKey = file.getMinioObjectKey();

                try (InputStream is = minioClient.getObject(
                        GetObjectArgs.builder()
                                .bucket(projectBucket)
                                .object(objectKey)
                                .build())) {

                    java.util.zip.ZipEntry entry = new java.util.zip.ZipEntry(path);
                    zos.putNextEntry(entry);
                    is.transferTo(zos);
                    zos.closeEntry();
                } catch (Exception e) {
                    log.warn("Could not include file {} in zip for project {}: {}", path, projectId, e.getMessage());
                }
            }
            zos.finish();
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Failed to generate zip for project {}", projectId, e);
            throw new RuntimeException("Failed to generate project zip", e);
        }
    }

    private String determineContentType(String path) {
        String type = URLConnection.guessContentTypeFromName(path);
        if (type != null)
            return type;
        if (path.endsWith(".jsx") || path.endsWith(".ts") || path.endsWith(".tsx"))
            return "text/javascript";
        if (path.endsWith(".json"))
            return "application/json";
        if (path.endsWith(".css"))
            return "text/css";

        return "text/plain";
    }
}
