package com.trodix.signature.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import com.trodix.signature.repository.SignTaskRepository;
import com.trodix.signature.repository.SignatureHistoryElementRepository;
import com.trodix.signature.repository.SignedDocumentRepository;
import lombok.extern.slf4j.Slf4j;

@SpringBootTest
@Slf4j
public class SignTaskEntityTest {

    @Autowired
    private SignTaskRepository signTaskRepository;

    @Autowired
    private SignedDocumentRepository signedDocumentRepository;

    @Autowired
    private SignatureHistoryElementRepository signatureHistoryElementRepository;

    private final UUID documentId;

    public SignTaskEntityTest() {
        this.documentId = UUID.randomUUID();
    }

    @BeforeEach
    public void init() {
        final List<SignatureHistoryElementEntity> listHistory = new ArrayList<>();

        final SignatureHistoryElementEntity signatureHistoryElementEntity = new SignatureHistoryElementEntity();
        signatureHistoryElementEntity.setSignedBy("s1@trodix.com");
        signatureHistoryElementEntity.setSignedAt(LocalDateTime.now());
        listHistory.add(signatureHistoryElementEntity);

        final SignTaskEntity signTaskEntity = new SignTaskEntity();
        signTaskEntity.setDocumentId(documentId);
        signTaskEntity.setRecipientEmail("s1@trodix.com");
        signTaskEntity.setSenderEmail("s2@trodix.com");
        signTaskEntity.setOriginalFileName("fichier-original.pdf");

        final SignedDocumentEntity signedDocumentEntity = new SignedDocumentEntity();
        signedDocumentEntity.setOriginalFileName("fichier-original.pdf");
        signedDocumentEntity.setSignatureHistory(listHistory);
        signedDocumentEntity.setSignTask(signTaskEntity);

        signedDocumentRepository.saveAndFlush(signedDocumentEntity);
    }

    @Test
    @Order(1)
    void saveSignTaskEntity() {
        log.info("saveSignTaskEntity: documentId is {}", documentId);
        final List<SignTaskEntity> signTaskEntityResultList = signTaskRepository.findAll();
        assertEquals(1, signTaskEntityResultList.size());

        final SignTaskEntity signTaskEntityResult = signTaskEntityResultList.get(0);
        assertEquals(documentId, signTaskEntityResult.getDocumentId());
        assertEquals("s1@trodix.com", signTaskEntityResult.getRecipientEmail());
        assertEquals("s2@trodix.com", signTaskEntityResult.getSenderEmail());
        assertEquals("fichier-original.pdf", signTaskEntityResult.getOriginalFileName());
        assertNotNull(signTaskEntityResult.getSignedDocument());
        assertNotNull(signTaskEntityResult.getSignedDocument().getSignatureHistory());
    }

    @Test
    @Order(2)
    void deleteSignTaskEntity() {
        log.info("deleteSignTaskEntity: documentId is {}", documentId);

        final List<SignTaskEntity> signTaskEntityResultList = signTaskRepository.findAll();

        final SignTaskEntity signTaskEntityResult = signTaskEntityResultList.get(0);
        signTaskRepository.delete(signTaskEntityResult);
        assertEquals(0, signTaskRepository.findAll().size());

        assertEquals(0, signedDocumentRepository.findAll().size());
        assertEquals(0, signatureHistoryElementRepository.findAll().size());
    }

}
