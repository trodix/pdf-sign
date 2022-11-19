

package com.trodix.signature.entity;

import java.time.LocalDateTime;
import javax.persistence.Entity;
import io.quarkus.hibernate.orm.panache.PanacheEntity;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class SignatureHistoryElementEntity extends PanacheEntity {

    private String signedBy;

    private LocalDateTime signedAt;

}
