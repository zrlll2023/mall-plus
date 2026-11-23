package com.zrl.rlmall.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zrl.rlmall.entity.Voucher;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@org.apache.ibatis.annotations.Mapper
public interface VoucherMapper extends BaseMapper<Voucher> {

    List<Voucher> queryVoucherOfShop(@Param("shopId") Long shopId);
}
