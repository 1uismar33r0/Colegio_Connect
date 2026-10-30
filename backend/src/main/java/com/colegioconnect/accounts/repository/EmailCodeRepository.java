package com.colegioconnect.accounts.repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.colegioconnect.accounts.entity.EmailCode;
import com.colegioconnect.accounts.entity.User;

public interface EmailCodeRepository extends JpaRepository<EmailCode, UUID> {

    Optional<EmailCode> findFirstByUserOrderByExpiresAtDesc(User user);

    long countByUserAndCreatedAtAfter(User user, Instant since);
}