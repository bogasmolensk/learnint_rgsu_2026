package com.company.equipment_request_service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class EquipmentRequestApiTest extends BaseIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired JdbcTemplate jdbc;   // прямой доступ к БД для проверки состояния

    private final String EMP1 = basic("emp_one", "demo-emp1");
    private final String EMP2 = basic("emp_two", "demo-emp2");
    private final String APPR = basic("appr_one", "demo-appr1");

    private static String basic(String login, String pass) {
        return "Basic " + Base64.getEncoder().encodeToString((login + ":" + pass).getBytes());
    }

    private long create(String auth, String name, int qty, String just) throws Exception {
        MvcResult r = mvc.perform(post("/api/v1/requests")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"equipmentName\":\"" + name + "\",\"quantity\":" + qty +
                                ",\"justification\":\"" + just + "\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        return mapper.readTree(r.getResponse().getContentAsString()).get("id").asLong();
    }

    private MvcResult action(String url, String auth, String body) throws Exception {
        return mvc.perform(post(url)
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andReturn();
    }

    // создание, получение, редактирование, отправка, решение, отмена, история

    @Test
    void create_edit_submit_approve_and_history() throws Exception {
        long id = create(EMP1, "Монитор", 1, "для работы");

        mvc.perform(get("/api/v1/requests/" + id).header("Authorization", EMP1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.version").value(0));

        mvc.perform(patch("/api/v1/requests/" + id).header("Authorization", EMP1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":0,\"quantity\":2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantity").value(2))
                .andExpect(jsonPath("$.version").value(1));

        mvc.perform(post("/api/v1/requests/" + id + "/submit").header("Authorization", EMP1)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"version\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UNDER_REVIEW"));

        mvc.perform(post("/api/v1/requests/" + id + "/approve").header("Authorization", APPR)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"version\":2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));

        // история: создание + отправка + одобрение, в хронологическом порядке
        mvc.perform(get("/api/v1/requests/" + id + "/history").header("Authorization", EMP1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].newStatus").value("DRAFT"))
                .andExpect(jsonPath("$[1].newStatus").value("UNDER_REVIEW"))
                .andExpect(jsonPath("$[2].newStatus").value("APPROVED"));
    }

    @Test
    void reject_saves_comment_and_cancel_works() throws Exception {
        long id = create(EMP1, "Ноутбук", 1, "старый сломан");
        action("/api/v1/requests/" + id + "/submit", EMP1, "{\"version\":0}");

        mvc.perform(post("/api/v1/requests/" + id + "/reject").header("Authorization", APPR)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":1,\"comment\":\"нет бюджета\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"))
                .andExpect(jsonPath("$.decisionComment").value("нет бюджета"));

        long id2 = create(EMP2, "Клавиатура", 1, "нужна");
        action("/api/v1/requests/" + id2 + "/submit", EMP2, "{\"version\":0}");
        mvc.perform(post("/api/v1/requests/" + id2 + "/cancel").header("Authorization", EMP2)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"version\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    // фильтры отдельно и вместе, сортировка, страницы, пустой результат

    @Test
    void list_filters_sorting_pagination_empty() throws Exception {
        long a = create(EMP1, "AAA", 1, "x");
        create(EMP1, "BBB", 1, "x");
        create(EMP2, "CCC", 1, "x");
        action("/api/v1/requests/" + a + "/submit", EMP1, "{\"version\":0}");

        // сотрудник видит только свои
        mvc.perform(get("/api/v1/requests").header("Authorization", EMP1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));

        // фильтр по статусу
        mvc.perform(get("/api/v1/requests?status=UNDER_REVIEW").header("Authorization", EMP1))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].equipmentName").value("AAA"));

        // фильтры вместе: статус + диапазон дат
        mvc.perform(get("/api/v1/requests?status=DRAFT&from=2020-01-01T00:00:00Z&to=2030-01-01T00:00:00Z")
                        .header("Authorization", EMP1))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].equipmentName").value("BBB"));

        // страницы: размер 1, всего 2 страницы у сотрудника
        mvc.perform(get("/api/v1/requests?page=0&size=1&sort=id,asc").header("Authorization", EMP1))
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.currentPage").value(0))
                .andExpect(jsonPath("$.content[0].equipmentName").value("AAA"));
        mvc.perform(get("/api/v1/requests?page=1&size=1&sort=id,asc").header("Authorization", EMP1))
                .andExpect(jsonPath("$.currentPage").value(1))
                .andExpect(jsonPath("$.content[0].equipmentName").value("BBB"));

        // сортировка по убыванию
        mvc.perform(get("/api/v1/requests?sort=id,desc").header("Authorization", EMP1))
                .andExpect(jsonPath("$.content[0].equipmentName").value("BBB"));

        // пустой результат — это 200 с пустым массивом, а не ошибка
        mvc.perform(get("/api/v1/requests?status=APPROVED").header("Authorization", EMP1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.content").isEmpty());

        // у ответственного scope=pending: чужие на рассмотрении
        mvc.perform(get("/api/v1/requests?scope=pending").header("Authorization", APPR))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].equipmentName").value("AAA"));
    }

    // отрицательные сценарии

    @Test
    void validation_and_not_found_errors() throws Exception {
        // пустое название
        mvc.perform(post("/api/v1/requests").header("Authorization", EMP1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"equipmentName\":\"   \",\"quantity\":1,\"justification\":\"x\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));

        // слишком длинное название (больше 200)
        mvc.perform(post("/api/v1/requests").header("Authorization", EMP1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"equipmentName\":\"" + "X".repeat(201) + "\",\"quantity\":1,\"justification\":\"x\"}"))
                .andExpect(status().isBadRequest());

        // неположительное количество
        mvc.perform(post("/api/v1/requests").header("Authorization", EMP1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"equipmentName\":\"M\",\"quantity\":0,\"justification\":\"x\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/requests").header("Authorization", EMP1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"equipmentName\":\"M\",\"quantity\":-3,\"justification\":\"x\"}"))
                .andExpect(status().isBadRequest());

        // неизвестная заявка
        mvc.perform(get("/api/v1/requests/9999").header("Authorization", EMP1))
                .andExpect(status().isNotFound());
        mvc.perform(post("/api/v1/requests/9999/submit").header("Authorization", EMP1)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"version\":0}"))
                .andExpect(status().isNotFound());

        // неверный статус и кривая дата в параметрах списка
        mvc.perform(get("/api/v1/requests?status=FOO").header("Authorization", EMP1))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/requests?from=не-дата").header("Authorization", EMP1))
                .andExpect(status().isBadRequest());

        // отклонение без комментария
        long id = create(EMP1, "M", 1, "j");
        action("/api/v1/requests/" + id + "/submit", EMP1, "{\"version\":0}");
        mvc.perform(post("/api/v1/requests/" + id + "/reject").header("Authorization", APPR)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"version\":1}"))
                .andExpect(status().isBadRequest());
    }

    // после неуспешного действия заявка и история не меняются

    @Test
    void failed_actions_leave_request_and_history_untouched() throws Exception {
        long id = create(EMP1, "DB", 1, "check");
        action("/api/v1/requests/" + id + "/submit", EMP1, "{\"version\":0}");

        Object statusBefore = jdbc.queryForObject("select status from equipment_requests where id=?", Object.class, id);
        Object versionBefore = jdbc.queryForObject("select version from equipment_requests where id=?", Object.class, id);
        Object updatedBefore = jdbc.queryForObject("select updated_at from equipment_requests where id=?", Object.class, id);
        Integer historyBefore = jdbc.queryForObject("select count(*) from request_status_history where request_id=?", Integer.class, id);

        // сотрудник не может одобрить -> 403
        mvc.perform(post("/api/v1/requests/" + id + "/approve").header("Authorization", EMP1)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"version\":1}"))
                .andExpect(status().isForbidden());
        // повторная отправка -> 409 запрещённый переход
        mvc.perform(post("/api/v1/requests/" + id + "/submit").header("Authorization", EMP1)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"version\":1}"))
                .andExpect(status().isConflict());
        // устаревшая версия -> 409 конфликт версий
        mvc.perform(patch("/api/v1/requests/" + id).header("Authorization", EMP1)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"version\":0,\"quantity\":9}"))
                .andExpect(status().isConflict());

        assertThat(jdbc.queryForObject("select status from equipment_requests where id=?", Object.class, id))
                .isEqualTo(statusBefore);
        assertThat(jdbc.queryForObject("select version from equipment_requests where id=?", Object.class, id))
                .isEqualTo(versionBefore);
        assertThat(jdbc.queryForObject("select updated_at from equipment_requests where id=?", Object.class, id))
                .isEqualTo(updatedBefore);
        assertThat(jdbc.queryForObject("select count(*) from request_status_history where request_id=?", Integer.class, id))
                .isEqualTo(historyBefore);
    }

    @Test
    void history_recordsActor_notAuthor() throws Exception {
        long id = create(EMP1, "ActorCheck", 1, "x");
        action("/api/v1/requests/" + id + "/submit", EMP1, "{\"version\":0}");
        action("/api/v1/requests/" + id + "/approve", APPR, "{\"version\":1}");

        Long authorId = jdbc.queryForObject(
                "select author_id from equipment_requests where id=?", Long.class, id);
        Long approverId = jdbc.queryForObject(
                "select id from users where login='appr_one'", Long.class);

        Long lastActorId = jdbc.queryForObject(
                "select changed_by from request_status_history where request_id=? " +
                        "order by created_at desc, id desc limit 1", Long.class, id);

        assertThat(lastActorId).isEqualTo(approverId);   // записан тот, кто решил
        assertThat(lastActorId).isNotEqualTo(authorId);  // и это не автор заявки
    }

    // конфликт версий через API

    @Test
    void stale_version_gives_409() throws Exception {
        long id = create(EMP1, "V", 1, "x");
        mvc.perform(patch("/api/v1/requests/" + id).header("Authorization", EMP1)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"version\":0,\"quantity\":3}"))
                .andExpect(status().isOk());
        mvc.perform(patch("/api/v1/requests/" + id).header("Authorization", EMP1)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"version\":0,\"quantity\":4}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("VERSION_CONFLICT"));
    }
}