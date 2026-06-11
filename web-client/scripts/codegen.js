import { execSync } from 'child_process';
import fs from 'fs';
import path from 'path';

const apiDir = path.resolve('../api');
const srcDir = path.resolve('src');

if (!fs.existsSync(apiDir)) {
  console.error('API directory not found at:', apiDir);
  process.exit(1);
}

const files = fs.readdirSync(apiDir);
files.forEach(file => {
  if (file.endsWith('.yaml') || file.endsWith('.yml')) {
    const serviceName = path.basename(file, path.extname(file));
    const inputFile = path.join(apiDir, file);
    const outputFile = path.join(srcDir, `${serviceName}.ts`);
    console.log(`Generating TypeScript types for ${serviceName}...`);
    try {
      execSync(`npx openapi-typescript "${inputFile}" -o "${outputFile}"`, { stdio: 'inherit' });
    } catch (e) {
      console.error(`Failed to generate types for ${serviceName}:`, e.message);
    }
  }
});
