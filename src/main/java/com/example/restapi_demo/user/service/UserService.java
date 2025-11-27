package com.example.restapi_demo.user.service;

import com.example.restapi_demo.user.model.User;
import java.util.List;

public interface UserService {

    User register(String email, String password, String passwordConfirm,
                  String nickname, String profileImage);

    User findById(Long id);

    User findByEmail(String email);

    User updateProfile(Long id, String nickname, String profileImage);

    User authenticate(String email, String password);

    boolean deleteMe(Long id);

    ChangePasswordResult changePassword(Long id, String newPassword, String newPasswordConfirm);



    List<User> findByNicknameKeyword(String keyword);


    boolean existsByEmail(String email);


    long countByNickname(String nickname);

    class ChangePasswordResult {
        public final boolean success;
        public final List<String[]> errors;
        public ChangePasswordResult(boolean success, List<String[]> errors) {
            this.success = success;
            this.errors = errors;
        }
    }
}
