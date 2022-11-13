package com.trodix.signature.repository;

import java.util.UUID;
import javax.enterprise.context.ApplicationScoped;
import javax.transaction.Transactional;
import com.trodix.signature.entity.SignedDocumentEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepository;

@ApplicationScoped
@Transactional
public class SignatureRepository implements PanacheRepository<SignedDocumentEntity> {

    public SignedDocumentEntity findByDocumentId(UUID documentId) {
        return find("documentId", documentId).firstResult();
    }

    public void deleteByDocumentId(UUID documentId) {
        delete("documentId", documentId);
    }
    
}
