package com.trodix.signature.persistance.repository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.Assert;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
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
import com.trodix.signature.domain.entity.SignTaskStatus;
import com.trodix.signature.domain.entity.SignatureHistoryEntryEntity;
import com.trodix.signature.domain.entity.TaskEntity;
import com.trodix.signature.domain.entity.UserEntity;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@Testcontainers
public class TaskRepositoryIT {

    @Autowired
    private TaskRepository taskRepository;

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
    @Order(1)
    public void test_persist() {

        final UserEntity initiator = new UserEntity();
        initiator.setEmail("test1@trodix.com");

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
        signatureHistoryEntryList.add(history1);

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

    @Test
    @Order(2)
    public void test_findByTaskId() {

        final UserEntity initiator = new UserEntity();
        initiator.setId(1L);
        initiator.setEmail("test1@trodix.com");

        final List<DocumentEntity> documentList = new ArrayList<>();
        final DocumentEntity document1 = new DocumentEntity();
        document1.setDocumentId(UUID.fromString("aaf4c614-784d-4d2e-aee4-c93a18a523ee"));
        document1.setOriginalFileName("document1-test.pdf");
        document1.setDownloaded(false);
        documentList.add(document1);

        final DocumentEntity document2 = new DocumentEntity();
        document2.setDocumentId(UUID.fromString("953ca389-ab83-49bb-bacd-f9701b1f63f8"));
        document2.setOriginalFileName("document2-test.pdf");
        document2.setDownloaded(false);
        documentList.add(document2);

        final List<UserEntity> taskRecipientList = new ArrayList<>();
        final UserEntity recipient2 = new UserEntity();
        recipient2.setEmail("test2@trodix.com");
        taskRecipientList.add(recipient2);

        final UserEntity recipient3 = new UserEntity();
        recipient3.setEmail("test3@trodix.com");
        taskRecipientList.add(recipient3);

        final List<SignatureHistoryEntryEntity> signatureHistoryEntryList = new ArrayList<>();

        final SignatureHistoryEntryEntity history1 = new SignatureHistoryEntryEntity();
        history1.setSignedBy(recipient2);
        history1.setSignedAt(LocalDateTime.parse("2022-12-23 10:16:04", DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        signatureHistoryEntryList.add(history1);

        final SignatureHistoryEntryEntity history2 = new SignatureHistoryEntryEntity();
        history2.setSignedBy(recipient3);
        history2.setSignedAt(LocalDateTime.parse("2022-12-23 14:18:05", DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        signatureHistoryEntryList.add(history2);

        final TaskEntity expectedTask = new TaskEntity();
        expectedTask.setTaskId(UUID.fromString("4509bad5-a9f8-4ef8-8fbe-376971abc932"));
        expectedTask.setSignTaskStatus(SignTaskStatus.IN_PROGRESS);
        expectedTask.setDueDate(LocalDateTime.parse("2022-12-24 00:00", DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
        expectedTask.setCreatedAt(LocalDateTime.parse("2022-12-22 08:05:15", DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        expectedTask.setInitiator(initiator);
        expectedTask.setDocumentList(documentList);
        expectedTask.setTaskRecipientList(taskRecipientList);
        expectedTask.setSignatureHistory(signatureHistoryEntryList);


        final Optional<TaskEntity> taskOptional = taskRepository.findByTaskId(expectedTask.getTaskId());

        Assert.assertTrue(taskOptional.isPresent());

        final TaskEntity task = taskOptional.get();
        Assert.assertEquals(expectedTask.getTaskId(), task.getTaskId());
        Assert.assertEquals(expectedTask.getSignTaskStatus(), task.getSignTaskStatus());
        Assert.assertEquals(expectedTask.getDueDate(), task.getDueDate());
        Assert.assertEquals(expectedTask.getCreatedAt(), task.getCreatedAt());
        Assert.assertEquals(expectedTask.getInitiator().getEmail(), task.getInitiator().getEmail());
        Assert.assertEquals(expectedTask.getDocumentList().size(), task.getDocumentList().size());
        Assert.assertEquals(expectedTask.getDocumentList().get(0).getDocumentId(), task.getDocumentList().get(0).getDocumentId());
        Assert.assertEquals(expectedTask.getDocumentList().get(0).getOriginalFileName(), task.getDocumentList().get(0).getOriginalFileName());
        Assert.assertEquals(expectedTask.getDocumentList().get(0).isDownloaded(), task.getDocumentList().get(0).isDownloaded());
        Assert.assertEquals(expectedTask.getDocumentList().get(1).getDocumentId(), task.getDocumentList().get(1).getDocumentId());
        Assert.assertEquals(expectedTask.getDocumentList().get(1).getOriginalFileName(), task.getDocumentList().get(1).getOriginalFileName());
        Assert.assertEquals(expectedTask.getDocumentList().get(1).isDownloaded(), task.getDocumentList().get(1).isDownloaded());
        Assert.assertEquals(expectedTask.getTaskRecipientList().size(), task.getTaskRecipientList().size());
        Assert.assertEquals(expectedTask.getTaskRecipientList().get(0).getEmail(), task.getTaskRecipientList().get(0).getEmail());
        Assert.assertEquals(expectedTask.getTaskRecipientList().get(1).getEmail(), task.getTaskRecipientList().get(1).getEmail());
        Assert.assertEquals(expectedTask.getSignatureHistory().size(), task.getSignatureHistory().size());
        Assert.assertEquals(expectedTask.getSignatureHistory().get(0).getSignedBy().getEmail(), task.getSignatureHistory().get(0).getSignedBy().getEmail());
        Assert.assertEquals(expectedTask.getSignatureHistory().get(0).getSignedAt(), task.getSignatureHistory().get(0).getSignedAt());
        Assert.assertEquals(expectedTask.getSignatureHistory().get(1).getSignedBy().getEmail(), task.getSignatureHistory().get(1).getSignedBy().getEmail());
        Assert.assertEquals(expectedTask.getSignatureHistory().get(1).getSignedAt(), task.getSignatureHistory().get(1).getSignedAt());
    }


}
