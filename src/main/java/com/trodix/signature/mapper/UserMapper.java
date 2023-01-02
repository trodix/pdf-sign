package com.trodix.signature.mapper;

import com.trodix.signature.domain.entity.UserEntity;
import com.trodix.signature.domain.model.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {

    public User userEntityToUserModel(UserEntity userEntity);

}
