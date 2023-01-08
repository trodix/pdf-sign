

package com.trodix.signature.domain.entity;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class SignatureHistoryEntryEntity {

    private Long id;

    private UserEntity signedBy;

    private LocalDateTime signedAt;

}
