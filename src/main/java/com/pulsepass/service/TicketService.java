package com.pulsepass.service;

import com.pulsepass.dto.request.PurchaseTicketRequest;
import com.pulsepass.dto.response.TicketResponse;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;

public interface TicketService {

    // FR-SECTKT-01: solo USER, y el userEmail del body debe ser el del principal
    @PreAuthorize("hasRole('USER') and @authorizationService.isSelfByEmail(#request.userEmail())")
    TicketResponse purchase(PurchaseTicketRequest request);

    // FR-SECTKT-02: propietario, o STAFF/ADMIN
    @PreAuthorize("hasAnyRole('STAFF','ADMIN') or @authorizationService.isTicketOwner(#ticketCode)")
    TicketResponse findByCode(String ticketCode);

    // FR-SECTKT-03: propietario, o ADMIN
    @PreAuthorize("hasRole('ADMIN') or @authorizationService.isSelfByEmail(#email)")
    List<TicketResponse> findByUserEmail(String email);

    // FR-SECTKT-04
    @PreAuthorize("hasAnyRole('STAFF','ADMIN')")
    List<TicketResponse> findPaidTicketsByEvent(String eventCode);

    // FR-SECTKT-05: ADMIN, o USER propietario. La autorización se evalúa ANTES de mutar estado.
    @PreAuthorize("hasRole('ADMIN') or (hasRole('USER') and @authorizationService.isTicketOwner(#ticketCode))")
    TicketResponse cancel(String ticketCode);

    // FR-SECTKT-06
    @PreAuthorize("hasAnyRole('STAFF','ADMIN')")
    TicketResponse markAsUsed(String ticketCode);
}
