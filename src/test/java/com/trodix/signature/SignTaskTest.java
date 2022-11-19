package com.trodix.signature;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import com.trodix.signature.entity.SignTaskEntity;
import com.trodix.signature.entity.SignatureHistoryElementEntity;
import com.trodix.signature.entity.SignedDocumentEntity;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@TestInstance(Lifecycle.PER_CLASS)
@Transactional
public class SignTaskTest {

    @Test
    void createSignTask() {


        final List<SignatureHistoryElementEntity> listHistory = new ArrayList<>();

        final SignatureHistoryElementEntity lh = new SignatureHistoryElementEntity();
        lh.setSignedBy("s1@trodix.com");
        lh.setSignedAt(LocalDateTime.now());
        listHistory.add(lh);

        final UUID documentId = UUID.randomUUID();
        final SignTaskEntity p = new SignTaskEntity();
        p.setDocumentId(documentId);
        p.setRecipientEmail("s1@trodix.com");
        p.setSenderEmail("s2@trodix.com");
        p.setOriginalFileName("fichier-original.pdf");

        final SignedDocumentEntity c = new SignedDocumentEntity();
        c.setOriginalFileName("fichier-original.pdf");
        c.setSignatureHistory(listHistory);
        c.setSignTask(p);

        c.persistAndFlush();

        SignTaskEntity p1 = (SignTaskEntity) SignTaskEntity.listAll().get(0);
        final SignedDocumentEntity c1 = (SignedDocumentEntity) SignedDocumentEntity.listAll().get(0);

        assertEquals(1, SignTaskEntity.listAll().size());
        assertEquals("s1@trodix.com", p1.getRecipientEmail());
        assertNotNull(p1.getSignedDocument());
        assertNotNull(p1.getSignedDocument().getSignatureHistory());
        assertTrue(c1.getSignatureHistory().size() == 1);
    }

    @Test
    void deleteParentCascadeChild() {

        final List<SignatureHistoryElementEntity> listHistory = new ArrayList<>();

        final SignatureHistoryElementEntity lh = new SignatureHistoryElementEntity();
        lh.setSignedBy("s1@trodix.com");
        lh.setSignedAt(LocalDateTime.now());
        listHistory.add(lh);

        final UUID documentId = UUID.randomUUID();
        final SignTaskEntity p = new SignTaskEntity();
        p.setDocumentId(documentId);
        p.setRecipientEmail("s1@trodix.com");
        p.setSenderEmail("s2@trodix.com");
        p.setOriginalFileName("fichier-original.pdf");

        final SignedDocumentEntity c = new SignedDocumentEntity();
        c.setOriginalFileName("fichier-original.pdf");
        c.setSignatureHistory(listHistory);
        c.setSignTask(p);

        c.persistAndFlush();

        final SignTaskEntity p1 = (SignTaskEntity) SignTaskEntity.listAll().get(0);
        final SignedDocumentEntity c1 = (SignedDocumentEntity) SignedDocumentEntity.listAll().get(0);

        p1.delete();

        assertTrue(SignTaskEntity.listAll().size() == 0);
    }

}
