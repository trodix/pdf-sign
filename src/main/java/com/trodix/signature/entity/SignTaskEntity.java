package com.trodix.signature.entity;

import java.time.LocalDateTime;
import java.util.UUID;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.OneToOne;
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

    @OneToOne(
            fetch = FetchType.LAZY,
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            mappedBy = "signTask")
    private SignedDocumentEntity signedDocument;

    public static SignTaskEntity findByDocumentId(final UUID documentId) {
        return find("documentId", documentId).singleResult();
    }

    public static void deleteByDocumentId(final UUID documentId) {
        delete("documentId", documentId);
    }

}
