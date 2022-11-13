package com.trodix.signature.model;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SignatureHistoryElementModel {

    private String signedBy;

    private LocalDateTime signedAt;

}
