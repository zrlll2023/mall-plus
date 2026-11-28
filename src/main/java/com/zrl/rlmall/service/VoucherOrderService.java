package com.zrl.rlmall.service;

import com.zrl.rlmall.dto.Result;
import com.zrl.rlmall.entity.VoucherOrder;
import com.baomidou.mybatisplus.spring.service.IService;

public interface VoucherOrderService extends IService<VoucherOrder> {

    Result seckillVoucher(Long voucherId);

    void createVoucherOrder(VoucherOrder voucherOrder);
}
