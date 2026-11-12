package com.company.equipment_request_service.service;

import com.company.equipment_request_service.dto.ActionVersionDto;
import com.company.equipment_request_service.dto.RejectRequestDto;
import com.company.equipment_request_service.dto.RequestDetailDto;
import com.company.equipment_request_service.entity.*;
import com.company.equipment_request_service.repository.EquipmentRequestRepository;
import com.company.equipment_request_service.repository.RequestStatusHistoryRepository;
import com.company.equipment_request_service.repository.UserRepository;
import com.company.equipment_request_service.security.CurrentUser;
import com.company.equipment_request_service.security.CurrentUserProvider;
import com.company.equipment_request_service.web.error.InvalidStatusTransitionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EquipmentRequestLifecycleTest {

    @Mock EquipmentRequestRepository requestRepo;
    @Mock UserRepository userRepo;
    @Mock RequestStatusHistoryRepository historyRepo;
    @Mock CurrentUserProvider currentUserProvider;

    EquipmentRequestService service;

    @BeforeEach
    void setUp() {
        service = new EquipmentRequestService(requestRepo, userRepo, historyRepo, currentUserProvider);
    }

    private User user() {
        User u = new User();
        u.setId(1L); u.setLogin("ivanov"); u.setDisplayName("Иванов И.И.");
        u.setRole(UserRole.EMPLOYEE); u.setCreatedAt(Instant.now());
        return u;
    }

    private EquipmentRequest req(RequestStatus status, Integer version) {
        EquipmentRequest r = new EquipmentRequest();
        r.setId(10L); r.setAuthor(user()); r.setEquipmentName("Monitor");
        r.setQuantity(1); r.setJustification("test");
        r.setStatus(status); r.setVersion(version);
        r.setCreatedAt(Instant.now()); r.setUpdatedAt(Instant.now());
        return r;
    }

    private void stubSuccess(EquipmentRequest r) {
        when(currentUserProvider.currentUser()).thenReturn(new CurrentUser(1L, UserRole.EMPLOYEE));
        when(userRepo.findById(anyLong())).thenReturn(Optional.of(user()));
        when(requestRepo.findById(10L)).thenReturn(Optional.of(r));
        when(requestRepo.saveAndFlush(any(EquipmentRequest.class))).thenAnswer(i -> i.getArgument(0));
    }

    @Test void submit_fromDraft_ok() {
        EquipmentRequest r = req(RequestStatus.DRAFT, 0); stubSuccess(r);
        RequestDetailDto submit = service.submit(10L, new ActionVersionDto(0));
        assertThat(submit.status()).isEqualTo(RequestStatus.UNDER_REVIEW);
        verify(requestRepo).saveAndFlush(r);
        verify(historyRepo).save(any(RequestStatusHistory.class));
    }

    @Test void approve_fromUnderReview_ok() {
        EquipmentRequest r = req(RequestStatus.UNDER_REVIEW, 1);
        stubSuccess(r); // Сначала базовая настройка (создаст EMPLOYEE id=1)
        when(currentUserProvider.currentUser()).thenReturn(new CurrentUser(999L, UserRole.APPROVER));
        RequestDetailDto approve = service.approve(10L, new ActionVersionDto(1));
        assertThat(approve.status()).isEqualTo(RequestStatus.APPROVED);
        verify(requestRepo).saveAndFlush(r);
        verify(historyRepo).save(any(RequestStatusHistory.class));
    }

    @Test void reject_fromUnderReview_savesComment() {
        EquipmentRequest r = req(RequestStatus.UNDER_REVIEW, 1);
        stubSuccess(r); // Сначала базовая настройка
        when(currentUserProvider.currentUser()).thenReturn(new CurrentUser(999L, UserRole.APPROVER));
        RequestDetailDto reject = service.reject(10L, new RejectRequestDto("bad", 1));
        assertThat(reject.status()).isEqualTo(RequestStatus.REJECTED);
        assertThat(reject.decisionComment()).isEqualTo("bad");
        verify(requestRepo).saveAndFlush(r);
        verify(historyRepo).save(any(RequestStatusHistory.class));
    }

    @Test void cancel_fromDraft_ok() {
        EquipmentRequest r = req(RequestStatus.DRAFT, 0); stubSuccess(r);
        RequestDetailDto cancel = service.cancel(10L, new ActionVersionDto(0));
        assertThat(cancel.status()).isEqualTo(RequestStatus.CANCELLED);
        verify(requestRepo).saveAndFlush(r);
        verify(historyRepo).save(any(RequestStatusHistory.class));
    }

    @Test void cancel_fromUnderReview_ok() {
        EquipmentRequest r = req(RequestStatus.UNDER_REVIEW, 1); stubSuccess(r);
        RequestDetailDto cancel = service.cancel(10L, new ActionVersionDto(1));
        assertThat(cancel.status()).isEqualTo(RequestStatus.CANCELLED);
        verify(requestRepo).saveAndFlush(r);
        verify(historyRepo).save(any(RequestStatusHistory.class));
    }

    @Test void submit_fromUnderReview_blocked() {
        when(currentUserProvider.currentUser()).thenReturn(new CurrentUser(1L, UserRole.EMPLOYEE));
        EquipmentRequest r = req(RequestStatus.UNDER_REVIEW, 1);
        when(requestRepo.findById(10L)).thenReturn(Optional.of(r));
        assertThatThrownBy(() -> service.submit(10L, new ActionVersionDto(1)))
                .isInstanceOf(InvalidStatusTransitionException.class);
        verify(requestRepo, never()).saveAndFlush(any());
        verify(historyRepo, never()).save(any());
        assertThat(r.getStatus()).isEqualTo(RequestStatus.UNDER_REVIEW);
    }

    @Test void approve_fromDraft_blocked() {
        when(currentUserProvider.currentUser()).thenReturn(new CurrentUser(999L, UserRole.APPROVER));
        EquipmentRequest r = req(RequestStatus.DRAFT, 0);
        when(requestRepo.findById(10L)).thenReturn(Optional.of(r));
        assertThatThrownBy(() -> service.approve(10L, new ActionVersionDto(0)))
                .isInstanceOf(InvalidStatusTransitionException.class);
        verify(requestRepo, never()).saveAndFlush(any());
        verify(historyRepo, never()).save(any());
        assertThat(r.getStatus()).isEqualTo(RequestStatus.DRAFT);
    }

    @Test void reject_fromDraft_blocked() {
        when(currentUserProvider.currentUser()).thenReturn(new CurrentUser(999L, UserRole.APPROVER));
        EquipmentRequest r = req(RequestStatus.DRAFT, 0);
        when(requestRepo.findById(10L)).thenReturn(Optional.of(r));
        assertThatThrownBy(() -> service.reject(10L, new RejectRequestDto("bad", 0)))
                .isInstanceOf(InvalidStatusTransitionException.class);
        verify(requestRepo, never()).saveAndFlush(any());
        verify(historyRepo, never()).save(any());
        assertThat(r.getStatus()).isEqualTo(RequestStatus.DRAFT);
    }

    @Test void cancel_fromApproved_blocked() {
        when(currentUserProvider.currentUser()).thenReturn(new CurrentUser(1L, UserRole.EMPLOYEE));
        EquipmentRequest r = req(RequestStatus.APPROVED, 2);
        when(requestRepo.findById(10L)).thenReturn(Optional.of(r));
        assertThatThrownBy(() -> service.cancel(10L, new ActionVersionDto(2)))
                .isInstanceOf(InvalidStatusTransitionException.class);
        verify(requestRepo, never()).saveAndFlush(any());
        verify(historyRepo, never()).save(any());
        assertThat(r.getStatus()).isEqualTo(RequestStatus.APPROVED);
    }
}