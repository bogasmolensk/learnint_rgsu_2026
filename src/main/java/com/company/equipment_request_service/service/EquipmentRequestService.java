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
import com.company.equipment_request_service.specification.EquipmentRequestSpecifications;
import com.company.equipment_request_service.web.error.BadRequestException;
import com.company.equipment_request_service.web.error.FieldErrorDto;
import com.company.equipment_request_service.web.error.ForbiddenException;
import com.company.equipment_request_service.web.error.InvalidStatusTransitionException;
import com.company.equipment_request_service.web.error.VersionConflictException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

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
        // 1. Получаем текущего пользователя из провайдера (не из запроса!)
        CurrentUser cu = currentUserProvider.currentUser();

        // 2. Загружаем пользователя из БД (проверка, что он существует)
        User author = userRepo.findById(cu.id())
                .orElseThrow(() -> new IllegalStateException(
                        "Текущий пользователь id=" + cu.id() + " не найден в БД"));

        // 3. Создаём сущность
        EquipmentRequest r = new EquipmentRequest();
        r.setAuthor(author);
        r.setEquipmentName(dto.equipmentName());
        r.setQuantity(dto.quantity());
        r.setJustification(dto.justification());
        r.setStatus(RequestStatus.DRAFT);

        Instant now = Instant.now();
        r.setCreatedAt(now);
        r.setUpdatedAt(now);

        // 4. Сохраняем
        EquipmentRequest saved = requestRepo.save(r);

        // 5. Маппим в DTO
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
        EquipmentRequest r = requestRepo.findById(id)
                .orElseThrow(() -> new RequestNotFoundException(id));

        if (r.getStatus() != RequestStatus.DRAFT) {
            throw new InvalidStatusTransitionException(
                    "Редактирование запрещено: заявка не в статусе DRAFT");   // 409 по advice
        }
        CurrentUser cu = currentUserProvider.currentUser();
        if (!r.getAuthor().getId().equals(cu.id())) {
            throw new ForbiddenException("Изменять заявку может только её автор");  // 403
        }

        if (dto.equipmentName() != null && !dto.equipmentName().isBlank()) r.setEquipmentName(dto.equipmentName());
        if (dto.quantity() != null) r.setQuantity(dto.quantity());
        if (dto.justification() != null && !dto.justification().isBlank()) r.setJustification(dto.justification());
        r.setUpdatedAt(Instant.now());

        try {
            EquipmentRequest saved = requestRepo.saveAndFlush(r);  // flush сразу, поймать конфликт здесь
            return RequestDetailDto.from(saved);
        } catch (ObjectOptimisticLockingFailureException e) {
            throw new VersionConflictException(
                    "Заявка была изменена другим пользователем. Обновите данные и повторите.");  // 409
        }
    }

    @Transactional(readOnly = true)
    public PageResponseDto<RequestListItemDto> list(RequestSearchCriteria c) {

        Specification<EquipmentRequest> spec = Specification.where(null);

        // 1. scope / фильтр по автору по РОЛИ из провайдера (не из запроса!)
        CurrentUser cu = currentUserProvider.currentUser();
        if (cu.role() == UserRole.EMPLOYEE) {
            // Сотрудник видит ТОЛЬКО свои
            spec = spec.and(EquipmentRequestSpecifications.byAuthor(cu.id()));
        } else { // APPROVER
            String scope = (c.scope() == null || c.scope().isBlank()) ? "all" : c.scope().trim().toLowerCase();
            switch (scope) {
                case "all"     -> { /* без фильтра по автору */ }
                case "mine"    -> spec = spec.and(EquipmentRequestSpecifications.byAuthor(cu.id()));
                case "pending" -> spec = spec.and(EquipmentRequestSpecifications.pendingFor(cu.id()));
                default -> throw new BadRequestException("INVALID_SCOPE",
                        "Неизвестное значение scope: '" + c.scope() + "'",
                        List.of(new FieldErrorDto("scope", "допустимо: all, mine, pending")));
            }
        }

        // 2. фильтр по статусу: парсим строку в enum
        if (c.status() != null && !c.status().isBlank()) {
            RequestStatus status;
            try {
                status = RequestStatus.valueOf(c.status().trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("INVALID_STATUS",
                        "Неизвестный статус: '" + c.status() + "'",
                        List.of(new FieldErrorDto("status",
                                "допустимо: DRAFT, UNDER_REVIEW, APPROVED, REJECTED, CANCELLED")));
            }
            spec = spec.and(EquipmentRequestSpecifications.byStatus(status));
        }

        // 3. диапазон дат: парсим from/to
        Instant from = parseInstantOrThrow(c.from(), "from");
        Instant to   = parseInstantOrThrow(c.to(),   "to");
        if (from != null && to != null && from.isAfter(to)) {
            throw new BadRequestException("INVALID_DATE_RANGE",
                    "Начало диапазона не позже окончания: from=" + c.from() + ", to=" + c.to(),
                    List.of(new FieldErrorDto("from", "from не может быть позже to")));
        }
        if (from != null && to != null) {
            spec = spec.and(EquipmentRequestSpecifications.byCreatedBetween(from, to));
        } else if (from != null) {
            spec = spec.and(EquipmentRequestSpecifications.byCreatedBetween(from, Instant.MAX));
        } else if (to != null) {
            spec = spec.and(EquipmentRequestSpecifications.byCreatedBetween(Instant.MIN, to));
        }

        // 4. пагинация: page>=0, 1<=size<=100, дефолты 0/20
        int page = (c.page() == null) ? 0 : c.page();
        int size = (c.size() == null) ? 20 : c.size();
        List<FieldErrorDto> pageErrors = new ArrayList<>();
        if (page < 0) pageErrors.add(new FieldErrorDto("page", "номер страницы не может быть отрицательным"));
        if (size < 1 || size > 100) pageErrors.add(new FieldErrorDto("size", "размер страницы от 1 до 100"));
        if (!pageErrors.isEmpty()) {
            throw new BadRequestException("VALIDATION_ERROR",
                    pageErrors.stream().map(f -> f.field() + ": " + f.message())
                            .collect(java.util.stream.Collectors.joining("; ")),
                    pageErrors);
        }

        // 5. сортировка
        Sort sort = buildSort(c.sort());
        Pageable pageable = PageRequest.of(page, size, sort);

        // 6. выполнение ОДНИМ запросом в БД: фильтр+пагинация+сортировка вместе
        Page<EquipmentRequest> result = requestRepo.findAll(spec, pageable);

        Page<RequestListItemDto> mapped = result.map(RequestListItemDto::from);
        return PageResponseDto.of(mapped);
    }

    private Instant parseInstantOrThrow(String value, String field) {
        if (value == null || value.isBlank()) return null;       // фильтр не применяется
        try {
            return Instant.parse(value.trim());                  // ожидает 2026-10-12T15:30:00Z
        } catch (DateTimeParseException e) {
            throw new BadRequestException("INVALID_DATE_FORMAT",
                    "Некорректный формат даты в параметре " + field + ": '" + value + "'",
                    List.of(new FieldErrorDto(field, "ожидался ISO 8601 UTC, напр. 2026-10-12T15:30:00Z")));
        }
    }

    private Sort buildSort(String sortParam) {
        Set<String> allowed = Set.of("createdAt", "id");
        Sort base;
        if (sortParam == null || sortParam.isBlank()) {
            base = Sort.by(Sort.Direction.DESC, "createdAt");
        } else {
            String[] parts = sortParam.split(",");
            String field = parts[0].trim();
            if (!allowed.contains(field)) {
                throw new BadRequestException("INVALID_SORT",
                        "Неизвестное поле сортировки: '" + field + "'",
                        List.of(new FieldErrorDto("sort", "допустимо: createdAt, id")));
            }
            Sort.Direction dir = (parts.length > 1 && parts[1].trim().equalsIgnoreCase("asc"))
                    ? Sort.Direction.ASC : Sort.Direction.DESC;
            base = Sort.by(dir, field);
        }
        boolean hasId = base.stream().anyMatch(o -> "id".equals(o.getProperty()));
        return hasId ? base : base.and(Sort.by(Sort.Direction.ASC, "id"));
    }
}