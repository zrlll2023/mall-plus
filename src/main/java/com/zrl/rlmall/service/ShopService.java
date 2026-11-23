package com.zrl.rlmall.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.zrl.rlmall.dto.Result;
import com.zrl.rlmall.entity.Shop;

public interface ShopService extends IService<Shop> {
    Result queryById(Long id);

    Result update(Shop shop);
}
