package com.platform.common.constant;

/**
 * Redis 键常量定义
 */
public final class RedisKeyConstants {

    private RedisKeyConstants() {
        // 私有构造方法，防止实例化
    }

    /**
     * 幂等防重 Key 前缀
     * 完整格式: comp:apply:idemp:{compId}:{userId}
     */
    public static final String IDEMPOTENT_KEY_PREFIX = "comp:apply:idemp:";

    /**
     * 竞赛名额 Key 前缀
     * 完整格式: comp:capacity:{compId}
     */
    public static final String CAPACITY_KEY_PREFIX = "comp:capacity:";

    /**
     * 报名结果 Key 前缀
     * 完整格式: comp:apply:result:{compId}:{userId}
     */
    public static final String RESULT_KEY_PREFIX = "comp:apply:result:";

    /**
     * 默认过期时间（秒）
     */
    public static final long DEFAULT_EXPIRE_SECONDS = 3600; // 1小时


    /**
     * 定义 Redis 防重 Key 的前缀
      */
    public static final String REDIS_IDEMPOTENT_PREFIX = "noti:audit:reg:";
}