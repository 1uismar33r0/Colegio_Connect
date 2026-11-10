package com.colegioconnect.organizations.dto;

import java.util.List;

import org.springframework.data.domain.Page;

import com.colegioconnect.organizations.entity.Organization;

public record PendingCompanyPageResponse(
        List<PendingCompanyResponse> items,
        int page,
        int size,
        long totalElements,
        int totalPages) {

    public static PendingCompanyPageResponse from(Page<Organization> organizations) {
        return new PendingCompanyPageResponse(
                organizations.getContent().stream().map(PendingCompanyResponse::from).toList(),
                organizations.getNumber(),
                organizations.getSize(),
                organizations.getTotalElements(),
                organizations.getTotalPages());
    }
}
