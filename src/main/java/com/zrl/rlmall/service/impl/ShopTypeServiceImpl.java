package com.zrl.rlmall.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.zrl.rlmall.entity.ShopType;
import com.zrl.rlmall.mapper.ShopTypeMapper;
import com.zrl.rlmall.service.ShopTypeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.zrl.rlmall.utils.RedisConstants.*;

@Service
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements ShopTypeService {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;


    @Override
    public List<ShopType> listShopTypes() {
        // 所有店铺类型共用一个缓存键
        String key = CACHE_SHOP_TYPE_KEY + "list";
        // 缓存命中时，把 JSON 数组转回 List<ShopType>。
        String shopTypeJson = stringRedisTemplate.opsForValue().get(key);
        // 判断是否存在
        if(StrUtil.isNotBlank(shopTypeJson)){
            return JSONUtil.toList(shopTypeJson, ShopType.class);
        }
        // 未命中缓存空值
        List<ShopType> shopTypes = query().orderByAsc("sort").list();
        // 把整个列表转成 JSON，使用项目现有的 30 分钟 TTL。
        stringRedisTemplate.opsForValue().set(key,JSONUtil.toJsonStr(shopTypes),CACHE_SHOP_TYPE_ID_TTL, TimeUnit.MINUTES);
        // 结束
        return shopTypes;
    }
}
