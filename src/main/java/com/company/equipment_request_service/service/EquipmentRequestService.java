package com.company.equipment_request_service.service;

import com.company.equipment_request_service.dto.*;
import com.company.equipment_request_service.entity.*;
import com.company.equipment_request_service.repository.EquipmentRequestRepository;
import com.company.equipment_request_service.repository.UserRepository;
import com.company.equipment_request_service.security.CurrentUser;
import com.company.equipment_request_service.security.CurrentUserProvider;
import com.company.equipment_request_service.web.error.RequestNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service

public class EquipmentRequestService {

    private final EquipmentRequestRepository requestRepo;
    private final UserRepository userRepo;
    private final CurrentUserProvider currentUserProvider;


    public EquipmentRequestService(
            EquipmentRequestRepository requestRepo,
            UserRepository userRepo,
            CurrentUserProvider currentUserProvider
    ) {
        this.requestRepo = requestRepo;
        this.userRepo = userRepo;
        this.currentUserProvider = currentUserProvider;
    }

    @Transactional

    public RequestDetailDto create(CreateRequestDto dto) {
        // 1. Получаем текущего пользователя из провайдера (не из запроса!).
        CurrentUser cu = currentUserProvider.currentUser();

        // 2. Загружаем пользователя из БД (проверка, что он существует).
        User author = userRepo.findById(cu.id())
                .orElseThrow(() -> new IllegalStateException(
                        "Текущий пользователь id=" + cu.id() + " не найден в БД"));

        // 3. Создаём сущность.
        EquipmentRequest r = new EquipmentRequest();
        r.setAuthor(author);
        r.setEquipmentName(dto.equipmentName());
        r.setQuantity(dto.quantity());
        r.setJustification(dto.justification());
        r.setStatus(RequestStatus.DRAFT);

        Instant now = Instant.now();
        r.setCreatedAt(now);
        r.setUpdatedAt(now);

        // 4. Сохраняем.
        EquipmentRequest saved = requestRepo.save(r);

        // 5. Маппим в DTO (внутри транзакции, чтобы lazy-загрузка работала).
        return RequestDetailDto.from(saved);
    }

    @Transactional(readOnly = true)

    public RequestDetailDto getById(Long id) {
        EquipmentRequest r = requestRepo.findById(id)
                .orElseThrow(() -> new RequestNotFoundException(id));

        return RequestDetailDto.from(r);
    }

    @Transactional

    public RequestDetailDto update(Long id, UpdateRequestDto dto) {
        // 1. Загружаем заявку.
        EquipmentRequest r = requestRepo.findById(id)
                .orElseThrow(() -> new RequestNotFoundException(id));

        // 2. Проверяем статус (правило: редактировать можно только DRAFT).
        if (r.getStatus() != RequestStatus.DRAFT) {
            throw new IllegalStateException("CONFLICT_STATUS");
        }

        // 3. Проверяем права (правило: редактировать может только автор).
        CurrentUser cu = currentUserProvider.currentUser();
        if (!r.getAuthor().getId().equals(cu.id())) {
            throw new IllegalStateException("FORBIDDEN_NOT_AUTHOR");
        }

        // 4. Обновляем только клиентские поля (частичный PATCH).
        if (dto.equipmentName() != null && !dto.equipmentName().isBlank()) {
            r.setEquipmentName(dto.equipmentName());
        }
        if (dto.quantity() != null) {
            r.setQuantity(dto.quantity());
        }
        if (dto.justification() != null && !dto.justification().isBlank()) {
            r.setJustification(dto.justification());
        }

        // 5. Обновляем служебные поля.
        r.setUpdatedAt(Instant.now());

        // 6. Сохраняем.
        EquipmentRequest saved = requestRepo.saveAndFlush(r);

        return RequestDetailDto.from(saved);
    }
}