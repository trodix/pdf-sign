package com.trodix.signature.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.trodix.signature.entity.SignTaskEntity;

@Repository
public interface SignTaskRepository extends JpaRepository<SignTaskEntity, Long> {

    public Optional<SignTaskEntity> findByDocumentId(UUID documentId);

    public void deleteByDocumentId(UUID documentId);

}
