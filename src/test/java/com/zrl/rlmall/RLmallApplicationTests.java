package com.zrl.rlmall;

import com.zrl.rlmall.utils.RedisIdWorker;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SpringBootTest
class RLmallApplicationTests {

    @Test
    void contextLoads() {
    }

    @Resource
    private RedisIdWorker redisIdWorker;

        private ExecutorService executorService = Executors.newFixedThreadPool(500);

        @Test
    void testIdWorker() {

        }
}
