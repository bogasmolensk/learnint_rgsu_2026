package com.company.equipment_request_service.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")   // тестовая база + настоящий вход; stub-user НЕ включён
class RequestAccessControlTest {

    @Autowired MockMvc mvc;

    private final ObjectMapper mapper = new ObjectMapper();

    @Value("${demo.password.emp1:demo-emp1}")   String emp1Pass;
    @Value("${demo.password.emp2:demo-emp2}")   String emp2Pass;
    @Value("${demo.password.appr1:demo-appr1}") String apprPass;

    private String basic(String login, String pass) {
        return "Basic " + java.util.Base64.getEncoder()
                .encodeToString((login + ":" + pass).getBytes());
    }

    private long extractId(MvcResult r) throws Exception {
        return mapper.readTree(r.getResponse().getContentAsString()).get("id").asLong();
    }

    private MvcResult createAs(String login, String pass, String name) throws Exception {
        return mvc.perform(post("/api/v1/requests")
                        .header("Authorization", basic(login, pass))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"equipmentName\":\"" + name + "\",\"quantity\":1,\"justification\":\"j\"}"))
                .andExpect(status().isCreated())
                .andReturn();
    }

    @Test
    void noAuth_returns401() throws Exception {
        mvc.perform(get("/api/v1/requests/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void employee_cannotSeeOthersRequest_returns404() throws Exception {
        long id = extractId(createAs("emp_two", emp2Pass, "KB"));
        mvc.perform(get("/api/v1/requests/" + id)
                        .header("Authorization", basic("emp_one", emp1Pass)))
                .andExpect(status().isNotFound());
    }

    @Test
    void employee_cannotSeeOthersHistory_returns404() throws Exception {
        long id = extractId(createAs("emp_two", emp2Pass, "KB2"));
        mvc.perform(get("/api/v1/requests/" + id + "/history")
                        .header("Authorization", basic("emp_one", emp1Pass)))
                .andExpect(status().isNotFound());
    }

    @Test
    void approver_decidesOthers_butNotOwn() throws Exception {
        long id = extractId(createAs("emp_one", emp1Pass, "M"));
        mvc.perform(post("/api/v1/requests/" + id + "/submit")
                        .header("Authorization", basic("emp_one", emp1Pass))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":0}"))
                .andExpect(status().isOk());

        // ответственный одобряет ЧУЖУЮ -> 200
        mvc.perform(post("/api/v1/requests/" + id + "/approve")
                        .header("Authorization", basic("appr_one", apprPass))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));

        // ответственный создаёт СВОЮ, отправляет, пытается одобрить -> 403
        long ownId = extractId(createAs("appr_one", apprPass, "Own"));
        mvc.perform(post("/api/v1/requests/" + ownId + "/submit")
                        .header("Authorization", basic("appr_one", apprPass))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":0}"))
                .andExpect(status().isOk());
        mvc.perform(post("/api/v1/requests/" + ownId + "/approve")
                        .header("Authorization", basic("appr_one", apprPass))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":1}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void approver_cannotEditOthers() throws Exception {
        long id = extractId(createAs("emp_one", emp1Pass, "E"));
        mvc.perform(patch("/api/v1/requests/" + id)
                        .header("Authorization", basic("appr_one", apprPass))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":0,\"quantity\":5}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void employee_approveForbidden_byRole() throws Exception {
        long id = extractId(createAs("emp_one", emp1Pass, "D"));
        mvc.perform(post("/api/v1/requests/" + id + "/approve")
                        .header("Authorization", basic("emp_one", emp1Pass))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":0}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void createIgnoresClientAuthorId() throws Exception {
        MvcResult c = mvc.perform(post("/api/v1/requests")
                        .header("Authorization", basic("emp_one", emp1Pass))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"equipmentName\":\"X\",\"quantity\":1,\"justification\":\"j\",\"authorId\":999}"))
                .andExpect(status().isCreated()).andReturn();
        long authorId = mapper.readTree(c.getResponse().getContentAsString()).get("authorId").asLong();
        assertThat(authorId).isNotEqualTo(999L);   // автор = вошедший пользователь, подмена не сработала
    }

    @Test
    void responseHasNoPasswordField() throws Exception {
        MvcResult c = createAs("emp_one", emp1Pass, "Y");
        assertThat(c.getResponse().getContentAsString()).doesNotContain("password");
    }
}