

package com.trodix.signature.entity;

import java.time.LocalDate;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import lombok.Data;

@Entity
@Data
public class SignatureHistoryElementEntity {

    @Id
    @GeneratedValue
    private Long id;

    private String signedBy;

    private LocalDate signedAt;

}
