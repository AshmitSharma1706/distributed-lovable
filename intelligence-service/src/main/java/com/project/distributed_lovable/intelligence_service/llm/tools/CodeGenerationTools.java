package com.project.distributed_lovable.intelligence_service.llm.tools;

import com.project.distributed_lovable.intelligence_service.client.WorkspaceClient;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
public class CodeGenerationTools {

    private final Long projectId;
    private final WorkspaceClient workspaceClient;
    private final String authorization;

    @Tool(name = "read_files",
            description = "Read the contents of project files. Provide the relative file paths as a list.")
    public List<String> readFiles(
            @ToolParam(description = "Relative project file paths, for example: src/App.tsx, src/main.tsx")
            List<String> paths){
        List<String> result=new ArrayList<>();
        for (String path:paths){
            String cleanPath=path.startsWith("/") ? path.substring(1) : path;
            String content=workspaceClient.getFileContent(projectId, cleanPath, authorization);
            result.add(String.format(
                            "--- START OF FILE: %s ---\n%s\n--- END OF FILE ---", cleanPath, content
                    ));
        }
        return result;
    }
}
