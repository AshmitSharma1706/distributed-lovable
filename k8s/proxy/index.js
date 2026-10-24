const http = require('http');
const httpProxy = require('http-proxy');
const Redis = require('ioredis');

// Fallback to cluster service DNS
const redisUrl = process.env.REDIS_URL || 'redis://redis.lovable-core.svc.cluster.local:6379';

const redis = new Redis(redisUrl, {
    maxRetriesPerRequest: 1,
    commandTimeout: 2000,
    enableOfflineQueue: false,
    enableReadyCheck: true,
    connectTimeout: 5000,
    retryStrategy(times) {
        const delay = Math.min(times * 100, 2000);
        console.log(`[Redis] Reconnecting in ${delay}ms (attempt ${times})...`);
        return delay;
    }
});

redis.on('error', (err) => console.error('[Redis Client Error]:', err.message));
redis.on('connect', () => console.log(`✅ Connected to Redis successfully at ${redisUrl}`));

const proxy = httpProxy.createProxyServer({
    ws: true,
    xfwd: true,
    changeOrigin: true,
    proxyTimeout: 10000,
    timeout: 10000
});

// Catch proxy-level errors to prevent process crashes
proxy.on('error', (err, req, res) => {
    const host = req?.headers?.host || 'unknown';
    console.error(`[Proxy Core Error] ${host} -> ${err.message}`);
    if (res && !res.headersSent && typeof res.writeHead === 'function') {
        res.writeHead(502, { 'Content-Type': 'text/plain' });
        res.end('Preview server unavailable or starting...');
    }
});

async function getTarget(hostname) {
    try {
        // 1. Direct route lookup
        let targetIpOrSvc = await redis.get(`route:${hostname}`);
        if (targetIpOrSvc) return targetIpOrSvc;

        // 2. Fallback: extract project ID and check known base domains
        const match = hostname.match(/^project-(\d+)/i);
        if (match) {
            const projectId = match[1];
            const sslipKey = `route:project-${projectId}.34-93-185-166.sslip.io`;
            const csKey = `route:project-${projectId}.previews.codingshuttle.in`;

            targetIpOrSvc = (await redis.get(sslipKey)) || (await redis.get(csKey));
            if (targetIpOrSvc) {
                console.log(`[Proxy Fallback] Host '${hostname}' resolved via project-${projectId} -> ${targetIpOrSvc}`);
                return targetIpOrSvc;
            }
        }
        return null;
    } catch (err) {
        console.error(`[Redis Lookup Error for ${hostname}]:`, err.message);
        return null;
    }
}

const getTargetUrl = (target) => {
    return target.includes(':') ? `http://${target}` : `http://${target}:5173`;
};

const server = http.createServer(async (req, res) => {
    const startTime = Date.now();
    const rawHost = req.headers.host || '';
    const hostname = rawHost.split(':')[0];

    const targetIpOrSvc = await getTarget(hostname);
    const lookupDuration = Date.now() - startTime;

    if (!targetIpOrSvc) {
        console.log(`[404] ${req.method} ${hostname}${req.url} (Redis lookup: ${lookupDuration}ms) -> Route not found`);
        res.writeHead(404, { 'Content-Type': 'text/plain' });
        return res.end(`Preview not found or spinning up for ${hostname}.`);
    }

    const target = getTargetUrl(targetIpOrSvc);
    console.log(`[HTTP Proxy] ${req.method} ${hostname}${req.url} -> ${target}${req.url} (${lookupDuration}ms)`);

    proxy.web(req, res, { target }, (e) => {
        console.error(`[Proxy Web Error] ${hostname} -> ${e.message}`);
        if (!res.headersSent) {
            res.writeHead(502, { 'Content-Type': 'text/plain' });
            res.end('Preview server unavailable or starting...');
        }
    });
});

server.on('upgrade', async (req, socket, head) => {
    const rawHost = req.headers.host || '';
    const hostname = rawHost.split(':')[0];

    socket.on('error', (err) => {
        console.error(`[WS Client Socket Error] ${hostname}: ${err.message}`);
    });

    const targetIpOrSvc = await getTarget(hostname);

    if (socket.destroyed) {
        return;
    }

    if (targetIpOrSvc) {
        const target = getTargetUrl(targetIpOrSvc);
        console.log(`[WS Proxy] ${hostname} -> ${target}`);

        proxy.ws(req, socket, head, { target }, (e) => {
            console.error(`[Proxy WS Error] ${hostname} -> ${e.message}`);
            if (!socket.destroyed) {
                socket.destroy();
            }
        });
    } else {
        console.log(`[WS 404] ${hostname} -> No route found for upgrade`);
        socket.destroy();
    }
});

const PORT = process.env.PORT || 80;
server.listen(PORT, '0.0.0.0', () =>
    console.log(`🚀 Wildcard Proxy Active on Port ${PORT} (bound to 0.0.0.0)`)
);