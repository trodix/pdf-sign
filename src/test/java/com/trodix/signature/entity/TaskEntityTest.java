// package com.trodix.signature.entity;

// import static org.junit.jupiter.api.Assertions.assertEquals;
// import static org.junit.jupiter.api.Assertions.assertNotNull;

// import java.util.UUID;

// import com.trodix.signature.repository.TaskRepository;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.boot.test.context.SpringBootTest;
// import com.trodix.signature.repository.SignatureHistoryEntryRepository;
// import com.trodix.signature.repository.DocumentRepository;
// import lombok.extern.slf4j.Slf4j;

// @SpringBootTest
// @Slf4j
// public class TaskEntityTest {

//     @Autowired
//     private TaskRepository taskRepository;

//     @Autowired
//     private DocumentRepository signedDocumentRepository;

//     @Autowired
//     private SignatureHistoryEntryRepository signatureHistoryEntryRepository;

//     private final UUID documentId;

//     public TaskEntityTest() {
//         this.documentId = UUID.randomUUID();
//     }

//    @BeforeEach
//    public void init() {
//        final List<SignatureHistoryEntryEntity> listHistory = new ArrayList<>();

//        final SignatureHistoryEntryEntity signatureHistoryEntryEntity = new SignatureHistoryEntryEntity();
//        signatureHistoryEntryEntity.setSignedBy("s1@trodix.com");
//        listHistory.add(signatureHistoryEntryEntity);

//        final TaskEntity taskEntity = new TaskEntity();
//        taskEntity.setDocumentId(documentId);
//        taskEntity.setSenderEmail("s2@trodix.com");
//        taskEntity.setOriginalFileName("fichier-original.pdf");

//        final DocumentEntity documentEntity = new DocumentEntity();
//        documentEntity.setSignatureHistory(listHistory);
//        documentEntity.setSignTask(taskEntity);

//        signedDocumentRepository.saveAndFlush(documentEntity);
//    }

//    @Test
//    @Order(1)
//    void saveSignTaskEntity() {
//        log.info("saveSignTaskEntity: documentId is {}", documentId);
//        final List<TaskEntity> taskEntityResultList = signTaskRepository.findAll();
//        assertEquals(1, taskEntityResultList.size());

//        final TaskEntity taskEntityResult = taskEntityResultList.get(0);
//        assertEquals(documentId, taskEntityResult.getDocumentId());
//        assertEquals("s2@trodix.com", taskEntityResult.getSenderEmail());
//        assertEquals("fichier-original.pdf", taskEntityResult.getOriginalFileName());
//        assertNotNull(taskEntityResult.getDocument());
//        assertNotNull(taskEntityResult.getDocument().getSignatureHistory());
//    }

//    @Test
//    @Order(2)
//    void deleteSignTaskEntity() {
//        log.info("deleteSignTaskEntity: documentId is {}", documentId);

//        final List<TaskEntity> taskEntityResultList = signTaskRepository.findAll();

//        final TaskEntity taskEntityResult = taskEntityResultList.get(0);
//        signTaskRepository.delete(taskEntityResult);
//        assertEquals(0, signTaskRepository.findAll().size());

//        assertEquals(0, signedDocumentRepository.findAll().size());
//        assertEquals(0, signatureHistoryEntryRepository.findAll().size());
//    }

// }
