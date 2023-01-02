package com.trodix.signature.presentation.dto.request;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.multipart.MultipartFile;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateTaskRequest {

    @NotEmpty(message = "documentList cannot be empty.")
    private List<@Valid MultipartFile> documentList;

    @NotNull
    private List<TaskRecipientRequest> recipientList;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime dueDate;

}
