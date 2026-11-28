package com.zrl.rlmall.service.impl;

import com.zrl.rlmall.dto.Result;
import com.zrl.rlmall.entity.VoucherOrder;
import com.zrl.rlmall.mapper.VoucherOrderMapper;
import com.zrl.rlmall.service.SeckillVoucherService;
import com.zrl.rlmall.service.VoucherOrderService;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.zrl.rlmall.utils.RedisIdWorker;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.aop.framework.AopContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import com.zrl.rlmall.utils.UserHolder;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.concurrent.*;

@Service
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements VoucherOrderService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private SeckillVoucherService seckillVoucherService;

    @Resource
    private RedisIdWorker redisIdWorker;

    @Resource
    private RedissonClient redissonClient;

    private static final DefaultRedisScript<Long> SECKILL_SCRIPT;
    static {
        SECKILL_SCRIPT = new DefaultRedisScript<>();
        SECKILL_SCRIPT.setLocation(new ClassPathResource("seckill.lua"));
        SECKILL_SCRIPT.setResultType(Long.class);
    }
    // 创建阻塞队列
    private BlockingQueue<VoucherOrder> orderTasks = new ArrayBlockingQueue<>(1024 * 1024);
    // 创建线程池
    private static final ExecutorService SECKILL_ORDER_EXECUTOR = Executors.newSingleThreadExecutor();

    // 在当前类初始化完毕后执行
    @PostConstruct
    private void init(){
        SECKILL_ORDER_EXECUTOR.submit(new VoucherOrderHandler());
    }
    // 创建线程任务
    private class VoucherOrderHandler implements Runnable {

        @Override
        public void run() {
            while (true) {
                try {
                    // 1.获取队列中的订单信息
                    VoucherOrder voucherOrder = orderTasks.take();
                    // 2.创建订单
                    handleVoucherOrder(voucherOrder);
                } catch (Exception e) {
                    log.error("处理订单异常",e);
                }
            }
        }
    }

    private void handleVoucherOrder(VoucherOrder voucherOrder) {
        // 1.获取用户
        Long userId = voucherOrder.getUserId();
        // 2.创建锁对象
        // SimpleRedisLock lock = new SimpleRedisLock("order:" + userId, stringRedisTemplate);
        RLock lock = redissonClient.getLock("lock:order:" + userId);
        // 3.获取锁
        boolean isLock = lock.tryLock();
        // 4.判断是否获取成功锁
        if (!isLock) {
            // 获取锁失败，返回错误或重试
            log.error("不允许重复下单");
            return;
        }
        try {
            proxy.createVoucherOrder(voucherOrder);
        } finally {
            // 释放锁
            lock.unlock();
        }
    }

    private VoucherOrderService proxy;

    @Override
    public Result seckillVoucher(Long voucherId) {
        // 获取用户
        Long userId = UserHolder.getUser().getId();
        // 1.执行lua脚本
        Long result = stringRedisTemplate.execute(
                SECKILL_SCRIPT,
                Collections.emptyList(),
                voucherId.toString(), userId.toString()
        );
        // 2.判断结果是为0
        int r = result.intValue();
        if (r != 0) {
            // 2.1.不为0，代表没有购买资格
            return Result.fail(r == 1 ? "库存不足" : "不能重复下单");
        }
        // 2.2.为0，有购买资格，把下单信息保存到阻塞队列
        VoucherOrder voucherOrder = new VoucherOrder();
        // 订单id
        long orderId = redisIdWorker.nextId("order");
        voucherOrder.setId(orderId);
        // 用户id
        voucherOrder.setUserId(userId);
        // 代金券id
        voucherOrder.setVoucherId(voucherId);
        // 3.获取代理对象
        proxy = (VoucherOrderService) AopContext.currentProxy();
        // 保存阻塞队列
        orderTasks.add(voucherOrder);
        // 3.返回下单id
        return Result.ok(orderId);
    }

    /*
    @Override
    public Result seckillVoucher(Long voucherId) {
        // 先获取当前用户 ID
//        UserDTO user = UserHolder.getUser();
//        if (user == null) {
//            // 正常情况下拦截器会挡住，但为了安全还是判空
//            return Result.fail("请先登录！");
//        }
//        Long userId = user.getId();

        if (voucherId == null) {
            return Result.fail("优惠券 ID 不能为空");
        }

        // 实现声明 voucher，方便使用
        SeckillVoucher voucher = null;

        String key = CACHE_VOUCHER_ID + voucherId;
        // 1.查询优惠券信息
        // 1.1.先在缓存中查询
        String voucherJson = stringRedisTemplate.opsForValue().get(key);
        if (StrUtil.isNotBlank(voucherJson)) {
            // 先把 JSON 转成 Voucher 对象
            voucher = JSONUtil.toBean(voucherJson, SeckillVoucher.class);
        } else if (voucherJson != null) { // 判断是否命中缓存空值
            return Result.fail("优惠券不存在");
        }else {
            // 1.2.查询不到再去数据库中查询
            voucher = seckillVoucherService.getById(voucherId);
            if (voucher == null) {
                stringRedisTemplate.opsForValue().set(key, "", CACHE_NULL_TTL, TimeUnit.MINUTES);
                return Result.fail("优惠券不存在");
            }
            // 1.3.保存数据到缓存中
            stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(voucher), CACHE_VOUCHER_ID_TTL, TimeUnit.MINUTES);
        }
            // 2.判断秒杀是否开始，是就继续，不是就返回
        if (voucher.getBeginTime().isAfter(LocalDateTime.now())) {
            return Result.fail("活动尚未开始");
        }
        // 3.判断秒杀是否结束，是就继续，不是就返回
        if (voucher.getEndTime().isBefore(LocalDateTime.now())) {
            return Result.fail("活动已经结束");
        }
        // 4.判断秒杀券库存是否充足，是就继续，不是就返回
        if(voucher.getStock() < 1){
            return Result.fail("库存不足");
        }
        Long userId = UserHolder.getUser().getId();
        // 创建锁对象
        // SimpleRedisLock lock = new SimpleRedisLock("order:" + userId, stringRedisTemplate);
        RLock lock = redissonClient.getLock("lock:order:" + userId);
        // 获取锁
        boolean isLock = lock.tryLock();
        // 判断是否获取成功锁
        if (!isLock) {
            // 获取锁失败，返回错误或重试
            return Result.fail("不允许重复下单");
        }
        try {
            // 获取代理对象（事务）
            VoucherOrderService proxy = (VoucherOrderService) AopContext.currentProxy();
            return proxy.createVoucherOrder(voucherId);
        }finally {
            // 释放锁
            lock.unlock();
        }
    }
    */

    // 子线程
    @Transactional(rollbackFor = Exception.class)
    public void createVoucherOrder(VoucherOrder voucherOrder) {
        // 5.判断用户是否下过单
        Long userId = voucherOrder.getUserId();
        // 5.1.查询订单
        long count = query().eq("user_id", userId).eq("voucher_id", voucherOrder.getVoucherId()).count();
        // 5.2.判断是否存在
        if (count > 0) {
            log.error("已经购买过了，不能再次购买");
            return;
        }
        // 6.扣减库存
        boolean success = seckillVoucherService.update()
                .setSql("stock = stock - 1")
                .eq("voucher_id", voucherOrder.getVoucherId()) // .eq("stock", voucher.getStock()) // 乐观锁
                .gt("stock", 0) // 优化乐观锁, where id = ? and stock > 0
                .update();
        if (!success) {
            log.error("库存不足");
            return;
        }
        // 7.创建订单
        save(voucherOrder);
//        // 7.1 生成订单 ID，传入一个前缀用于区分业务
//        long orderId = redisIdWorker.nextId("voucherOrder");
//        // 7.2 构造订单实体对象
//        VoucherOrder voucherOrder = new VoucherOrder();
//        voucherOrder.setId(orderId);
//        voucherOrder.setUserId(userId);
//        voucherOrder.setVoucherId(voucherOrder);
//        voucherOrder.setStatus(1);
//        // 7.3 保存到数据库
//        save(voucherOrder);
//        // 8.返回订单id
//        return Result.ok(orderId);
    }
}
