package com.trodix.signature.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.trodix.signature.entity.SignTaskEntity;

@Repository
public interface SignTaskRepository extends JpaRepository<SignTaskEntity, Long> {

    public Optional<SignTaskEntity> findByDocumentId(UUID documentId);

    @Query("SELECT t FROM SignTaskEntity t WHERE (t.senderEmail = :email AND t.signTaskStatus IN ('IN_PROGRESS', 'SIGNED', 'REJECTED')) OR (t.recipientEmail = :email AND t.signTaskStatus IN ('IN_PROGRESS'))")
    public List<SignTaskEntity> findForUser(@Param("email") String email);

    public void deleteByDocumentId(UUID documentId);

}
