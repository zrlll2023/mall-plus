package com.zrl.rlmall.service.impl;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.zrl.rlmall.dto.Result;
import com.zrl.rlmall.entity.Shop;
import com.zrl.rlmall.mapper.ShopMapper;
import com.zrl.rlmall.service.ShopService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import static com.zrl.rlmall.utils.RedisConstants.*;

@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements ShopService {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 根据id查询商铺缓存
     * @param id
     * @return
     */
    @Override
    public Result queryById(Long id) {
        // 缓存穿透
        // Shop shop = queryWithPassThrough(id);

        // 互斥锁解决缓存击穿
        Shop shop = queryWithMutex(id);
        if (shop == null) {
            return Result.fail("店铺不存在");
        }
        // 结束
        return Result.ok(shop);
    }

    public Shop queryWithMutex(Long id){
        String key = CACHE_SHOP_KEY + id;
        // 替换为块注释
        String shopJson = stringRedisTemplate.opsForValue().get(key);
        // 判断是否存在
        if(StrUtil.isNotBlank(shopJson)){
            // 存在
            return JSONUtil.toBean(shopJson, Shop.class);
        } else if (shopJson != null) { // 判断是否命中缓存空值
            return null;
        }

        // 实现缓存重建
        // 获取互斥锁
        String lockKey = LOCK_SHOP_ID_KEY + id;
        Shop shop = null;
        try {
            boolean isLock = trylock(lockKey);
            // 判断是否成功获取
            if (!isLock) {
                // 如果失败，则休眠并重试
                Thread.sleep(50);
                return queryWithMutex(id);
            }
            // 如果成功，根据id查询数据库
            shop = getById(id);
            // 如果数据库中没有该商铺，返回404
            if (shop == null) {
                stringRedisTemplate.opsForValue().set(key, " ", CACHE_NULL_TTL, TimeUnit.MINUTES);
                return null;
            }
            // 如果查到了商铺，将商铺字段写入 Redis String，并给缓存 key 设置过期时间
            // stringRedisTemplate.opsForValue().set(key,JSONUtil.toJsonStr(shop),CACHE_SHOP_ID_TTL, TimeUnit.MINUTES);
            // 每次回填缓存时取一次偏移量。nextLong 的上界不包含在内，所以传 6 得到 0～5。
            long jitterMinutes = ThreadLocalRandom.current()
                    .nextLong(CACHE_SHOP_TTL_MAX_JITTER_MINUTES + 1);
            long ttlMinutes = CACHE_SHOP_ID_TTL + jitterMinutes;
            stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(shop), ttlMinutes, TimeUnit.MINUTES);
        }catch (InterruptedException e){
            throw new RuntimeException(e);
        }finally {
            // 释放互斥锁
            unlock(lockKey);
        }
        // 结束
        return shop;
    }

    public Shop queryWithPassThrough(Long id){
        String key = CACHE_SHOP_KEY + id;
        // 替换为块注释
        String shopJson = stringRedisTemplate.opsForValue().get(key);
        // 判断是否存在
        if(StrUtil.isNotBlank(shopJson)){
            // 存在
            return JSONUtil.toBean(shopJson, Shop.class);
        } else if (shopJson != null) { // 判断是否命中缓存空值
            return null;
        }
        // 不存在，根据id查询数据库
        Shop shop = getById(id);
        // 如果数据库中没有该商铺，返回404
        if(shop == null){
            stringRedisTemplate.opsForValue().set(key," ",CACHE_NULL_TTL, TimeUnit.MINUTES);
            return null;
        }
        // 如果查到了商铺，将商铺字段写入 Redis String，并给缓存 key 设置过期时间
        // stringRedisTemplate.opsForValue().set(key,JSONUtil.toJsonStr(shop),CACHE_SHOP_ID_TTL, TimeUnit.MINUTES);
        // 每次回填缓存时取一次偏移量。nextLong 的上界不包含在内，所以传 6 得到 0～5。
        long jitterMinutes = ThreadLocalRandom.current()
                .nextLong(CACHE_SHOP_TTL_MAX_JITTER_MINUTES + 1);
        long ttlMinutes = CACHE_SHOP_ID_TTL + jitterMinutes;
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(shop), ttlMinutes, TimeUnit.MINUTES);
        // 结束
        return shop;
    }

    private boolean trylock(String key){
        Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", 10, TimeUnit.SECONDS);
        return BooleanUtil.isTrue(flag);
    }

    private void unlock(String key){
        stringRedisTemplate.delete(key);
    }

    /**
     * 更新店铺id
     * @param shop
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public Result update(Shop shop) {
        if (shop.getId() == null) {
            return Result.fail("店铺id不能为空");
        }
        // 更新数据库
        updateById(shop);
        // 删除缓存
        stringRedisTemplate.delete(CACHE_SHOP_KEY + shop.getId());
        return Result.ok();
    }
}
