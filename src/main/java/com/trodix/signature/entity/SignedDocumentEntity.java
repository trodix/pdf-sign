
package com.trodix.signature.entity;

import java.util.List;
import java.util.UUID;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import lombok.Data;

@Entity
@Data
public class SignedDocumentEntity {

    @Id
    @GeneratedValue
    private Long id;

    private UUID documentId;

    private String originalFileName;

    private String signedDocumentName;

    @OneToMany(cascade = {CascadeType.ALL})
    @JoinColumn(name = "signed_document_id", referencedColumnName = "id")
    private List<SignatureHistoryElementEntity> signatureHistory;

}
