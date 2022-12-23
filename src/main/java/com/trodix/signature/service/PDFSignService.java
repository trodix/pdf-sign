package com.trodix.signature.service;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
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
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.mail.MessagingException;
import javax.persistence.NoResultException;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
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
import com.trodix.signature.entity.SignTaskEntity;
import com.trodix.signature.entity.SignedDocumentEntity;
import com.trodix.signature.mapper.SignatureMapper;
import com.trodix.signature.model.SignRequestModel;
import com.trodix.signature.model.SignRequestTaskModel;
import com.trodix.signature.model.SignTaskModel;
import com.trodix.signature.model.SignatureHistoryElementModel;
import com.trodix.signature.model.SignedDocumentModel;
import com.trodix.signature.model.SignTaskStatus;
import com.trodix.signature.repository.SignTaskRepository;
import com.trodix.signature.repository.SignedDocumentRepository;
import freemarker.template.TemplateException;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class PDFSignService {

    @Value("${app.signed-file-destination}")
    private String signedFileDestination;

    @Value("${app.frontend.baseurl}")
    private String frontendUrl;

    private final SignatureMapper signatureMapper;

    private final SignedDocumentRepository signedDocumentRepository;

    private final SignTaskRepository signTaskRepository;

    private final EmailService emailService;

    public PDFSignService(final SignatureMapper signatureMapper, final SignedDocumentRepository signedDocumentRepository,
            final SignTaskRepository signTaskRepository, final EmailService emailService) {
        this.signatureMapper = signatureMapper;
        this.signedDocumentRepository = signedDocumentRepository;
        this.signTaskRepository = signTaskRepository;
        this.emailService = emailService;
    }

    public SignedDocumentModel signPdf(final SignRequestTaskModel signRequestTaskModel) throws GeneralSecurityException, IOException {
        final File document = getTaskDocument(signRequestTaskModel.getDocumentId());
        final SignTaskEntity entity = signTaskRepository.findByDocumentId(signRequestTaskModel.getDocumentId()).orElseThrow(NoResultException::new);

        final SignRequestModel signRequestModel = this.signatureMapper.signRequestTaskModelToSignRequestModel(signRequestTaskModel);
        signRequestModel.setOriginalFile(document);
        signRequestModel.setOriginalFileName(entity.getOriginalFileName());

        return signPdfInternal(signRequestModel);
    }

    public SignedDocumentModel signPdf(final SignRequestModel signRequestModel) throws GeneralSecurityException, IOException {
        return signPdfInternal(signRequestModel);
    }

    private SignedDocumentModel signPdfInternal(final SignRequestModel signRequestModel) throws GeneralSecurityException, IOException {

        SignedDocumentModel signedDocumentModel;
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

            final UUID signedDocumentId = signRequestModel.getDocumentId();
            final String signedDocumentName = signedDocumentId.toString() + "-signed" + "." + FilenameUtils.getExtension(originalFileName);
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

            final SignatureHistoryElementModel signatureHistoryElementModel =
                    new SignatureHistoryElementModel(signRequestModel.getSenderEmail(), LocalDateTime.now());

            signedDocumentModel = new SignedDocumentModel();
            signedDocumentModel.setDocumentId(signedDocumentId);
            signedDocumentModel.setOriginalFileName(originalFileName);
            signedDocumentModel.addHistoryElement(signatureHistoryElementModel);

            registerSignedDocument(signedDocumentModel);
        }

        return signedDocumentModel;
    }

    public SignTaskModel createSignTask(final SignTaskModel signTaskModel) throws IOException {

        if (signTaskModel.getDocumentId() == null) {
            signTaskModel.setDocumentId(UUID.randomUUID());
        }

        storeFile(signTaskModel.getTmpDocument(), signTaskModel.getDocumentId());

        final SignTaskEntity entity = this.signatureMapper.signTaskModelToSignTaskEntity(signTaskModel);
        entity.setSignTaskStatus(SignTaskStatus.IN_PROGRESS);
        signTaskRepository.saveAndFlush(entity);

        final SignTaskEntity result = signTaskRepository.findByDocumentId(entity.getDocumentId()).orElseThrow(NoResultException::new);

        final String to = entity.getRecipientEmail();
        final String subject = "[Sign PDF] New Sign request";
        final Map<String, Object> tpl = new HashMap<>();
        tpl.put("senderEmail", entity.getSenderEmail());
        tpl.put("originalFileName", entity.getOriginalFileName());

        final String signDocumentUrl = frontendUrl + "/tasks/preview/" + entity.getDocumentId();
        tpl.put("signDocumentUrl", signDocumentUrl);

        try {
            this.emailService.sendNewSignTaskEmailNotification(to, subject, tpl);
        } catch (IOException | TemplateException | MessagingException e) {
            log.error("Error while sending email notification", e);
        }

        return this.signatureMapper.signTaskEntityToSignTaskModel(result);
    }

    public Path storeFile(final MultipartFile fileUpload, final UUID documentId) throws IOException {
        return storeFile(signedFileDestination, fileUpload, documentId);
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

    public static Path storeFile(final String signedFileDestination, final MultipartFile fileUpload, final UUID documentId) throws IOException {
        final String docExt = FilenameUtils.getExtension(fileUpload.getOriginalFilename());
        final String newName = documentId.toString() + "." + docExt;
        final Path dest = Paths.get(signedFileDestination, newName);
        fileUpload.transferTo(dest);
        return dest;
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

    public Path signedDocumentIdToPath(final UUID documentId) {
        final SignedDocumentModel signedDocumentModel = getSignedDocumentModel(documentId);
        final String ext = FilenameUtils.getExtension(signedDocumentModel.getOriginalFileName());
        final String signedDocumentName = signedDocumentModel.getDocumentId().toString() + "-signed" + "." + ext;
        return Paths.get(signedFileDestination, signedDocumentName);
    }

    public Path taskDocumentIdToPath(final UUID documentId) {
        final SignTaskModel signedDocumentModel = getTaskDocumentModel(documentId);
        if (signedDocumentModel == null) {
            throw new RuntimeException("Sign task not found for documentId " + documentId);
        }
        final String ext = FilenameUtils.getExtension(signedDocumentModel.getOriginalFileName());
        final String signedDocumentName = signedDocumentModel.getDocumentId().toString() + "." + ext;
        return Paths.get(signedFileDestination, signedDocumentName);
    }

    public SignedDocumentModel getSignedDocumentModel(final UUID documentId) {
        final SignedDocumentEntity entity = signedDocumentRepository.findByDocumentId(documentId).orElseThrow(NoResultException::new);

        return signatureMapper.signatureDocumentEntityToSignedDocumentModel(entity);
    }

    public SignTaskModel getTaskDocumentModel(final UUID documentId) {
        final SignTaskEntity entity = signTaskRepository.findByDocumentId(documentId).orElse(null);

        return signatureMapper.signTaskEntityToSignTaskModel(entity);
    }

    public void registerSignedDocument(final SignedDocumentModel signedDocumentModel) {
        final SignedDocumentEntity newEntity = signatureMapper.signedDocumentModelToSignedDocumentEntity(signedDocumentModel);
        final SignTaskEntity signTaskEntity = signTaskRepository.findByDocumentId(signedDocumentModel.getDocumentId()).orElseThrow(NoResultException::new);
        signTaskEntity.setSignTaskStatus(SignTaskStatus.SIGNED);
        newEntity.setSignTask(signTaskEntity);
        try {
            final SignedDocumentEntity oldEntity =
                    signedDocumentRepository.findByDocumentId(signedDocumentModel.getDocumentId()).orElseThrow(NoResultException::new);
            newEntity.setId(oldEntity.getId());
        } catch (final NoResultException e) {
            signedDocumentRepository.saveAndFlush(newEntity);
        }

        final String to = newEntity.getSignTask().getSenderEmail();
        final String subject = "[Sign PDF] Your file has been signed";
        final Map<String, Object> tpl = new HashMap<>();
        tpl.put("senderEmail", newEntity.getSignTask().getSenderEmail());
        tpl.put("recipientEmail", newEntity.getSignTask().getRecipientEmail());
        tpl.put("documentId", newEntity.getSignTask().getDocumentId());
        tpl.put("originalFileName", newEntity.getSignTask().getOriginalFileName());
        tpl.put("createdAt", newEntity.getSignTask().getCreatedAt());
        tpl.put("dueDate", newEntity.getSignTask().getDueDate());

        final String downloadUrl = frontendUrl + "/tasks/preview/" + newEntity.getSignTask().getDocumentId();
        tpl.put("downloadUrl", downloadUrl);

        try {
            this.emailService.sendSignedDocumentEmailNotification(to, subject, tpl);
        } catch (IOException | TemplateException | MessagingException e) {
            log.error("Error while sending email notification", e);
        }
    }

    public void markSignedDocumentAsDownloaded(final UUID documentId) {
        final SignedDocumentEntity entity = signedDocumentRepository.findByDocumentId(documentId).orElseThrow(NoResultException::new);
        entity.setDownloaded(true);
        entity.getSignTask().setSignTaskStatus(SignTaskStatus.DOWNLOADED);
        signedDocumentRepository.saveAndFlush(entity);

        // FIXME downloadedSignedDocumentsCleaner();

        final String to = entity.getSignTask().getRecipientEmail();
        final String subject = "[Sign PDF] Your file has been downloaded";
        final Map<String, Object> tpl = new HashMap<>();
        tpl.put("senderEmail", entity.getSignTask().getSenderEmail());
        tpl.put("originalFileName", entity.getSignTask().getOriginalFileName());

        try {
            this.emailService.sendDownloadedDocumentEmailNotification(to, subject, tpl);
        } catch (IOException | TemplateException | MessagingException e) {
            log.error("Error while sending email notification", e);
        }
    }

    public void deleteSignedDocument(final UUID documentId) {
        try {
            final File signedDocument = getSignedDocument(documentId);
            Files.delete(signedDocument.toPath());
        } catch (final IOException e) {
            log.error("Error while deleting signed document {}", documentId, e);
        }
    }

    public void deleteTask(final UUID documentId) {
        try {
            final File taskDocument = getTaskDocument(documentId);
            Files.delete(taskDocument.toPath());

            final SignTaskEntity entity = signTaskRepository.findByDocumentId(documentId).orElseThrow(NoResultException::new);
            entity.getSignedDocument().setSignatureHistory(null);
            entity.setSignedDocument(null);
            signTaskRepository.saveAndFlush(entity);

            // SignedDocumentEntity entity = SignedDocumentEntity.findByDocumentId(documentId);
            // entity.setSignTask(null);
            // entity.persistAndFlush();

            signTaskRepository.deleteByDocumentId(documentId);
            deleteSignedDocument(documentId);
        } catch (final IOException e) {
            log.error("Error while deleting task {}", documentId, e);
        }
    }

    public File getSignedDocument(final UUID documentId) {
        File document;

        try {
            final Path path = signedDocumentIdToPath(documentId);
            document = path.toFile();
            return document;
        } catch (final RuntimeException e) {
            log.error("Error while reading the document from id: {}", documentId);
        }

        throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    }

    public File getTaskDocument(final UUID documentId) {
        File document;

        try {
            final Path path = taskDocumentIdToPath(documentId);
            document = path.toFile();
            return document;
        } catch (final RuntimeException e) {
            log.error("Error while reading the document from id: {}", documentId);
        }

        throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    }

    public List<SignedDocumentModel> getSignedDocuments() {
        final List<SignedDocumentEntity> entities = signedDocumentRepository.findAll();
        return signatureMapper.signedDocumentEntityListToSignedDocumentModelList(entities);
    }

    public List<SignTaskModel> getTaskDocumentsForUser(final String email) {
        final List<SignTaskEntity> entities = signTaskRepository.findForUser(email);
        return signatureMapper.signTaskEntityListToSignTaskModelList(entities);
    }

    protected void downloadedSignedDocumentsCleaner() {
        final List<SignedDocumentEntity> downloadedDocuments = signedDocumentRepository.findByDownloaded(true);
        downloadedDocuments.forEach(document -> deleteTask(document.getDocumentId()));
    }

    public static File multipartFileToFile(final MultipartFile file) throws IOException {
        final File convFile = new File(file.getOriginalFilename());
        convFile.createNewFile();

        try (FileOutputStream fos = new FileOutputStream(convFile)) {
            fos.write(file.getBytes());
        }

        return convFile;
    }

}
