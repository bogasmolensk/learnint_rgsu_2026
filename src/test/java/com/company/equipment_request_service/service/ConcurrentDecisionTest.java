package com.company.equipment_request_service.service;

import com.company.equipment_request_service.dto.ActionVersionDto;
import com.company.equipment_request_service.dto.CreateRequestDto;
import com.company.equipment_request_service.entity.EquipmentRequest;
import com.company.equipment_request_service.entity.RequestStatus;
import com.company.equipment_request_service.repository.EquipmentRequestRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles({"test", "stub-user"})
class ConcurrentDecisionTest {

    @Autowired EquipmentRequestService service;
    @Autowired EquipmentRequestRepository requestRepo;
    @Autowired PlatformTransactionManager txManager;

    @Test
    void concurrentApproveAndReject_fixesExactlyOneDecision() throws Exception {
        // заявка в UNDER_REVIEW v=1, создано обычными сервисными вызовами
        var created = service.create(new CreateRequestDto("Monitor", 1, "test"));
        service.submit(created.id(), new ActionVersionDto(created.version()));
        Long id = created.id();
        int startVersion = requestRepo.findById(id).orElseThrow().getVersion(); // = 1

        var bothRead = new CountDownLatch(2);
        var results = new ConcurrentHashMap<String, String>();

        TransactionTemplate tt = new TransactionTemplate(txManager);

        Thread a = new Thread(() -> raceTick(tt, id, startVersion, bothRead, results, "A",
                r -> r.setStatus(RequestStatus.APPROVED)));
        Thread b = new Thread(() -> raceTick(tt, id, startVersion, bothRead, results, "B",
                r -> { r.setStatus(RequestStatus.REJECTED); r.setDecisionComment("no"); }));

        a.start(); b.start();
        a.join(); b.join();

        // ровно один победил, ровно один получил конфликт
        assertThat(results.values()).containsExactlyInAnyOrder("OK", "CONFLICT");

        // зафиксировано ровно одно решение, версия выросла ОДИН раз
        EquipmentRequest fin = requestRepo.findById(id).orElseThrow();
        assertThat(fin.getStatus()).isIn(RequestStatus.APPROVED, RequestStatus.REJECTED);
        assertThat(fin.getVersion()).isEqualTo(startVersion + 1);
    }

    // один участник гонки: своя транзакция
    private void raceTick(TransactionTemplate tt, Long id, int startVersion,
                          CountDownLatch bothRead, ConcurrentHashMap<String, String> results,
                          String label, Consumer<EquipmentRequest> mutate) {
        try {
            tt.executeWithoutResult(status -> {
                EquipmentRequest r = requestRepo.findById(id).orElseThrow();
                assertThat(r.getVersion()).isEqualTo(startVersion); // прочитали ту же v=1
                bothRead.countDown();
                awaitQuietly(bothRead);                             // ждём, пока ОБОЕ прочитали
                mutate.accept(r);                                   // A->APPROVED, B->REJECTED
                r.setUpdatedAt(Instant.now());
                requestRepo.saveAndFlush(r);                        // UPDATE where v=1
            });
            results.put(label, "OK");
        } catch (ObjectOptimisticLockingFailureException e) {
            results.put(label, "CONFLICT");
        } catch (Exception e) {
            results.put(label, "ERROR:" + e.getClass().getSimpleName());
        }
    }

    private void awaitQuietly(CountDownLatch l) {
        try {
            assertThat(l.await(5, TimeUnit.SECONDS)).isTrue();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}