package com.trodix.signature.service;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.NotFoundException;
import org.apache.commons.io.FilenameUtils;
import org.eclipse.microprofile.config.inject.ConfigProperty;
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
import com.trodix.signature.entity.SignedDocumentEntity;
import com.trodix.signature.mapper.SignatureMapper;
import com.trodix.signature.model.SignRequestModel;
import com.trodix.signature.model.SignatureHistoryElementModel;
import com.trodix.signature.model.SignedDocumentModel;
import com.trodix.signature.repository.SignatureRepository;
import lombok.extern.slf4j.Slf4j;

@ApplicationScoped
@Slf4j
public class PDFSignService {

    @ConfigProperty(name = "app.signed-file-destination", defaultValue = "/tmp")
    private String signedFileDestination;

    private final SignatureRepository signatureRepository;

    private final SignatureMapper signatureMapper;

    public PDFSignService(final SignatureRepository signatureRepository, final SignatureMapper signatureMapper) {
        this.signatureRepository = signatureRepository;
        this.signatureMapper = signatureMapper;
    }

    public SignedDocumentModel signPdf(final SignRequestModel signRequestModel)
            throws GeneralSecurityException, IOException {

        final String originalFilePath = signRequestModel.getOriginalFile().getPath();
        final String originalFileName = FilenameUtils.getName(signRequestModel.getOriginalFileName());

        try (final PdfReader reader = new PdfReader(originalFilePath);
                final PdfReader reader2 = new PdfReader(originalFilePath);
                PdfDocument document = new PdfDocument(reader)) {

            // Create the signature appearance

            // ** Get width and height of whole page
            final int pageNumber = signRequestModel.getSignPageNumber() != null ? signRequestModel.getSignPageNumber() : document.getNumberOfPages();
            final Rectangle pdfLastPage = document.getLastPage().getPageSize();
            final float SIGN_RECT_SIZE_WIDTH = 200;
            final float SIGN_RECT_SIZE_HEIGHT = 100;
            final float x = signRequestModel.getSignXPos() != null ? signRequestModel.getSignXPos() : pdfLastPage.getRight() - SIGN_RECT_SIZE_WIDTH;
            final float y = signRequestModel.getSignYPos() != null ? signRequestModel.getSignYPos() : pdfLastPage.getBottom() + SIGN_RECT_SIZE_HEIGHT;
            final Rectangle signRect = new Rectangle(x, y, SIGN_RECT_SIZE_WIDTH, SIGN_RECT_SIZE_HEIGHT);
            log.info("Signature area defined to pageNumber={}, x={}, y={}", pageNumber, x, y);

            final String signedDocumentName =
                    FilenameUtils.getBaseName(originalFileName) + "-" + "signed-" + new Timestamp(System.currentTimeMillis()).getTime() + "."
                            + FilenameUtils.getExtension(originalFileName);
            final String dest = signedFileDestination + "/" + signedDocumentName;

            final PdfSigner signer = new PdfSigner(reader2, new FileOutputStream(dest), new StampingProperties());
            final PdfSignatureAppearance appearance = signer.getSignatureAppearance();
            appearance
                    .setReason(signRequestModel.getReason())
                    .setLocation(signRequestModel.getLocation())
                    .setPageRect(signRect)
                    .setPageNumber(pageNumber);

            signer.setFieldName("sig");

            final IExternalDigest digest = new BouncyCastleDigest();
            final IExternalSignature pks =
                    new PrivateKeySignature(signRequestModel.getPk(), signRequestModel.getDigestAlgorithm(), signRequestModel.getProvider());

            // Sign the document using the detached mode, CMS or CAdES equivalent.
            signer.signDetached(digest, pks, signRequestModel.getChain(), null, null, null, 0, signRequestModel.getSignatureType());
            log.info("New PDF file has been signed and exported to: " + dest);
            
            verifySignatures(dest);

            final SignatureHistoryElementModel signatureHistoryElementModel = new SignatureHistoryElementModel(signRequestModel.getSenderEmail(), new Date());

            final SignedDocumentModel signedDocumentModel = new SignedDocumentModel();
            signedDocumentModel.setDocumentId(UUID.randomUUID());
            signedDocumentModel.setOriginalFileName(originalFileName);
            signedDocumentModel.setSignedDocumentName(signedDocumentName);
            signedDocumentModel.addHistoryElement(signatureHistoryElementModel);

            registerSignedDocument(signedDocumentModel);

            return signedDocumentModel;
        }

    }

    public PrivateKey getPrivateKey(final KeyStore keystore, final String p12Password, final String keyAlias)
            throws NoSuchAlgorithmException, KeyStoreException, UnrecoverableKeyException {

        final PrivateKey key = (PrivateKey) keystore.getKey(keyAlias, p12Password.toCharArray());

        if (key == null) {
            throw new UnrecoverableKeyException("Key is null, alias used: " + keyAlias);
        }

        log.info("Loaded Private key has {} algorithm", key.getAlgorithm());

        return key;
    }

    public KeyStore loadKeystore(final String path, final String p12Password)
            throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {
        final KeyStore keystore = KeyStore.getInstance("PKCS12");
        try (InputStream is = new BufferedInputStream(new FileInputStream(path))) {
            keystore.load(is, p12Password.toCharArray());
            return keystore;
        }
    }

    public Certificate[] getChainCertificates(final KeyStore keystore, final String keyAlias) throws KeyStoreException {
        final Certificate[] chain = keystore.getCertificateChain(keyAlias);
        log.info("Chain certificates from keystore: {}", Arrays.toString(chain));

        return chain;
    }

    public void verifySignatures(final String path) throws IOException, GeneralSecurityException {
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

    private PdfPKCS7 verifySignature(final SignatureUtil signUtil, final String name) throws GeneralSecurityException {
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

    public Path documentIdToPath(final UUID documentId) {
        final SignedDocumentModel signedDocumentModel = getSignDocumentModel(documentId);
        final String signedDocumentName = signedDocumentModel.getSignedDocumentName();
        final Path path = Paths.get(signedFileDestination, signedDocumentName);
        return path;
    }

    public SignedDocumentModel getSignDocumentModel(final UUID documentId) {
        final SignedDocumentEntity entity = this.signatureRepository.findByDocumentId(documentId);

        return signatureMapper.signatureDocumentEntityToSignedDocumentModel(entity);
    }

    public void registerSignedDocument(final SignedDocumentModel signedDocumentModel) {
        final SignedDocumentEntity entity = signatureMapper.signedDocumentModelToSignedDocumentEntity(signedDocumentModel);
        signatureRepository.persistAndFlush(entity);
    }

    public void deleteSignedDocument(final UUID documentId) {
        signatureRepository.deleteByDocumentId(documentId);
    }

    public File getDocument(final UUID documentId) {
        File document;

        try {
            final Path path = documentIdToPath(documentId);
            document = path.toFile();
            return document;
        } catch (final RuntimeException e) {
            log.info("Error while reading the document from id: {}", documentId);
        }

        throw new NotFoundException();
    }

    public List<SignedDocumentModel> getSignedDocuments() {
        final List<SignedDocumentEntity> entities = signatureRepository.findAll().stream().toList();
        return signatureMapper.signatureDocumentEntityListToSignedDocumentModelList(entities);
    }

}
