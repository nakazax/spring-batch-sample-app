package com.example.batch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import lombok.extern.slf4j.Slf4j;

@SpringBootApplication
@Slf4j
public class BatchProcessingApplication {

    public static void main(String[] args) throws Exception {
        log.info("Start main: args={}", String.join(",", args));
        int exitCode = SpringApplication.exit(SpringApplication.run(BatchProcessingApplication.class, args));
        log.info("Exit main: exitCode={}", exitCode);
        System.exit(exitCode);
    }
}
