package com.trodix.signature.dto.response;

import java.util.List;
import java.util.UUID;
import com.trodix.signature.model.SignatureHistoryElementModel;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SignedDocumentResponse {

    private UUID documentId;

    private String originalFileName;

    private String signedDocumentName;

    private List<SignatureHistoryElementModel> signatureHistory;

}
