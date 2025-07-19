package com.spring.ai.mcp.agent.rest;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/agent")
public class ChatAgentController {

    private final ChatClient chatClient;
    private final Advisor advisor;

    @GetMapping("/chat")
    public String chat(@RequestBody String message) {
        log.info("Received message: {}", message);
        return chatClient.prompt().user(message).call().content();
    }

    @GetMapping("/advice")
    public String respondToUser(@RequestBody String message) {
        log.info("Responding to user: {}", message);
        return chatClient.prompt()
                .advisors(advisor)
                .user(message)
                .call()
                .content();
    }

}
