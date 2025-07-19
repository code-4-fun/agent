package com.spring.ai.mcp.agent.rest;

import com.spring.ai.mcp.agent.task.DocumentLoaderTask;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;

@RestController
@RequestMapping("/api/agent/document")
@Slf4j
@RequiredArgsConstructor
public class DocumentReaderController {

    private final DocumentLoaderTask documentLoaderTask;

    @PostMapping("/load-doc")
    public String loadDocument(@RequestParam String documentUrl) {
        documentLoaderTask.loadDocuments(Collections.singletonList(documentUrl));
        return "Document loaded successfully from: " + documentUrl;
    }

    @PostMapping("/load-info")
    public void loadInformation(@RequestBody String info) {
        documentLoaderTask.loadInformation(info);
        log.info("Information loaded successfully: {}", info);
    }

}
