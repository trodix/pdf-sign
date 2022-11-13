
package com.trodix.signature.entity;

import java.util.List;
import java.util.UUID;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import io.quarkus.hibernate.orm.panache.PanacheEntity;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class SignedDocumentEntity extends PanacheEntity {

    private UUID documentId;

    private String originalFileName;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @JoinColumn(name = "signed_document_id", referencedColumnName = "id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private List<SignatureHistoryElementEntity> signatureHistory;

    public static SignedDocumentEntity findByDocumentId(final UUID documentId) {
        return find("documentId", documentId).singleResult();
    }

    public static void deleteByDocumentId(final UUID documentId) {
        delete("documentId", documentId);
    }

}
