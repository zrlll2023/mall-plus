package com.zrl.rlmall.utils;

// Redis常量类
public class RedisConstants {
    public static final String LOGIN_CODE_KEY = "login:code:";

    public static final Long LOGIN_CODE_TTL = 2L;

    public static final String LOGIN_TOKEN = "login:token:";

    public static final Long LOGIN_USER_TTL = 30L;

    public static final String CACHE_SHOP_KEY = "cache:shop:";

    public static final Long CACHE_SHOP_ID_TTL = 30L;

    public static final Long CACHE_NULL_TTL = 2L;

    public static final String CACHE_SHOP_TYPE_KEY = "cache:shop:type:";

    public static final Long CACHE_SHOP_TYPE_ID_TTL = 30L;

    // 新增：店铺缓存最多额外存活 5 分钟，用于分散过期时间。
    public static final long CACHE_SHOP_TTL_MAX_JITTER_MINUTES = 5L;

    public static final String LOCK_SHOP_ID_KEY = "lock:shop:";

    public static final String CACHE_VOUCHER_ID = "cache:voucher:id:";

    public static final Long CACHE_VOUCHER_ID_TTL = 30L;

    public static final String SECKILL_STOCK_ID = "seckill:stock:";
}
