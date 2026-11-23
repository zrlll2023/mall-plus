package com.zrl.rlmall.controller;


import com.zrl.rlmall.dto.Result;
import com.zrl.rlmall.entity.ShopType;
import com.zrl.rlmall.service.ShopTypeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/shop-type")
public class ShopTypeController {
    @Autowired
    private ShopTypeService typeService;

    /**
     * 查询全部店铺类型
     * @return
     */
    @GetMapping("list")
    public Result queryTypeList() {
        List<ShopType> typeList = typeService.listShopTypes();
        return Result.ok(typeList);
    }
}
