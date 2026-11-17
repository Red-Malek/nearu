package com.nearu.backend.servicerequest;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ServiceRequestRepository extends JpaRepository<ServiceRequest, UUID> {
}
