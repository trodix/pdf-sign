package com.trodix.signature.dto.response;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SignResponse {

    private UUID documentId;

    private String originalName;

    private String signedName;

}
