package com.trodix.signature.domain.entity;

import javax.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class UserEntity {

    private Long id;

    @NotNull
    private String email;

}
