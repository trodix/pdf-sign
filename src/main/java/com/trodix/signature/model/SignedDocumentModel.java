package com.trodix.signature.model;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SignedDocumentModel {

    private UUID documentId;
    
    private String originalName;

    private String signedName;

}
