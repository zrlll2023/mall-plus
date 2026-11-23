package com.zrl.rlmall.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.zrl.rlmall.entity.ShopType;

import java.util.List;

public interface ShopTypeService extends IService<ShopType> {
    List<ShopType> listShopTypes();
}
