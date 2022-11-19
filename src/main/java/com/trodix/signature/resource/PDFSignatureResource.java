package com.trodix.signature.resource;

import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.Certificate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import javax.annotation.security.RolesAllowed;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.BeanParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.eclipse.microprofile.jwt.Claims;
import org.eclipse.microprofile.jwt.JsonWebToken;
import com.arjuna.ats.jta.exceptions.NotImplementedException;
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
import com.trodix.signature.service.PDFSignService;
import io.quarkus.security.ForbiddenException;
import lombok.extern.slf4j.Slf4j;

@Path("/api/sign")
@RolesAllowed({"app-user"})
@Slf4j
public class PDFSignatureResource {

    private final PDFSignService signService;

    private final SignatureMapper signatureMapper;

    private final JsonWebToken jwt;

    public PDFSignatureResource(final PDFSignService signService, final SignatureMapper signatureMapper, final JsonWebToken jwt) {
        this.signService = signService;
        this.signatureMapper = signatureMapper;
        this.jwt = jwt;
    }

    @POST
    public SignedDocumentResponse signPDF(@BeanParam final SignRequest signRequest) throws IOException {

        final UUID documentId = UUID.randomUUID();

        final SignTaskModel signTaskModel = new SignTaskModel();
        signTaskModel.setTmpDocument(signRequest.getDocument());
        signTaskModel.setOriginalFileName(signRequest.getDocument().fileName());
        signTaskModel.setSenderEmail(jwt.getClaim(Claims.email));
        signTaskModel.setRecipientEmail(jwt.getClaim(Claims.email));
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
                    PDFSignService.loadKeystore(signRequest.getCert().uploadedFile().toAbsolutePath().toString(), signRequest.getP12Password());
            final PrivateKey pk = PDFSignService.getPrivateKey(keystore, signRequest.getP12Password(), signRequestModel.getKeyAlias());
            final Certificate[] chain = PDFSignService.getChainCertificates(keystore, signRequestModel.getKeyAlias());

            signRequestModel.setOriginalFile(signRequest.getDocument().uploadedFile().toFile());
            signRequestModel.setOriginalFileName(signRequest.getDocument().fileName());
            signRequestModel.setProvider(providerName);
            signRequestModel.setChain(chain);
            signRequestModel.setPk(pk);
            signRequestModel.setReason(signRequest.getReason());
            signRequestModel.setLocation(signRequest.getLocation());
            signRequestModel.setSenderEmail(jwt.getClaim(Claims.email));
            signRequestModel.setSignPageNumber(signRequest.getSignPageNumber());
            signRequestModel.setSignXPos(signRequest.getSignXPos());
            signRequestModel.setSignYPos(signRequest.getSignYPos());
            signRequestModel.setDocumentId(documentId);

            final SignedDocumentModel signedDocumentModel = this.signService.signPdf(signRequestModel);

            return signatureMapper.signedDocumentModelToSignedDocumentResponse(signedDocumentModel);
        } catch (GeneralSecurityException | IOException e) {
            e.printStackTrace();
        }

        throw new BadRequestException("Unable to sign the document with the provided data");
    }

    @POST
    @Path("/{documentId}")
    public SignedDocumentResponse signPDFTask(final UUID documentId, @BeanParam final SignRequestTaskRequest signRequest) throws IOException {

        final SignTaskModel signTaskModel = this.signService.getTaskDocumentModel(documentId);

        if (!new EqualsBuilder().append(signTaskModel.getRecipientEmail(), this.jwt.getClaim(Claims.email)).isEquals()) {
            throw new ForbiddenException(this.jwt.getClaim(Claims.email) + " don't match the task recipient for documentId " + documentId);
        }

        final SignRequestTaskModel signRequestTaskModel = new SignRequestTaskModel();

        final BouncyCastleProvider provider = new BouncyCastleProvider();
        Security.addProvider(provider);
        final String providerName = provider.getName();

        try {
            final KeyStore keystore =
                    PDFSignService.loadKeystore(signRequest.getCert().uploadedFile().toAbsolutePath().toString(), signRequest.getP12Password());
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
            signRequestTaskModel.setSenderEmail(jwt.getClaim(Claims.email));

            final SignedDocumentModel signedDocumentModel = this.signService.signPdf(signRequestTaskModel);

            return signatureMapper.signedDocumentModelToSignedDocumentResponse(signedDocumentModel);
        } catch (GeneralSecurityException | IOException e) {
            e.printStackTrace();
        }

        throw new BadRequestException("Unable to sign the document with the provided data");
    }

    @GET
    @Path("/{documentId}")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response downloadSignedDocument(@PathParam(value = "documentId") final UUID documentId) {

        final SignedDocumentModel signedDocumentModel = this.signService.getSignedDocumentModel(documentId);

        if (!new EqualsBuilder().append(signedDocumentModel.getSignTaskModel().getSenderEmail(), this.jwt.getClaim(Claims.email)).isEquals()) {
            throw new ForbiddenException(this.jwt.getClaim(Claims.email) + " don't match the task sender for documentId " + documentId);
        }

        try {
            final File document = this.signService.getSignedDocument(documentId);
            final ResponseBuilder response = Response.ok(document);
            response.header("Content-Disposition", "attachment; filename=" + document);

            return response.build();
        } finally {
            this.signService.markSignedDocumentAsDownloaded(documentId);
        }

    }

    @GET
    @Path("/list")
    public List<SignedDocumentResponse> getSignedDocuments() {
        final List<SignedDocumentModel> result = this.signService.getSignedDocuments().stream()
                .filter(i -> i.getSignTaskModel().getSenderEmail().equals(this.jwt.getClaim(Claims.email))).toList();
        return signatureMapper.signedDocumentModelListToSignResponseList(result);
    }

    @GET
    @Path("/preview/{documentId}")
    @Produces("application/pdf")
    public Response previewSignedDocument(@PathParam(value = "documentId") final UUID documentId) {

        final SignedDocumentModel signedDocumentModel = this.signService.getSignedDocumentModel(documentId);

        if (!new EqualsBuilder().append(signedDocumentModel.getSignTaskModel().getSenderEmail(), this.jwt.getClaim(Claims.email)).isEquals()) {
            throw new ForbiddenException(this.jwt.getClaim(Claims.email) + " don't match the task sender for documentId " + documentId);
        }

        final File document = this.signService.getSignedDocument(documentId);
        final ResponseBuilder response = Response.ok(document);
        response.header("Content-Disposition", "inline; filename=" + document);

        return response.build();
    }

    @POST
    @Path("/task")
    public SignTaskResponse createSignTask(@BeanParam final CreateSignTaskRequest signTaskRequest) throws IOException {

        final SignTaskModel signTaskModel = SignatureMapper.signTaskRequestToSignTaskModel(signTaskRequest);
        signTaskModel.setOriginalFileName(signTaskRequest.getDocument().fileName());
        signTaskModel.setSenderEmail(jwt.getClaim(Claims.email));

        final SignTaskModel result = this.signService.createSignTask(signTaskModel);

        return this.signatureMapper.signTaskModelToCreateSignTaskResponse(result);
    }

    @GET
    @Path("/task/{documentId}")
    public List<SignTaskResponse> getSignTask(final UUID documentId) throws NotImplementedException {

        final SignTaskModel signTaskModel = this.signService.getTaskDocumentModel(documentId);

        if (!new EqualsBuilder().append(signTaskModel.getRecipientEmail(), this.jwt.getClaim(Claims.email)).isEquals()) {
            throw new ForbiddenException(this.jwt.getClaim(Claims.email) + " don't match the task recipient for documentId " + documentId);
        }

        throw new NotImplementedException();
    }

    @GET
    @Path("/task/list")
    public List<SignTaskResponse> getTaskDocuments() {
        final List<SignTaskModel> result =
                this.signService.getTaskDocuments().stream().filter(i -> i.getRecipientEmail().equals(this.jwt.getClaim(Claims.email))).toList();
        return signatureMapper.signTaskModelListToSignTaskResponseList(result);
    }

    @GET
    @Path("/task/preview/{documentId}")
    @Produces("application/pdf")
    public Response previewSignTaskDocument(@PathParam(value = "documentId") final UUID documentId) {

        final SignTaskModel signTaskModel = this.signService.getTaskDocumentModel(documentId);

        if (!new EqualsBuilder().append(signTaskModel.getRecipientEmail(), this.jwt.getClaim(Claims.email)).isEquals()) {
            throw new ForbiddenException(this.jwt.getClaim(Claims.email) + " don't match the task recipient for documentId " + documentId);
        }

        final File document = this.signService.getTaskDocument(documentId);
        final ResponseBuilder response = Response.ok(document);
        response.header("Content-Disposition", "inline; filename=" + document);

        return response.build();
    }

}
