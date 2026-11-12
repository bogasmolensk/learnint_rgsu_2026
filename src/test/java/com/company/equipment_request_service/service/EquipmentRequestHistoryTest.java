package com.company.equipment_request_service.service;

import com.company.equipment_request_service.dto.ActionVersionDto;
import com.company.equipment_request_service.dto.CreateRequestDto;
import com.company.equipment_request_service.entity.RequestStatus;
import com.company.equipment_request_service.repository.EquipmentRequestRepository;
import com.company.equipment_request_service.repository.RequestStatusHistoryRepository;
import com.company.equipment_request_service.web.error.InvalidStatusTransitionException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles({"test", "stub-user"})
class EquipmentRequestHistoryTest {

    @Autowired
    EquipmentRequestService service;
    @Autowired
    EquipmentRequestRepository requestRepo;
    @Autowired
    RequestStatusHistoryRepository historyRepo;

    @Test
    void submit_recordsHistoryRow_inDatabase() {
        var created = service.create(new CreateRequestDto("Monitor", 1, "test"));
        var submitted = service.submit(created.id(), new ActionVersionDto(created.version()));

        assertThat(submitted.status()).isEqualTo(RequestStatus.UNDER_REVIEW);

        var history = historyRepo.findByRequestIdOrderByCreatedAtAscIdAsc(created.id());
        assertThat(history).hasSize(2); // null->DRAFT при создании + DRAFT->UNDER_REVIEW при submit
        assertThat(history.get(0).getPreviousStatus()).isNull();
        assertThat(history.get(0).getNewStatus()).isEqualTo(RequestStatus.DRAFT);
        assertThat(history.get(1).getPreviousStatus()).isEqualTo(RequestStatus.DRAFT);
        assertThat(history.get(1).getNewStatus()).isEqualTo(RequestStatus.UNDER_REVIEW);
        assertThat(history.get(1).getChangedBy().getId()).isEqualTo(1L);
    }

    @Test
    void illegalTransition_writesNoHistoryRow() {
        var created = service.create(new CreateRequestDto("Monitor", 1, "test"));
        var submitted = service.submit(created.id(), new ActionVersionDto(created.version()));

        long before = historyRepo.findByRequestIdOrderByCreatedAtAscIdAsc(created.id()).size();

        assertThatThrownBy(() -> service.submit(created.id(), new ActionVersionDto(submitted.version())))
                .isInstanceOf(InvalidStatusTransitionException.class);

        long after = historyRepo.findByRequestIdOrderByCreatedAtAscIdAsc(created.id()).size();
        assertThat(after).isEqualTo(before);
    }
}
