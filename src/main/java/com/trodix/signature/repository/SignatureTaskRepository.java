package com.trodix.signature.repository;

import java.util.UUID;
import javax.enterprise.context.ApplicationScoped;
import javax.transaction.Transactional;
import com.trodix.signature.entity.SignTaskEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepository;

@ApplicationScoped
@Transactional
public class SignatureTaskRepository implements PanacheRepository<SignTaskEntity> {

    public SignTaskEntity findByDocumentId(final UUID documentId) {
        return find("documentId", documentId).firstResult();
    }

    public void deleteByDocumentId(final UUID documentId) {
        delete("documentId", documentId);
    }

}
