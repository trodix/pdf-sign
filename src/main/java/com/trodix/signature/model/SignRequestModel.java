package com.trodix.signature.model;

import java.io.File;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.util.UUID;
import com.itextpdf.signatures.DigestAlgorithms;
import com.itextpdf.signatures.PdfSigner.CryptoStandard;
import lombok.AllArgsConstructor;
import lombok.Data;


@Data
@AllArgsConstructor
public class SignRequestModel {

    private File originalFile;
    private String originalFileName;
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
    private UUID documentId;

    public SignRequestModel() {
        this.signatureType = CryptoStandard.CMS;
        this.digestAlgorithm = DigestAlgorithms.SHA256;
        this.keyAlias = "1";
    }

}
