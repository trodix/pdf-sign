
package com.trodix.signature.entity;

import java.util.List;
import java.util.UUID;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
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

    private boolean downloaded;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
    @JoinColumn(name = "signed_document_id", referencedColumnName = "id")
    //@OnDelete(action = OnDeleteAction.CASCADE)
    private List<SignatureHistoryElementEntity> signatureHistory;

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @JoinColumn(name = "sign_task_id")
    private SignTaskEntity signTask;

    public static SignedDocumentEntity findByDocumentId(final UUID documentId) {
        return find("documentId", documentId).singleResult();
    }

    public static void deleteByDocumentId(final UUID documentId) {
        delete("documentId", documentId);
    }

    public static List<SignedDocumentEntity> findDownloaded() {
        return find("downloaded", true).list();
    }

}
