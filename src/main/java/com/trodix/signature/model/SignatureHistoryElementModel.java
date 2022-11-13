package com.trodix.signature.model;

import java.util.Date;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SignatureHistoryElementModel {

    private String signedBy;

    private Date signedAt;

}
