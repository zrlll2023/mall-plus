package com.zrl.rlmall.service;

import com.zrl.rlmall.dto.Result;
import com.zrl.rlmall.entity.Voucher;
import com.baomidou.mybatisplus.spring.service.IService;

public interface VoucherService extends IService<Voucher> {

    Result queryVoucherOfShop(Long shopId);

    void addSeckillVoucher(Voucher voucher);
}
