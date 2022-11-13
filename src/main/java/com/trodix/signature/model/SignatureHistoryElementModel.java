package com.trodix.signature.model;

import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SignatureHistoryElementModel {

    private String signedBy;

    private LocalDate signedAt;

}
