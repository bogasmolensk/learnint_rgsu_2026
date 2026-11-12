package com.company.equipment_request_service;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class HashGenerator {
    @Test
    void printHashes() {
        BCryptPasswordEncoder enc = new BCryptPasswordEncoder();
        System.out.println("EMP_ONE  = " + enc.encode("demo-emp1"));
        System.out.println("EMP_TWO  = " + enc.encode("demo-emp2"));
        System.out.println("APPR_ONE = " + enc.encode("demo-appr1"));
    }
}