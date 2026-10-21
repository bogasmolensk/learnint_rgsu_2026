package com.company.equipment_request_service.web;

import com.company.equipment_request_service.dto.*;
import com.company.equipment_request_service.service.EquipmentRequestService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController

@RequestMapping("/api/v1/requests")

public class EquipmentRequestController {

    private final EquipmentRequestService service;

    public EquipmentRequestController(EquipmentRequestService service) {
        this.service = service;
    }

    @PostMapping

    public ResponseEntity<RequestDetailDto> create(@Valid @RequestBody CreateRequestDto dto) {

        RequestDetailDto created = service.create(dto);

        URI location = URI.create("/api/v1/requests/" + created.id());

        return ResponseEntity
                .status(HttpStatus.CREATED)  // HTTP 201
                .location(location)
                .body(created);
    }

    @GetMapping("/{id}")

    public ResponseEntity<RequestDetailDto> getById(@PathVariable Long id) {

        return ResponseEntity.ok(service.getById(id));
    }

    @PatchMapping("/{id}")

    public ResponseEntity<RequestDetailDto> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateRequestDto dto
    ) {
        return ResponseEntity.ok(service.update(id, dto));
    }
}