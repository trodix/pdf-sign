package com.trodix.signature.model;

import java.io.File;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import com.itextpdf.signatures.PdfSigner.CryptoStandard;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class SignRequestModel {

    private File originalFile;
    private Certificate[] chain;
    private PrivateKey pk;
    private String digestAlgorithm;
    private String provider;
    private CryptoStandard signatureType;
    private String reason;
    private String location;

}
