-- 令牌桶限流算法
local key = KEYS[1]
local capacity = tonumber(ARGV[1] or 0)
local timestamp = tonumber(ARGV[2] or 0)
local rate = tonumber(ARGV[3] or 0.1)
local app = tonumber(ARGV[4] or 1)

-- 参数保护
if capacity <= 0 then capacity = 10 end
if rate <= 0 then rate = 0.1 end

local fill_time = capacity / rate
local ttl = math.floor(fill_time * 2)
local current_tokens = tonumber(redis.call('get', key) or capacity)
local last_tokens_time = tonumber(redis.call('get', key .. ':timestamp') or 0)
local delta = math.max(0, timestamp - last_tokens_time)
local filled_tokens = math.min(capacity, current_tokens + (delta * rate))
local allowed = filled_tokens >= app
local new_tokens = filled_tokens

-- 只有当 allowed 为 true 时，才会执行令牌数的更新写入操作
if allowed then
    new_tokens = filled_tokens - app
end

redis.call('setex', key, ttl, new_tokens)
redis.call('setex', key .. ':timestamp', ttl, timestamp)

return allowed and 1 or 0