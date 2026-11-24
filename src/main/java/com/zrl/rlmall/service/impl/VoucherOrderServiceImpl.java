package com.zrl.rlmall.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.zrl.rlmall.dto.Result;
import com.zrl.rlmall.dto.UserDTO;
import com.zrl.rlmall.entity.Voucher;
import com.zrl.rlmall.entity.VoucherOrder;
import com.zrl.rlmall.mapper.VoucherMapper;
import com.zrl.rlmall.mapper.VoucherOrderMapper;
import com.zrl.rlmall.service.SeckillVoucherService;
import com.zrl.rlmall.service.VoucherOrderService;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.zrl.rlmall.utils.RedisIdWorker;
import jakarta.annotation.Resource;
import org.springframework.aop.framework.AopContext;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import com.zrl.rlmall.utils.UserHolder;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.concurrent.TimeUnit;

import static com.zrl.rlmall.utils.RedisConstants.*;

@Service
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements VoucherOrderService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private VoucherMapper voucherMapper;

    @Resource
    private SeckillVoucherService seckillVoucherService;

    @Resource
    private VoucherOrderMapper voucherOrderMapper;

    @Resource
    private RedisIdWorker redisIdWorker;

    @Override
    public Result seckillVoucher(Long voucherId) {
        // 先获取当前用户 ID
//        UserDTO user = UserHolder.getUser();
//        if (user == null) {
//            // 正常情况下拦截器会挡住，但为了安全还是判空
//            return Result.fail("请先登录！");
//        }
//        Long userId = user.getId();

        // 实现声明 voucher，方便使用
        Voucher voucher = null;

        String key = CACHE_VOUCHER_ID + voucherId;
        // 1.查询优惠券信息
        // 1.1.先在缓存中查询
        String voucherJson = stringRedisTemplate.opsForValue().get(key);
        if (StrUtil.isNotBlank(voucherJson)) {
            // 先把 JSON 转成 Voucher 对象
            voucher = JSONUtil.toBean(voucherJson, Voucher.class);
        } else if (voucherJson != null) { // 判断是否命中缓存空值
            return Result.fail("优惠券不存在");
        } else if (voucherId == null) {
            // 1.2.查询不到再去数据库中查询
            voucher = voucherMapper.selectById(voucherId);
            if (voucher == null) {
                stringRedisTemplate.opsForValue().set(key," ",CACHE_NULL_TTL, TimeUnit.MINUTES);
            }
            // 1.3.保存数据到缓存中
            stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(voucher),CACHE_VOUCHER_ID_TTL, TimeUnit.MINUTES);
        }
        // 2.判断秒杀是否开始，是就继续，不是就返回
        if (voucher.getBeginTime().isAfter(LocalDateTime.now())) {
            return Result.fail("活动尚未开始");
        }
        // 3.判断秒杀是否结束，是就继续，不是就返回
        if (voucher.getEndTime().isBefore(LocalDateTime.now())) {
            return Result.fail("活动尚未结束");
        }
        // 4.判断秒杀券库存是否充足，是就继续，不是就返回
        String stockKey = "seckill:stock:" + voucherId;
        String stockStr = stringRedisTemplate.opsForValue().get(stockKey);
        if(stockStr == null || Integer.parseInt(stockStr) <= 0){
            return Result.fail("库存结束");
        }
        Long userId = UserHolder.getUser().getId();
        synchronized (userId.toString().intern()) {
            // 获取代理对象（事务）
            VoucherOrderService proxy = (VoucherOrderService) AopContext.currentProxy();
            return proxy.createVoucherOrder(voucherId);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Result createVoucherOrder(Long voucherId) {
        // 5.判断用户是否下过单
        Long userId = UserHolder.getUser().getId();
        // 5.1.查询订单
        long count = query().eq("user_id", userId).eq("voucher_id", voucherId).count();
        // 5.2.判断是否存在
        if (count > 0) {
            return Result.fail("已经购买过了，不能再次购买");
        }
        // 6.扣减库存
        boolean success = seckillVoucherService.update()
                .setSql("stock = stock - 1")
                .eq("voucher_id", voucherId) // .eq("stock", voucher.getStock()) // 乐观锁
                .gt("stock", 0) // 优化乐观锁, where id = ? and stock > 0
                .update();
        if (!success) {
            return Result.fail("库存不足");
        }
        // 7.创建订单
        // 7.1 生成订单 ID，传入一个前缀用于区分业务
        long orderId = redisIdWorker.nextId("voucherOrder");
        // 7.2 构造订单实体对象
        VoucherOrder voucherOrder = new VoucherOrder();
        voucherOrder.setId(orderId);
        voucherOrder.setUserId(userId);
        voucherOrder.setVoucherId(voucherId);
        voucherOrder.setStatus(1);
        // 7.3 保存到数据库
        save(voucherOrder);
        // 8.返回订单id
        return Result.ok(orderId);
    }
}
