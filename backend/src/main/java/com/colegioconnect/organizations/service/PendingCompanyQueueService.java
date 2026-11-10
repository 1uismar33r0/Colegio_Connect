package com.colegioconnect.organizations.service;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.colegioconnect.organizations.dto.PendingCompanyPageResponse;
import com.colegioconnect.organizations.entity.ApprovalStatus;
import com.colegioconnect.organizations.entity.OrgType;
import com.colegioconnect.organizations.repository.OrganizationRepository;

@Service
public class PendingCompanyQueueService {
    private static final int MAX_PAGE_SIZE = 100;

    private final OrganizationRepository organizations;

    public PendingCompanyQueueService(OrganizationRepository organizations) {
        this.organizations = organizations;
    }

    @Transactional(readOnly = true)
    public PendingCompanyPageResponse findPending(int page, int size) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "page must be non-negative and size must be between 1 and 100");
        }
        PageRequest request = PageRequest.of(page, size,
                Sort.by(Sort.Direction.ASC, "name", "id"));
        return PendingCompanyPageResponse.from(organizations.findByOrgTypeAndApprovalStatus(
                OrgType.EXTERNAL_COMPANY, ApprovalStatus.PENDING, request));
    }
}
