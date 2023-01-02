package com.trodix.signature.domain.service;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.StampingProperties;
import com.itextpdf.signatures.BouncyCastleDigest;
import com.itextpdf.signatures.IExternalDigest;
import com.itextpdf.signatures.IExternalSignature;
import com.itextpdf.signatures.PdfPKCS7;
import com.itextpdf.signatures.PdfSignatureAppearance;
import com.itextpdf.signatures.PdfSigner;
import com.itextpdf.signatures.PrivateKeySignature;
import com.itextpdf.signatures.SignatureUtil;
import com.trodix.signature.domain.model.Document;
import com.trodix.signature.domain.model.SignRequestOptions;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class SignatureService {

    @Value("${app.signed-file-destination}")
    private String fileStoragePath;

    private final DocumentService documentService;

    public SignatureService(final DocumentService documentService) {
        this.documentService = documentService;
    }

    public void signPdf(final SignRequestOptions signRequestOptions, final Document documentModel) throws GeneralSecurityException, IOException {

        final File file = this.documentService.getDocument(documentModel);
        final String originalFilePath = file.getPath();
        final String originalFileName = FilenameUtils.getName(documentModel.getOriginalFileName());

        try (final PdfReader reader = new PdfReader(originalFilePath);
                final PdfReader reader2 = new PdfReader(originalFilePath);
                PdfDocument document = new PdfDocument(reader)) {

            // Create the signature appearance

            // ** Get width and height of whole page
            final int pageNumber = signRequestOptions.getSignPageNumber() != null ? signRequestOptions.getSignPageNumber() : document.getNumberOfPages();
            final Rectangle pdfLastPage = document.getLastPage().getPageSize();
            final float SIGN_RECT_SIZE_WIDTH = 200;
            final float SIGN_RECT_SIZE_HEIGHT = 100;
            final float x = signRequestOptions.getSignXPos() != null ? signRequestOptions.getSignXPos() : pdfLastPage.getRight() - SIGN_RECT_SIZE_WIDTH;
            final float y = signRequestOptions.getSignYPos() != null ? signRequestOptions.getSignYPos() : pdfLastPage.getBottom() + SIGN_RECT_SIZE_HEIGHT;
            final Rectangle signRect = new Rectangle(x, y, SIGN_RECT_SIZE_WIDTH, SIGN_RECT_SIZE_HEIGHT);
            log.info("Signature area defined to pageNumber={}, x={}, y={}", pageNumber, x, y);

            final UUID signedDocumentId = documentModel.getDocumentId();
            final String signedDocumentName = signedDocumentId.toString() + "-signed" + "." + FilenameUtils.getExtension(originalFileName);
            final String dest = fileStoragePath + "/" + signedDocumentName;

            final PdfSigner signer = new PdfSigner(reader2, new FileOutputStream(dest), new StampingProperties());
            final PdfSignatureAppearance appearance = signer.getSignatureAppearance();
            appearance
                    .setReason(signRequestOptions.getReason())
                    .setLocation(signRequestOptions.getLocation())
                    .setPageRect(signRect)
                    .setPageNumber(pageNumber);

            signer.setFieldName("sig");

            final IExternalDigest digest = new BouncyCastleDigest();
            final IExternalSignature pks =
                    new PrivateKeySignature(signRequestOptions.getPk(), signRequestOptions.getDigestAlgorithm(), signRequestOptions.getProvider());

            // Sign the document using the detached mode, CMS or CAdES equivalent.
            signer.signDetached(digest, pks, signRequestOptions.getChain(), null, null, null, 0, signRequestOptions.getSignatureType());
            log.info("New PDF file has been signed and exported to: " + dest);

            verifySignatures(dest);
        }

    }

    public static PrivateKey getPrivateKey(final KeyStore keystore, final String p12Password, final String keyAlias)
            throws NoSuchAlgorithmException, KeyStoreException, UnrecoverableKeyException {

        final PrivateKey key = (PrivateKey) keystore.getKey(keyAlias, p12Password.toCharArray());

        if (key == null) {
            throw new UnrecoverableKeyException("Key is null, alias used: " + keyAlias);
        }

        log.info("Loaded Private key has {} algorithm", key.getAlgorithm());

        return key;
    }

    public static KeyStore loadKeystore(final String path, final String p12Password)
            throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {
        final KeyStore keystore = KeyStore.getInstance("PKCS12");
        try (InputStream is = new BufferedInputStream(new FileInputStream(path))) {
            keystore.load(is, p12Password.toCharArray());
            return keystore;
        }
    }

    public static Certificate[] getChainCertificates(final KeyStore keystore, final String keyAlias) throws KeyStoreException {
        final Certificate[] chain = keystore.getCertificateChain(keyAlias);
        log.info("Chain certificates from keystore: {}", Arrays.toString(chain));

        return chain;
    }

    public static void verifySignatures(final String path) throws IOException, GeneralSecurityException {
        log.info("Verifying signatures for: {}", path);

        final PdfDocument pdfDoc = new PdfDocument(new PdfReader(path));
        final SignatureUtil signUtil = new SignatureUtil(pdfDoc);
        final List<String> names = signUtil.getSignatureNames();
        log.info("Signatures found: {}", names.size());

        for (final String name : names) {
            log.info("===== {} =====", name);
            verifySignature(signUtil, name);
        }

        pdfDoc.close();
    }

    private static PdfPKCS7 verifySignature(final SignatureUtil signUtil, final String name) throws GeneralSecurityException {
        final PdfPKCS7 pkcs7 = signUtil.readSignatureData(name);

        log.info("Signature covers whole document: {}", signUtil.signatureCoversWholeDocument(name));
        log.info("Document revision: {} of {}", signUtil.getRevision(name), signUtil.getTotalRevisions());
        log.info("Integrity check OK? {}", pkcs7.verifySignatureIntegrityAndAuthenticity());

        return pkcs7;
    }

    public static Certificate[] getCertificate(final String certPath) throws CertificateException, IOException {
        final CertificateFactory factory = CertificateFactory.getInstance("X.509");
        final Certificate[] chain = new Certificate[1];
        try (InputStream stream = new FileInputStream(certPath)) {
            chain[0] = factory.generateCertificate(stream);
        }
        return chain;
    }

}
