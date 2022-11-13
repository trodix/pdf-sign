package com.trodix.signature.dto.request;

import javax.ws.rs.core.MediaType;
import org.jboss.resteasy.reactive.PartType;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.multipart.FileUpload;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SignRequest {

    @RestForm
    private String reason;

    @RestForm
    private String location;

    @RestForm
    private String p12Password;

    @RestForm
    private FileUpload document;

    @RestForm
    private FileUpload cert;

}
