package com.undercurrent;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.undercurrent.mapper")
public class UndercurrentApplication {

    public static void main(String[] args) {
        SpringApplication.run(UndercurrentApplication.class, args);
    }
}
