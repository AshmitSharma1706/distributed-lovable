package com.project.distributed_lovable.workspace_service.service;

import com.project.distributed_lovable.common_lib.dto.FileTreeDto;

public interface ProjectFileService {
    FileTreeDto getFileTree(Long projectId);

    String getFileContent(Long projectId, String path);

    void saveFile(Long projectId, String filePath, String fileContent);

    byte[] downloadProjectZip(Long projectId);
}
