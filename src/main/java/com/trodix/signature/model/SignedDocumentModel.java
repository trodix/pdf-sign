package com.trodix.signature.model;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SignedDocumentModel {

    private UUID documentId;

    private String originalFileName;

    private List<SignatureHistoryElementModel> signatureHistory;

    public List<SignatureHistoryElementModel> addHistoryElement(SignatureHistoryElementModel signatureHistory) {
        if (this.signatureHistory == null) {
            this.signatureHistory = new ArrayList<>();
        }
        this.signatureHistory.add(signatureHistory);
        return this.getSignatureHistory();
    }

}
