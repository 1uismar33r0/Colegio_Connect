package com.colegioconnect.accounts.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.colegioconnect.accounts.entity.EmailCode;

public interface EmailCodeRepository extends JpaRepository<EmailCode, UUID> {

    Optional<EmailCode> findByCode(String code);
}