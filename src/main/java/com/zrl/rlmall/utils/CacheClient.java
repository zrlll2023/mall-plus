package com.zrl.rlmall.utils;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.zrl.rlmall.entity.Shop;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static com.zrl.rlmall.utils.RedisConstants.*;
import static com.zrl.rlmall.utils.RedisConstants.CACHE_SHOP_ID_TTL;

@Slf4j
@Component
public class CacheClient {

    private final StringRedisTemplate stringRedisTemplate;


    public CacheClient(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    //
    public void set(String key, Object value, long time, TimeUnit unit) {
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(value), time, unit);
    }

    // 逻辑
    public void setWithLogicalExpire(String key, String value, long time, TimeUnit unit) {
        // 设置逻辑过期
        RedisData redisData = new RedisData();
        redisData.setData(value);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(unit.toSeconds(time)));
        // 写入Redis
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(redisData));
    }

    public <R, ID> R queryWithPassThrough(String keyPrefix, ID id, Class<R> type, Function<ID,R> dbFallback, long time, TimeUnit unit) {
        String key = keyPrefix + id;
        // 替换为块注释
        String json = stringRedisTemplate.opsForValue().get(key);
        // 判断是否存在
        if(StrUtil.isNotBlank(json)){
            // 存在
            return JSONUtil.toBean(json, type);
        } else if (json != null) { // 判断是否命中缓存空值
            return null;
        }
        // 不存在，根据id查询数据库
        R r = dbFallback.apply(id);
        // 如果数据库中没有该商铺，返回404
        if(r == null){
            stringRedisTemplate.opsForValue().set(key," ",CACHE_NULL_TTL, TimeUnit.MINUTES);
            return null;
        }
        this.set(key, r, time, unit);
        // 结束
        return r;
    }
}
