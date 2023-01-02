package com.trodix.signature.domain.service;

import com.trodix.signature.domain.entity.UserEntity;
import com.trodix.signature.domain.model.User;
import com.trodix.signature.mapper.UserMapper;
import com.trodix.signature.persistance.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserRepository userRepository;

    private final UserMapper userMapper;

    public UserService(UserRepository userRepository, UserMapper userMapper) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
    }

    public User getOrCreateUser(String email) {
        UserEntity user = this.userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            user = new UserEntity();
            user.setEmail(email);
            this.userRepository.persist(user);
            UserEntity createdUser = this.userRepository.findByEmail(email).orElseThrow();
            return userMapper.userEntityToUserModel(createdUser);
        }
        return userMapper.userEntityToUserModel(user);
    }

}
