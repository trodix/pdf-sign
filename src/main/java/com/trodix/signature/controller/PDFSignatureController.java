package com.trodix.signature.controller;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.Certificate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import javax.annotation.security.RolesAllowed;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.context.annotation.Scope;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.server.ResponseStatusException;
import com.trodix.signature.dto.request.CreateSignTaskRequest;
import com.trodix.signature.dto.request.SignRequest;
import com.trodix.signature.dto.request.SignRequestTaskRequest;
import com.trodix.signature.dto.response.SignTaskResponse;
import com.trodix.signature.dto.response.SignedDocumentResponse;
import com.trodix.signature.mapper.SignatureMapper;
import com.trodix.signature.model.SignRequestModel;
import com.trodix.signature.model.SignRequestTaskModel;
import com.trodix.signature.model.SignTaskModel;
import com.trodix.signature.model.SignedDocumentModel;
import com.trodix.signature.security.utils.Claims;
import com.trodix.signature.service.PDFSignService;
import lombok.extern.slf4j.Slf4j;

@RestController
@Scope(WebApplicationContext.SCOPE_REQUEST)
@RequestMapping("/api/sign")
@RolesAllowed({"market-customer"})
@Slf4j
public class PDFSignatureController {

    private final PDFSignService signService;

    private final SignatureMapper signatureMapper;

    private final Jwt jwt;


    public PDFSignatureController(final PDFSignService signService, final SignatureMapper signatureMapper, final Jwt jwt) {
        this.signService = signService;
        this.signatureMapper = signatureMapper;
        this.jwt = jwt;
    }

    @PostMapping
    public SignedDocumentResponse signPDF(@ModelAttribute final SignRequest signRequest) throws IOException {

        final UUID documentId = UUID.randomUUID();

        final SignTaskModel signTaskModel = new SignTaskModel();
        signTaskModel.setTmpDocument(signRequest.getDocument());
        signTaskModel.setOriginalFileName(signRequest.getDocument().getOriginalFilename());
        signTaskModel.setSenderEmail(jwt.getClaim(Claims.EMAIL.value));
        signTaskModel.setRecipientEmail(jwt.getClaim(Claims.EMAIL.value));
        signTaskModel.setDueDate(null);
        signTaskModel.setCreatedAt(LocalDateTime.now());
        signTaskModel.setDocumentId(documentId);

        this.signService.createSignTask(signTaskModel);

        final SignRequestModel signRequestModel = new SignRequestModel();

        final BouncyCastleProvider provider = new BouncyCastleProvider();
        Security.addProvider(provider);
        final String providerName = provider.getName();

        try {
            final KeyStore keystore =
                    PDFSignService.loadKeystore(PDFSignService.multipartFileToFile(signRequest.getCert()).getAbsolutePath(), signRequest.getP12Password());
            final PrivateKey pk = PDFSignService.getPrivateKey(keystore, signRequest.getP12Password(), signRequestModel.getKeyAlias());
            final Certificate[] chain = PDFSignService.getChainCertificates(keystore, signRequestModel.getKeyAlias());

            signRequestModel.setOriginalFile(PDFSignService.multipartFileToFile(signRequest.getDocument()));
            signRequestModel.setOriginalFileName(signRequest.getDocument().getOriginalFilename());
            signRequestModel.setProvider(providerName);
            signRequestModel.setChain(chain);
            signRequestModel.setPk(pk);
            signRequestModel.setReason(signRequest.getReason());
            signRequestModel.setLocation(signRequest.getLocation());
            signRequestModel.setSenderEmail(jwt.getClaim(Claims.EMAIL.value));
            signRequestModel.setSignPageNumber(signRequest.getSignPageNumber());
            signRequestModel.setSignXPos(signRequest.getSignXPos());
            signRequestModel.setSignYPos(signRequest.getSignYPos());
            signRequestModel.setDocumentId(documentId);

            final SignedDocumentModel signedDocumentModel = this.signService.signPdf(signRequestModel);

            return signatureMapper.signedDocumentModelToSignedDocumentResponse(signedDocumentModel);
        } catch (GeneralSecurityException | IOException e) {
            e.printStackTrace();
        }

        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unable to sign the document with the provided data");
    }

    @PostMapping("/{documentId}")
    public SignedDocumentResponse signPDFTask(@PathVariable(value = "documentId") final UUID documentId,
            @ModelAttribute final SignRequestTaskRequest signRequest)
            throws IOException {

        final SignTaskModel signTaskModel = this.signService.getTaskDocumentModel(documentId);

        if (!new EqualsBuilder().append(signTaskModel.getRecipientEmail(), jwt.getClaim(Claims.EMAIL.value)).isEquals()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    jwt.getClaim(Claims.EMAIL.value) + " don't match the task recipient for documentId " + documentId);
        }

        final SignRequestTaskModel signRequestTaskModel = new SignRequestTaskModel();

        final BouncyCastleProvider provider = new BouncyCastleProvider();
        Security.addProvider(provider);
        final String providerName = provider.getName();

        try {
            final KeyStore keystore =
                    PDFSignService.loadKeystore(PDFSignService.multipartFileToFile(signRequest.getCert()).getAbsolutePath(), signRequest.getP12Password());
            final PrivateKey pk = PDFSignService.getPrivateKey(keystore, signRequest.getP12Password(), signRequestTaskModel.getKeyAlias());
            final Certificate[] chain = PDFSignService.getChainCertificates(keystore, signRequestTaskModel.getKeyAlias());

            signRequestTaskModel.setDocumentId(documentId);
            signRequestTaskModel.setProvider(providerName);
            signRequestTaskModel.setChain(chain);
            signRequestTaskModel.setPk(pk);
            signRequestTaskModel.setReason(signRequest.getReason());
            signRequestTaskModel.setLocation(signRequest.getLocation());
            signRequestTaskModel.setSignPageNumber(signRequest.getSignPageNumber());
            signRequestTaskModel.setSignXPos(signRequest.getSignXPos());
            signRequestTaskModel.setSignYPos(signRequest.getSignYPos());
            signRequestTaskModel.setSenderEmail(jwt.getClaim(Claims.EMAIL.value));

            final SignedDocumentModel signedDocumentModel = this.signService.signPdf(signRequestTaskModel);

            return signatureMapper.signedDocumentModelToSignedDocumentResponse(signedDocumentModel);
        } catch (GeneralSecurityException | IOException e) {
            e.printStackTrace();
        }

        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unable to sign the document with the provided data");
    }

    @GetMapping(value = "/{documentId}", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<Resource> downloadSignedDocument(@PathVariable(value = "documentId") final UUID documentId)
            throws IOException {

        final SignedDocumentModel signedDocumentModel = this.signService.getSignedDocumentModel(documentId);

        if (!new EqualsBuilder().append(signedDocumentModel.getSignTaskModel().getSenderEmail(), jwt.getClaim(Claims.EMAIL.value)).isEquals()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    jwt.getClaim(Claims.EMAIL.value) + " don't match the task sender for documentId " + documentId);
        }

        try {
            final File document = this.signService.getSignedDocument(documentId);
            final Path path = Paths.get(document.getAbsolutePath());
            final ByteArrayResource resource = new ByteArrayResource(Files.readAllBytes(path));

            final HttpHeaders headers = new HttpHeaders();
            headers.add("Content-Disposition", "attachment; filename=" + document);

            return ResponseEntity
                    .ok()
                    .headers(headers)
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(resource);
        } finally {
            this.signService.markSignedDocumentAsDownloaded(documentId);
        }

    }

    @GetMapping("/list")
    public List<SignedDocumentResponse> getSignedDocuments() {

        final List<SignedDocumentModel> result = this.signService.getSignedDocuments().stream()
                .filter(i -> i.getSignTaskModel().getSenderEmail().equals(jwt.getClaim(Claims.EMAIL.value))).toList();
        return signatureMapper.signedDocumentModelListToSignResponseList(result);
    }

    @GetMapping(value = "/preview/{documentId}", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<Resource> previewSignedDocument(@PathVariable(value = "documentId") final UUID documentId)
            throws IOException {

        final SignedDocumentModel signedDocumentModel = this.signService.getSignedDocumentModel(documentId);

        if (!new EqualsBuilder().append(signedDocumentModel.getSignTaskModel().getSenderEmail(), jwt.getClaim(Claims.EMAIL.value)).isEquals()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    jwt.getClaim(Claims.EMAIL.value) + " don't match the task sender for documentId " + documentId);
        }

        final File document = this.signService.getSignedDocument(documentId);
        final Path path = Paths.get(document.getAbsolutePath());
        final ByteArrayResource resource = new ByteArrayResource(Files.readAllBytes(path));

        final HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "inline; filename=" + document);

        return ResponseEntity
                .ok()
                .headers(headers)
                .contentType(MediaType.APPLICATION_PDF)
                .body(resource);
    }

    @PostMapping("/task")
    public SignTaskResponse createSignTask(@ModelAttribute final CreateSignTaskRequest signTaskRequest) throws IOException {

        final SignTaskModel signTaskModel = SignatureMapper.signTaskRequestToSignTaskModel(signTaskRequest);
        signTaskModel.setOriginalFileName(signTaskRequest.getDocument().getOriginalFilename());
        signTaskModel.setSenderEmail(jwt.getClaim(Claims.EMAIL.value));

        final SignTaskModel result = this.signService.createSignTask(signTaskModel);

        return this.signatureMapper.signTaskModelToCreateSignTaskResponse(result);
    }

    @GetMapping("/task/{documentId}")
    public List<SignTaskResponse> getSignTask(final UUID documentId) throws NotImplementedException {

        final SignTaskModel signTaskModel = this.signService.getTaskDocumentModel(documentId);

        if (!new EqualsBuilder().append(signTaskModel.getRecipientEmail(), jwt.getClaim(Claims.EMAIL.value)).isEquals()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    jwt.getClaim(Claims.EMAIL.value) + " don't match the task recipient for documentId " + documentId);
        }

        throw new NotImplementedException();
    }

    @GetMapping("/task/list")
    public List<SignTaskResponse> getTaskDocuments() {

        final List<SignTaskModel> result =
                this.signService.getTaskDocuments().stream().filter(i -> i.getRecipientEmail().equals(jwt.getClaim(Claims.EMAIL.value))).toList();
        return signatureMapper.signTaskModelListToSignTaskResponseList(result);
    }

    @GetMapping(value = "/task/preview/{documentId}", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<Resource> previewSignTaskDocument(@PathVariable(value = "documentId") final UUID documentId, final HttpServletResponse response,
            final Principal principal) throws IOException {

        final SignTaskModel signTaskModel = this.signService.getTaskDocumentModel(documentId);

        if (!new EqualsBuilder().append(signTaskModel.getRecipientEmail(), jwt.getClaim(Claims.EMAIL.value)).isEquals()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    jwt.getClaim(Claims.EMAIL.value) + " don't match the task recipient for documentId " + documentId);
        }

        final File document = this.signService.getTaskDocument(documentId);
        final Path path = Paths.get(document.getAbsolutePath());
        final ByteArrayResource resource = new ByteArrayResource(Files.readAllBytes(path));

        final HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "inline; filename=" + document);
        return ResponseEntity
                .ok()
                .headers(headers)
                .contentType(MediaType.APPLICATION_PDF)
                .body(resource);
    }

}
