package com.trodix.signature.model;

import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.util.UUID;
import com.itextpdf.signatures.DigestAlgorithms;
import com.itextpdf.signatures.PdfSigner.CryptoStandard;
import lombok.AllArgsConstructor;
import lombok.Data;


@Data
@AllArgsConstructor
public class SignRequestTaskModel {

    private UUID documentId;
    private Certificate[] chain;
    private PrivateKey pk;
    private String senderEmail;
    private String digestAlgorithm;
    private String provider;
    private CryptoStandard signatureType;
    private String reason;
    private String location;
    private String keyAlias;
    private Integer signPageNumber;
    private Float signXPos;
    private Float signYPos;

    public SignRequestTaskModel() {
        this.signatureType = CryptoStandard.CMS;
        this.digestAlgorithm = DigestAlgorithms.SHA256;
        this.keyAlias = "1";
    }

}