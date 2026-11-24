package com.zrl.rlmall;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@EnableAspectJAutoProxy(exposeProxy = true)
@MapperScan("com.zrl.rlmall.mapper")
@SpringBootApplication
public class RLmallApplication {

    public static void main(String[] args) {
        SpringApplication.run(RLmallApplication.class, args);
    }

}
