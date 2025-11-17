package org.opengoofy.index12306.framework.starter.cache;

import com.alibaba.fastjson2.JSON;
import com.google.common.collect.Lists;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.opengoofy.index12306.framework.starter.bases.Singleton;
import org.opengoofy.index12306.framework.starter.cache.config.RedisDistributedProperties;
import org.opengoofy.index12306.framework.starter.cache.core.CacheGetFilter;
import org.opengoofy.index12306.framework.starter.cache.core.CacheGetIfAbsent;
import org.opengoofy.index12306.framework.starter.cache.core.CacheLoader;
import org.opengoofy.index12306.framework.starter.cache.toolkit.CacheUtil;
import org.opengoofy.index12306.framework.starter.cache.toolkit.FastJson2Util;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;

import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * 分布式缓存之操作 Redis 模版代理
 * 对外暴露一套更高层的分布式缓存接口 DistributedCache，
 * 对内则委托真实的 Redis 客户端（StringRedisTemplate + RedissonClient）去执行操作，
 * 并在调用前后附加一堆增强逻辑
 * safePut和safeGet都和布隆过滤器有关，前者会更新，后者会查看
 * 底层通过 {@link RedissonClient}、{@link StringRedisTemplate} 完成外观接口行为
 */
@RequiredArgsConstructor
public class StringRedisTemplateProxy implements DistributedCache {

    private final StringRedisTemplate stringRedisTemplate;
    private final RedisDistributedProperties redisProperties;
    private final RedissonClient redissonClient;

    private static final String LUA_PUT_IF_ALL_ABSENT_SCRIPT_PATH = "lua/putIfAllAbsent.lua";
    private static final String SAFE_GET_DISTRIBUTED_LOCK_KEY_PREFIX = "safe_get_distributed_lock_get:";

    /*
     * 从缓存中取值，如果是字符串直接返回，如果不是，则序列化后返回
     */
    @Override
    public <T> T get(String key, Class<T> clazz) {
        // 从缓存中取值
        String value = stringRedisTemplate.opsForValue().get(key);
        // 如果 clazz 是 String 类型，直接返回 value，isAssignableFrom 判断 clazz 是否是 String 类型
        // 不能用clazz.isInstance(String)，因为该方法需要传入对象实例，不是类
        // 也不好用clazz.isInstance(key)，语义不同，isAssignableFrom判断clazz是否是String类型
        // 而另者判断的是key是否是clazz的实例，当clazz为Object时，会错判
        if (String.class.isAssignableFrom(clazz)) {
            return (T) value;
        }
        // 如果 clazz 不是 String 类型，则使用 FastJson2Util 工具类将 value 转换为 clazz 类型
        // 兼容泛型类型的反序列化，直接使用clazz，会擦除泛型信息
        return JSON.parseObject(value, FastJson2Util.buildType(clazz));
    }

    @Override
    public <T> T get(@NotBlank String key, Class<T> clazz, CacheLoader<T> cacheLoader, long timeout) {
        return get(key, clazz, cacheLoader, timeout, redisProperties.getValueTimeUnit());
    }

    /*
     * 相比普通get，它会判断得到的结果是否为空，如果为空，调用cacheLoader进行重建
     */
    @Override
    public <T> T get(@NotBlank String key, Class<T> clazz, CacheLoader<T> cacheLoader, long timeout, TimeUnit timeUnit) {
        T result = get(key, clazz);
        if (!CacheUtil.isNullOrBlank(result)) {
            return result;
        }
        // 如果缓存结果为空，则调用 cacheLoader 加载数据，并设置到缓存中
        return loadAndSet(key, cacheLoader, timeout, timeUnit, false, null);
    }

    @Override
    public <T> T safeGet(@NotBlank String key, Class<T> clazz, CacheLoader<T> cacheLoader, long timeout) {
        return safeGet(key, clazz, cacheLoader, timeout, redisProperties.getValueTimeUnit());
    }

    @Override
    public <T> T safeGet(@NotBlank String key, Class<T> clazz, CacheLoader<T> cacheLoader, long timeout, TimeUnit timeUnit) {
        return safeGet(key, clazz, cacheLoader, timeout, timeUnit, null);
    }

    @Override
    public <T> T safeGet(@NotBlank String key, Class<T> clazz, CacheLoader<T> cacheLoader, long timeout, RBloomFilter<String> bloomFilter) {
        return safeGet(key, clazz, cacheLoader, timeout, bloomFilter, null, null);
    }

    @Override
    public <T> T safeGet(@NotBlank String key, Class<T> clazz, CacheLoader<T> cacheLoader, long timeout, TimeUnit timeUnit, RBloomFilter<String> bloomFilter) {
        return safeGet(key, clazz, cacheLoader, timeout, timeUnit, bloomFilter, null, null);
    }

    @Override
    public <T> T safeGet(String key, Class<T> clazz, CacheLoader<T> cacheLoader, long timeout, RBloomFilter<String> bloomFilter, CacheGetFilter<String> cacheCheckFilter) {
        return safeGet(key, clazz, cacheLoader, timeout, redisProperties.getValueTimeUnit(), bloomFilter, cacheCheckFilter, null);
    }

    @Override
    public <T> T safeGet(String key, Class<T> clazz, CacheLoader<T> cacheLoader, long timeout, TimeUnit timeUnit, RBloomFilter<String> bloomFilter, CacheGetFilter<String> cacheCheckFilter) {
        return safeGet(key, clazz, cacheLoader, timeout, timeUnit, bloomFilter, cacheCheckFilter, null);
    }

    @Override
    public <T> T safeGet(String key, Class<T> clazz, CacheLoader<T> cacheLoader, long timeout,
                         RBloomFilter<String> bloomFilter, CacheGetFilter<String> cacheGetFilter, CacheGetIfAbsent<String> cacheGetIfAbsent) {
        return safeGet(key, clazz, cacheLoader, timeout, redisProperties.getValueTimeUnit(), bloomFilter, cacheGetFilter, cacheGetIfAbsent);
    }

    @Override
    public <T> T safeGet(String key, Class<T> clazz, CacheLoader<T> cacheLoader, long timeout, TimeUnit timeUnit,
                         RBloomFilter<String> bloomFilter, CacheGetFilter<String> cacheGetFilter, CacheGetIfAbsent<String> cacheGetIfAbsent) {
        // 先从缓存中取值
        T result = get(key, clazz);
        // 缓存结果不等于空或空字符串直接返回；
        // 缓存不存在或为空时，如果 filter 存在并且 filter(key) 返回 true，代表允许直接返回（例如某些特殊 key 不需要回源），为了适配布隆过滤器无法删除的场景，就提前结束
        // 缓存不存在或为空时，如果 bloomFilter 存在且 bloomFilter.contains(key) 返回 false，代表该 key 不在布隆过滤器中，直接返回空，避免回源，避免缓存穿透
        // 顺序不能乱
        if (!CacheUtil.isNullOrBlank(result)
                || Optional.ofNullable(cacheGetFilter).map(each -> each.filter(key)).orElse(false)
                || Optional.ofNullable(bloomFilter).map(each -> !each.contains(key)).orElse(false)) {
            return result;
        }
        // 缓存结果为空，并且可能存在于数据库中，则获取分布式锁，进行缓存重建
        RLock lock = redissonClient.getLock(SAFE_GET_DISTRIBUTED_LOCK_KEY_PREFIX + key);
        lock.lock();
        try {
            // 双重判定锁，减轻获得分布式锁后线程访问数据库压力
            // 可能前一个线程已经重建完缓存了，而后续线程因为排队拿锁，所以又进来重建了，所以需要再次判断
            if (CacheUtil.isNullOrBlank(result = get(key, clazz))) {
                // 先重建缓存，如果访问 cacheLoader 加载数据为空，执行后置函数操作
                if (CacheUtil.isNullOrBlank(result = loadAndSet(key, cacheLoader, timeout, timeUnit, true, bloomFilter))) {
                    // 如果传入了cacheGetIfAbsent，那就执行，用于查询结果为空时执行逻辑
                    Optional.ofNullable(cacheGetIfAbsent).ifPresent(each -> each.execute(key));
                }
            }
        } finally {
            lock.unlock();
        }
        return result;
    }

    @Override
    public void put(String key, Object value) {
        put(key, value, redisProperties.getValueTimeout());
    }


    @Override
    public void put(String key, Object value, long timeout) {
        put(key, value, timeout, redisProperties.getValueTimeUnit());
    }

    @Override
    public void put(String key, Object value, long timeout, TimeUnit timeUnit) {
        String actual = value instanceof String ? (String) value : JSON.toJSONString(value);
        stringRedisTemplate.opsForValue().set(key, actual, timeout, timeUnit);
    }

    @Override
    public void safePut(String key, Object value, long timeout, RBloomFilter<String> bloomFilter) {
        safePut(key, value, timeout, redisProperties.getValueTimeUnit(), bloomFilter);
    }

    @Override
    public void safePut(String key, Object value, long timeout, TimeUnit timeUnit, RBloomFilter<String> bloomFilter) {
        put(key, value, timeout, timeUnit);
        // 添加新元素后，要保证一致性，布隆过滤器中也要添加
        if (bloomFilter != null) {
            bloomFilter.add(key);
        }
    }

        /**
     * 执行 Lua 脚本，实现批量 putIfAbsent 操作，当所有 key 都不存在时，才会全部写入。
     * 使用 Singleton 保证脚本只加载一次，提升性能。
     * 
     * @param keys 需要批量 putIfAbsent 的 key 集合
     * @return 全部写入返回 true，否则 false
     */
    /**
     * 这里需要lua脚本，是因为Redis本身并没有“批量putIfAbsent且原子性保证”这样的操作。
     * Redis的msetnx虽然可以一次性设置多个key，但只能用在所有key都不存在时，且没有办法设置过期时间。
     * 而如果通过普通代码操作，做多次setIfAbsent，然后再统一设置过期时间，会导致并发下的原子性丢失（比如有些key被其他线程提前写入，导致部分写入）。
     * 使用lua脚本，可以保证在Redis服务器端以原子方式判断所有key都不存在时，才进行写入操作及过期时间设置，避免并发问题和保障一致性。
     */
    @Override
    public Boolean putIfAllAbsent(@NotNull Collection<String> keys) {
        // 获取或初始化 Lua 脚本对象，避免每次调用都创建 Script 实例
        DefaultRedisScript<Boolean> actual = Singleton.get(LUA_PUT_IF_ALL_ABSENT_SCRIPT_PATH, () -> {
            DefaultRedisScript<Boolean> redisScript = new DefaultRedisScript<>();
            redisScript.setScriptSource(new ResourceScriptSource(new ClassPathResource(LUA_PUT_IF_ALL_ABSENT_SCRIPT_PATH)));
            redisScript.setResultType(Boolean.class);
            return redisScript;
        });
        // 执行 Lua 脚本，如果全部设置成功返回 true
        Boolean result = stringRedisTemplate.execute(actual, Lists.newArrayList(keys), redisProperties.getValueTimeout().toString());
        return result != null && result;
    }


    @Override
    public Boolean hasKey(String key) {
        return stringRedisTemplate.hasKey(key);
    }

    @Override
    public Boolean delete(String key) {
        return stringRedisTemplate.delete(key);
    }

    @Override
    public Long delete(Collection<String> keys) {
        return stringRedisTemplate.delete(keys);
    }

    @Override
    public Object getInstance() {
        return stringRedisTemplate;
    }

    @Override
    public Long countExistingKeys(String... keys) {
        return stringRedisTemplate.countExistingKeys(Lists.newArrayList(keys));
    }

    private <T> T loadAndSet(String key, CacheLoader<T> cacheLoader, long timeout, TimeUnit timeUnit, boolean safeFlag, RBloomFilter<String> bloomFilter) {
        T result = cacheLoader.load();
        if (CacheUtil.isNullOrBlank(result)) {
            return result;
        }
        if (safeFlag) {
            safePut(key, result, timeout, timeUnit, bloomFilter);
        } else {
            put(key, result, timeout, timeUnit);
        }
        return result;
    }
}
