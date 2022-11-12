package com.trodix.signature.resource;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.Certificate;
import java.util.Enumeration;
import java.util.List;
import java.util.UUID;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import com.itextpdf.signatures.DigestAlgorithms;
import com.itextpdf.signatures.PdfSigner.CryptoStandard;
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

    public PDFSignatureResource(final PDFSignService signService, SignatureMapper signatureMapper) {
        this.signService = signService;
        this.signatureMapper = signatureMapper;
    }

    @POST
    //@Consumes(MediaType.MULTIPART_FORM_DATA)
    public SignResponse signPDF(final SignRequest signRequest) throws IOException {

        final String keystoreFilePath = getClass().getClassLoader().getResource("/certs/keyStore.p12").getPath();
        // log.info("Keystore file loaded from: {}", keystoreFilePath);

        final File src = new File(getClass().getClassLoader().getResource("dummy.pdf").getPath());
        // log.info("PDF loaded from: " + src);

        final CryptoStandard cryptoStandard = CryptoStandard.CMS;
        final String digestAlgorithm = DigestAlgorithms.SHA256;
        // final String p12Password = "password";
        final String keyAlias = "1";

        // final String reason = "want to sign a pdf";
        // final String location = "right here";
        // final float signaturePositionX = 36;
        // final float signaturePositionY = 648;
        // final File file = null;

        final BouncyCastleProvider provider = new BouncyCastleProvider();
        Security.addProvider(provider);
        final String providerName = provider.getName();

        try {
            //final KeyStore keystore = this.signService.loadKeystore(signRequest.getCert().uploadedFile().toAbsolutePath().toString(), signRequest.getP12Password());
            final KeyStore keystore = this.signService.loadKeystore(keystoreFilePath, signRequest.getP12Password());
            final PrivateKey pk = this.signService.getPrivateKey(keystore, signRequest.getP12Password(), keyAlias);
            final Certificate[] chain = this.signService.getChainCertificates(keystore, keyAlias);

            final SignRequestModel signRequestModel = new SignRequestModel();
            //signRequestModel.setOriginalFile(signRequest.getDocument().uploadedFile().toFile());
            signRequestModel.setOriginalFile(src);
            signRequestModel.setProvider(providerName);
            signRequestModel.setChain(chain);
            signRequestModel.setPk(pk);
            signRequestModel.setDigestAlgorithm(digestAlgorithm);
            signRequestModel.setSignatureType(cryptoStandard);
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
        List<SignedDocumentModel> result = this.signService.getSignedDocuments();
        return signatureMapper.signedDocumentModelListToSignResponseList(result);
    }

}
