const { execSync } = require('child_process');
const fs = require('fs');
const path = require('path');

const servicesDir = path.resolve(__dirname, '../services');
let failed = false;

if (fs.existsSync(servicesDir)) {
  const services = fs.readdirSync(servicesDir);
  for (const service of services) {
    const pomPath = path.join(servicesDir, service, 'pom.xml');
    if (fs.existsSync(pomPath)) {
      console.log(`\n========================================`);
      console.log(`Running tests and static analysis for service: ${service}`);
      console.log(`========================================`);
      try {
        const serviceDir = path.join(servicesDir, service);
        const hasWrapper = fs.existsSync(path.join(serviceDir, 'mvnw'));
        const cmd = hasWrapper 
          ? (process.platform === 'win32' ? 'mvnw.cmd' : './mvnw')
          : 'mvn';
        
        execSync(`${cmd} clean verify spotbugs:check -Dsurefire.exitTimeout=1 -DargLine="-Dlogging.level.com.tngtech.archunit.core.importer.ClassFileProcessor=ERROR"`, {
          cwd: serviceDir,
          stdio: 'inherit'
        });
      } catch (error) {
        console.error(`\nTests or static analysis failed for service: ${service}`);
        failed = true;
      }
    }
  }
}

if (failed) {
  process.exit(1);
} else {
  console.log('\nAll backend tests and static analysis completed successfully.');
}
