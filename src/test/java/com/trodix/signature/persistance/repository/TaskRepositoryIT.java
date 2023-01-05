package com.trodix.signature.persistance.repository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Ports;
import com.trodix.signature.domain.entity.DocumentEntity;
import com.trodix.signature.domain.entity.SignatureHistoryEntryEntity;
import com.trodix.signature.domain.entity.TaskEntity;
import com.trodix.signature.domain.entity.UserEntity;
import com.trodix.signature.domain.model.SignTaskStatus;
import com.trodix.signature.domain.model.User;
import com.trodix.signature.domain.service.UserService;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@Testcontainers
public class TaskRepositoryIT {

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private UserService userService;

    @Container
    public static PostgreSQLContainer container = new PostgreSQLContainer<>(DockerImageName.parse("postgres:15.0"))
            .withDatabaseName("pdf-sign")
            .withCreateContainerCmdModifier(cmd -> cmd.withHostConfig(
                    new HostConfig().withPortBindings(new PortBinding(Ports.Binding.bindPort(5440), new ExposedPort(5432)))));;

    @BeforeAll
    public static void setUp() {
        container.withReuse(true);
        container.start();
    }

    @DynamicPropertySource
    public static void overrideProperties(final DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", container::getJdbcUrl);
        registry.add("spring.datasource.username", container::getUsername);
        registry.add("spring.datasource.password", container::getPassword);
        registry.add("spring.datasource.driver-class-name", container::getDriverClassName);
    }

    @AfterAll
    public static void tearDown() {
        container.stop();
    }

    @Test
    public void testPersistTask() {

        final User initiatorModel = userService.getOrCreateUser("test1@trodix.com");
        final UserEntity initiator = new UserEntity();
        initiator.setId(initiatorModel.getId());
        initiator.setEmail(initiatorModel.getEmail());

        final List<DocumentEntity> documentList = new ArrayList<>();
        final DocumentEntity document1 = new DocumentEntity();
        document1.setDocumentId(UUID.randomUUID());
        document1.setOriginalFileName("document1-test.pdf");
        document1.setDownloaded(false);
        documentList.add(document1);

        final DocumentEntity document2 = new DocumentEntity();
        document2.setDocumentId(UUID.randomUUID());
        document2.setOriginalFileName("document2-test.pdf");
        document2.setDownloaded(false);
        documentList.add(document2);

        final List<UserEntity> taskRecipientList = new ArrayList<>();
        final UserEntity recipient1 = new UserEntity();
        recipient1.setEmail("test2@trodix.com");
        taskRecipientList.add(recipient1);

        final UserEntity recipient2 = new UserEntity();
        recipient2.setEmail("test3@trodix.com");
        taskRecipientList.add(recipient2);

        final List<SignatureHistoryEntryEntity> signatureHistoryEntryList = new ArrayList<>();
        final SignatureHistoryEntryEntity history1 = new SignatureHistoryEntryEntity();
        history1.setSignedBy(recipient2);
        history1.setSignedAt(LocalDateTime.parse("2022-12-14 11:30", DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));

        final TaskEntity taskEntity = new TaskEntity();
        taskEntity.setTaskId(UUID.randomUUID());
        taskEntity.setSignTaskStatus(SignTaskStatus.IN_PROGRESS);
        taskEntity.setDueDate(LocalDateTime.parse("2022-12-15 14:30", DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
        taskEntity.setInitiator(initiator);
        taskEntity.setDocumentList(documentList);
        taskEntity.setTaskRecipientList(taskRecipientList);
        taskEntity.setSignatureHistory(signatureHistoryEntryList);

        taskRepository.persist(taskEntity);
    }


}
