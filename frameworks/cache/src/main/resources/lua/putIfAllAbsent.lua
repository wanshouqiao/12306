--[[KEYS 全部不存在，创建并返回 true，反之返回空]]

-- 检查所有 KEYS 是否都不存在，若存在则返回 nil
for i, v in ipairs(KEYS) do
    if (redis.call('exists', v) == 1) then
        return nil;
    end
end

-- 为所有 KEYS 创建默认值并设置过期时间
for i, v in ipairs(KEYS) do
    redis.call('set', v, 'default');
    redis.call('pexpire', v, ARGV[1]);
end

-- 所有操作完成，返回 true
return true;
