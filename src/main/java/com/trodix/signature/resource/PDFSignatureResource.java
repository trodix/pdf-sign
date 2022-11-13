package com.trodix.signature.resource;

import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.Certificate;
import java.util.List;
import java.util.UUID;
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
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import com.trodix.signature.dto.request.SignRequest;
import com.trodix.signature.dto.response.SignResponse;
import com.trodix.signature.mapper.SignatureMapper;
import com.trodix.signature.model.SignRequestModel;
import com.trodix.signature.model.SignedDocumentModel;
import com.trodix.signature.service.PDFSignService;
import lombok.extern.slf4j.Slf4j;

@Path("/api/sign")
@Slf4j
public class PDFSignatureResource {

    private final PDFSignService signService;

    private final SignatureMapper signatureMapper;

    public PDFSignatureResource(final PDFSignService signService, final SignatureMapper signatureMapper) {
        this.signService = signService;
        this.signatureMapper = signatureMapper;
    }

    @POST
    public SignResponse signPDF(@BeanParam final SignRequest signRequest) throws IOException {

        final SignRequestModel signRequestModel = new SignRequestModel();

        final BouncyCastleProvider provider = new BouncyCastleProvider();
        Security.addProvider(provider);
        final String providerName = provider.getName();

        try {
            final KeyStore keystore =
                    this.signService.loadKeystore(signRequest.getCert().uploadedFile().toAbsolutePath().toString(), signRequest.getP12Password());
            final PrivateKey pk = this.signService.getPrivateKey(keystore, signRequest.getP12Password(), signRequestModel.getKeyAlias());
            final Certificate[] chain = this.signService.getChainCertificates(keystore, signRequestModel.getKeyAlias());

            signRequestModel.setOriginalFile(signRequest.getDocument().uploadedFile().toFile());
            signRequestModel.setOriginalFileName(signRequest.getDocument().fileName());
            signRequestModel.setProvider(providerName);
            signRequestModel.setChain(chain);
            signRequestModel.setPk(pk);
            signRequestModel.setReason(signRequest.getReason());
            signRequestModel.setLocation(signRequest.getLocation());

            final SignedDocumentModel signedDocumentModel = this.signService.signPdf(signRequestModel);

            return signatureMapper.signedDocumentModelToSignResponse(signedDocumentModel);
        } catch (GeneralSecurityException | IOException e) {
            e.printStackTrace();
        }

        throw new BadRequestException("Unable to sign the document with the provided data");
    }

    @GET
    @Path("/{documentId}")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response getSignedDocument(@PathParam(value = "documentId") final UUID documentId) {

        final File document = this.signService.getSignedDocument(documentId);
        final ResponseBuilder response = Response.ok(document);
        response.header("Content-Disposition", "attachment;filename=" + document);

        this.signService.deleteSignedDocument(documentId);

        return response.build();
    }

    @GET
    @Path("/list")
    public List<SignResponse> getSignedDocuments() {
        final List<SignedDocumentModel> result = this.signService.getSignedDocuments();
        return signatureMapper.signedDocumentModelListToSignResponseList(result);
    }

}
