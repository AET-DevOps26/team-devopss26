const fs = require('fs');
const path = require('path');

// 1. Clean services/*/generated
const servicesDir = path.resolve(__dirname, '../services');
if (fs.existsSync(servicesDir)) {
  const services = fs.readdirSync(servicesDir);
  services.forEach(service => {
    const genDir = path.join(servicesDir, service, 'generated');
    if (fs.existsSync(genDir)) {
      console.log(`Cleaning ${genDir}...`);
      fs.rmSync(genDir, { recursive: true, force: true });
    }
  });
}

// 2. Clean web-client/src/*-service.ts
const webClientSrc = path.resolve(__dirname, '../web-client/src');
if (fs.existsSync(webClientSrc)) {
  const files = fs.readdirSync(webClientSrc);
  files.forEach(file => {
    if (file.endsWith('-service.ts') || file === 'api.ts') {
      const filePath = path.join(webClientSrc, file);
      console.log(`Cleaning ${filePath}...`);
      fs.rmSync(filePath, { force: true });
    }
  });
}
