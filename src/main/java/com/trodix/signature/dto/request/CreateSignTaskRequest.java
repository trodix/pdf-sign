package com.trodix.signature.dto.request;

import java.time.LocalDateTime;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.multipart.FileUpload;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateSignTaskRequest {

    @RestForm
    private FileUpload document;

    @RestForm
    private String senderEmail;

    @RestForm
    private String recipientEmail;

    @RestForm
    private LocalDateTime dueDate;

}