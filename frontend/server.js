const express = require('express');
const fs = require('fs');
const http = require('http');
const https = require('https');
const path = require('path');

const app = express();
const port = Number(process.env.PORT || 8081);
const backendUrl = process.env.BACKEND_URL
  || `http://${process.env.BACKEND_HOST || 'localhost'}:${process.env.BACKEND_PORT || 8082}`;
const buildDir = path.join(__dirname, 'build');

function proxyApi(req, res) {
  const target = new URL(backendUrl);
  const client = target.protocol === 'https:' ? https : http;
  const headers = { ...req.headers, host: target.host };

  const proxyReq = client.request({
    protocol: target.protocol,
    hostname: target.hostname,
    port: target.port,
    method: req.method,
    path: req.originalUrl,
    headers,
  }, (proxyRes) => {
    res.writeHead(proxyRes.statusCode || 502, proxyRes.headers);
    proxyRes.pipe(res);
  });

  proxyReq.on('error', (error) => {
    res.status(502).json({
      message: 'Backend proxy failed',
      backendUrl,
      error: error.message,
    });
  });

  req.pipe(proxyReq);
}

if (!fs.existsSync(buildDir)) {
  console.error('Missing frontend/build. Run npm run build before npm run serve.');
  process.exit(1);
}

app.use('/api', proxyApi);
app.get('/frontend/health', (_req, res) => {
  res.json({ status: 'UP', tier: 'frontend', backendUrl });
});
app.use(express.static(buildDir));
app.get('*', (_req, res) => {
  res.sendFile(path.join(buildDir, 'index.html'));
});

app.listen(port, '0.0.0.0', () => {
  console.log(`OneAhead frontend listening on ${port}`);
  console.log(`Proxying API requests to ${backendUrl}`);
  console.log(`Dynatrace tags: ${process.env.DT_TAGS || ''}`);
});
