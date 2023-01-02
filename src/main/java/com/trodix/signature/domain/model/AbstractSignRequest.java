package com.trodix.signature.domain.model;

import com.itextpdf.signatures.DigestAlgorithms;
import com.itextpdf.signatures.PdfSigner;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.util.UUID;

@Data
@AllArgsConstructor
public abstract class AbstractSignRequest {

    private UUID taskId;
    private String senderEmail;
    private Certificate[] chain;
    private PrivateKey pk;
    private String digestAlgorithm;
    private String provider;
    private PdfSigner.CryptoStandard signatureType;
    private String reason;
    private String location;
    private String keyAlias;
    private Integer signPageNumber;
    private Float signXPos;
    private Float signYPos;

    public AbstractSignRequest() {
        this.signatureType = PdfSigner.CryptoStandard.CMS;
        this.digestAlgorithm = DigestAlgorithms.SHA256;
        this.keyAlias = "1";
    }

}
