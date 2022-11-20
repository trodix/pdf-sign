package com.trodix.signature.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.trodix.signature.entity.SignedDocumentEntity;

@Repository
public interface SignedDocumentRepository extends JpaRepository<SignedDocumentEntity, Long> {

    public Optional<SignedDocumentEntity> findByDocumentId(UUID documentId);

    public List<SignedDocumentEntity> findByDownloaded(boolean downloaded);

}
