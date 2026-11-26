package com.rkagent.rkagent;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.memory.repository.jdbc.JdbcChatMemoryRepository;
import org.springframework.ai.chat.memory.repository.jdbc.PostgresChatMemoryRepositoryDialect;

import java.util.List;

import javax.sql.DataSource;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;

@Service
public class ChatService {
    private final ChatClient chatClient;
    private final VectorStore vectorStore;
    private static final String DEFAULT_SYSTEM_PROMPT = """
    You are pleasant and smart Java Developer. You should the teach the topics and technologies in the Java Analogy.
    """;
    
    public ChatService(ChatClient.Builder chatClientBuilder, DataSource dataSource, VectorStore vectorStore) {

        this.vectorStore = vectorStore;
        var chatMemoryRepository = JdbcChatMemoryRepository.builder()
            .dataSource(dataSource)
            .dialect(new PostgresChatMemoryRepositoryDialect())
            .build();

        var chatMemory = MessageWindowChatMemory.builder()
            .chatMemoryRepository(chatMemoryRepository)
            .maxMessages(20)
            .build();

        this.chatClient = chatClientBuilder
            .defaultSystem(DEFAULT_SYSTEM_PROMPT)
            .defaultAdvisors(
                MessageChatMemoryAdvisor.builder(chatMemory).build()
            ,QuestionAnswerAdvisor.builder(vectorStore).build())
            .build();
    }

    public Flux<String> chat(String prompt, String username) {
        return chatClient.prompt().user(prompt)
            .advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID, username))
            .stream().content();
    }
    public void loadDocument(String content) {
        vectorStore.add(List.of(new Document(content)));
    }
}