package com.company.equipment_request_service.service;

import com.company.equipment_request_service.dto.ActionVersionDto;
import com.company.equipment_request_service.dto.CreateRequestDto;
import com.company.equipment_request_service.entity.EquipmentRequest;
import com.company.equipment_request_service.entity.RequestStatus;
import com.company.equipment_request_service.repository.EquipmentRequestRepository;
import com.company.equipment_request_service.repository.RequestStatusHistoryRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;

@SpringBootTest
@ActiveProfiles({"test", "stub-user"})
class EquipmentRequestRollbackTest {

    @Autowired EquipmentRequestService service;
    @Autowired EquipmentRequestRepository requestRepo;
    @MockBean RequestStatusHistoryRepository historyRepo;

    @Test
    void historySaveFailure_rollsBackRequestChange() {
        // 1-й save (при create) вернёт сущность как настоящий, 2-й save (при submit) бросит
        doAnswer(inv -> inv.getArgument(0))
                .doThrow(new RuntimeException("boom"))
                .when(historyRepo).save(any());

        var created = service.create(new CreateRequestDto("Monitor", 1, "test"));

        assertThatThrownBy(() -> service.submit(created.id(), new ActionVersionDto(created.version())))
                .isInstanceOf(RuntimeException.class);

        EquipmentRequest reloaded = requestRepo.findById(created.id()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(RequestStatus.DRAFT);
        assertThat(reloaded.getVersion()).isEqualTo(created.version());
    }
}