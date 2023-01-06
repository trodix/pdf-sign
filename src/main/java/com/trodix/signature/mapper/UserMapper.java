package com.trodix.signature.mapper;

import org.mapstruct.Mapper;
import com.trodix.signature.domain.entity.UserEntity;
import com.trodix.signature.domain.model.User;

@Mapper(componentModel = "spring")
public interface UserMapper {

    public User userEntityToUserModel(UserEntity userEntity);

}
