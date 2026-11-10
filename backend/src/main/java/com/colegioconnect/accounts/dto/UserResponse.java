package com.colegioconnect.accounts.dto;

import java.util.List;
import java.util.UUID;

import com.colegioconnect.accounts.entity.AccountType;
import com.colegioconnect.accounts.entity.User;
import com.colegioconnect.organizations.entity.Role;

public record UserResponse(UUID id, String email, AccountType accountType, List<Role> roles) {

    public static UserResponse from(User user, List<Role> roles) {
        return new UserResponse(user.getId(), user.getEmail(), user.getAccountType(), List.copyOf(roles));
    }
}
