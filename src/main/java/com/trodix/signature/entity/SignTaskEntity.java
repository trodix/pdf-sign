package com.trodix.signature.entity;

import java.time.LocalDateTime;
import java.util.UUID;
import javax.persistence.Entity;
import org.hibernate.annotations.CreationTimestamp;
import io.quarkus.hibernate.orm.panache.PanacheEntity;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class SignTaskEntity extends PanacheEntity {

    private UUID documentId;

    private String originalFileName;

    private String senderEmail;

    private String recipientEmail;

    private LocalDateTime dueDate;

    @CreationTimestamp
    private LocalDateTime createdAt;

    public static SignTaskEntity findByDocumentId(UUID documentId) {
        return find("documentId", documentId).singleResult();
    }

    public static void deleteByDocumentId(UUID documentId) {
        delete("documentId", documentId);
    }

}
