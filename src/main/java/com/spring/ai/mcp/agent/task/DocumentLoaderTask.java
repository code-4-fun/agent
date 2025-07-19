package com.spring.ai.mcp.agent.task;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class DocumentLoaderTask {

    private final VectorStore vectorStore;

    public void loadDocuments(List<String> url) {
        log.info("Loading documents into the vector store...");
        try {
            url.forEach(documentUrl -> {
                try {
                    var docs = new PagePdfDocumentReader(documentUrl).read();

                    docs.forEach(doc -> {
                        log.info("Processing document: {}", doc.getMetadata());
                        var tokenizedDocs = new TokenTextSplitter().apply(Collections.singletonList(doc));
                        tokenizedDocs.forEach(token -> {
                            log.info("Adding document to vector store: {}", token.getText());
                            vectorStore.add(Collections.singletonList(token));
                        });
                    });
                } catch (Exception e) {
                    log.error("Error loading document from URL {}: {}", documentUrl, e.getMessage(), e);
                    throw new RuntimeException("Failed to load document from URL: " + documentUrl, e);
                }
            });
            log.info("Documents loaded successfully.");
        } catch (Exception e) {
            log.error("Error loading documents: {}", e.getMessage(), e);
        }

    }

    public void loadInformation(String info) {
        var document = new Document(info);
        var tokens = new TokenTextSplitter().apply(Collections.singletonList(document));
        vectorStore.add(tokens);
    }

}
