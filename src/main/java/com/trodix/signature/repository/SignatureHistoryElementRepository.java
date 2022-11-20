package com.trodix.signature.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.trodix.signature.entity.SignedDocumentEntity;

@Repository
public interface SignatureHistoryElementRepository extends JpaRepository<SignedDocumentEntity, Long> {

}
