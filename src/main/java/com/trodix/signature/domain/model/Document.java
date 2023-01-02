package com.trodix.signature.domain.model;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Document {

    private String originalFileName;

    private UUID documentId;

    private Task task;

    private boolean downloaded;

}
