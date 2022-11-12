package com.trodix.signature.entity;

import java.util.UUID;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import lombok.Data;

@Entity
@Data
public class SignatureDocumentEntity {

    @Id
    @GeneratedValue
    private Long id;

    private UUID documentId;

    private String originalName;

    private String signedName;
    
}
