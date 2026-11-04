package com.colegioconnect.accounts.dto;

import java.util.UUID;

import com.colegioconnect.accounts.entity.AccountType;
import com.colegioconnect.accounts.entity.User;

public record UserResponse(UUID id, String email, AccountType accountType) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getAccountType());
    }
}