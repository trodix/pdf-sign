package com.trodix.signature.presentation.dto.response;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DocumentResponse {

    private UUID documentId;

    private String originalFileName;

    private boolean downloaded;

}
