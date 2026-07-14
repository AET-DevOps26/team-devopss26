const fs = require('fs');
const path = require('path');

const webClientDir = path.resolve(__dirname, '..');
const apiDir = path.resolve(webClientDir, '../api');
const targetDir = path.resolve(webClientDir, 'public/swagger');

try {
  // Ensure api directory exists
  if (!fs.existsSync(apiDir)) {
    console.error(`Error: API source directory not found at "${apiDir}". Please ensure it exists.`);
    process.exit(1);
  }

  // Ensure target directory exists
  if (!fs.existsSync(targetDir)) {
    fs.mkdirSync(targetDir, { recursive: true });
  }

  // Resolve swagger-ui-dist path
  let swaggerUiDistPath;
  try {
    swaggerUiDistPath = path.dirname(require.resolve('swagger-ui-dist'));
  } catch (resolveError) {
    console.error('Error: Failed to resolve "swagger-ui-dist". Make sure it is installed in devDependencies.');
    console.error(resolveError.message);
    process.exit(1);
  }

const filesToCopy = [
    'swagger-ui-bundle.js',
    'swagger-ui-standalone-preset.js',
    'swagger-ui.css',
    'favicon-32x32.png',
    'favicon-16x16.png'
  ];

  console.log('Copying Swagger UI assets...');
  filesToCopy.forEach(file => {
    const src = path.join(swaggerUiDistPath, file);
    const dest = path.join(targetDir, file);
    fs.copyFileSync(src, dest);
    console.log(`Copied ${file}`);
  });

  // Find and copy all YAML files in api directory
  console.log('Copying API specification files...');
  const files = fs.readdirSync(apiDir);
  const urls = [];

  // Order the specs to put main/overview spec first
  const orderedServices = [
    { file: 'openapi.yaml', name: 'Overview / API Index' },
    { file: 'note-service.yaml', name: 'Note Service' },
    { file: 'checklist-service.yaml', name: 'Checklist Service' },
    { file: 'calendar-service.yaml', name: 'Calendar Service' },
    { file: 'genai-service.yaml', name: 'GenAI Service' },
    { file: 'user-service.yaml', name: 'User Service' },
    { file: 'admin-service.yaml', name: 'Admin Service' }
  ];

  // Helper to get name for services not explicitly ordered
  const getServiceName = (filename) => {
    const base = filename.replace(/\.(yaml|yml)$/, '');
    return base.split('-').map(word => word.charAt(0).toUpperCase() + word.slice(1)).join(' ');
  };

  // Copy ordered services
  orderedServices.forEach(item => {
    const srcPath = path.join(apiDir, item.file);
    if (fs.existsSync(srcPath)) {
      fs.copyFileSync(srcPath, path.join(targetDir, item.file));
      // Exclude common.yaml from the dropdown urls array since it contains no paths
      if (item.file !== 'common.yaml') {
        urls.push({ url: `./${item.file}`, name: item.name });
      }
      console.log(`Copied ${item.file} -> ${item.name}`);
    }
  });

  // Copy any other yaml files in api directory that are not in orderedServices list
  files.forEach(file => {
    if ((file.endsWith('.yaml') || file.endsWith('.yml')) && !orderedServices.some(item => item.file === file)) {
      const srcPath = path.join(apiDir, file);
      fs.copyFileSync(srcPath, path.join(targetDir, file));
      // Exclude common.yaml from the dropdown urls array since it contains no paths
      if (file !== 'common.yaml') {
        const name = getServiceName(file);
        urls.push({ url: `./${file}`, name: name });
        console.log(`Copied ${file} -> ${name}`);
      } else {
        console.log(`Copied ${file} (shared spec, excluded from dropdown)`);
      }
    }
  });

  // Generate index.html for Swagger UI
  const indexHtmlContent = `<!DOCTYPE html>
<html lang="en">
  <head>
    <meta charset="UTF-8">
    <title>DevOps SS26 API Documentation</title>
    <link rel="stylesheet" type="text/css" href="./swagger-ui.css" />
    <link rel="icon" type="image/png" href="./favicon-32x32.png" sizes="32x32" />
    <link rel="icon" type="image/png" href="./favicon-16x16.png" sizes="16x16" />
    <style>
      html {
        box-sizing: border-box;
        overflow: -moz-scrollbars-vertical;
        overflow-y: scroll;
      }

      *,
      *:before,
      *:after {
        box-sizing: inherit;
      }

      body {
        margin: 0;
        background: #fafafa;
      }

      /* Premium styling for Swagger UI */
      .swagger-ui .topbar {
        background-color: #0f172a;
        box-shadow: 0 4px 6px -1px rgb(0 0 0 / 0.1), 0 2px 4px -2px rgb(0 0 0 / 0.1);
        padding: 12px 0;
      }
      .swagger-ui .topbar a {
        max-width: 300px;
        text-decoration: none;
      }
      .swagger-ui .topbar a span {
        font-family: 'Inter', sans-serif;
        font-weight: 700;
        color: #f1f5f9;
        font-size: 1.25rem;
      }
      .swagger-ui .topbar .download-url-wrapper select {
        border: 1px solid #334155;
        border-radius: 6px;
        background: #1e293b;
        color: #f8fafc;
        padding: 6px 12px;
        font-size: 0.9rem;
        outline: none;
        cursor: pointer;
        min-width: 250px;
      }
      .swagger-ui .topbar .download-url-wrapper select:focus {
        border-color: #3b82f6;
      }
    </style>
  </head>

  <body>
    <div id="swagger-ui"></div>

    <script src="./swagger-ui-bundle.js" charset="UTF-8"> </script>
    <script src="./swagger-ui-standalone-preset.js" charset="UTF-8"> </script>
    <script>
    window.onload = function() {
      const ui = SwaggerUIBundle({
        urls: ${JSON.stringify(urls, null, 2)},
        dom_id: '#swagger-ui',
        deepLinking: true,
        presets: [
          SwaggerUIBundle.presets.apis,
          SwaggerUIStandalonePreset
        ],
        plugins: [
          SwaggerUIBundle.plugins.DownloadUrl
        ],
        layout: "StandaloneLayout"
      });

      window.ui = ui;
    };
  </script>
  </body>
</html>
`;

  fs.writeFileSync(path.join(targetDir, 'index.html'), indexHtmlContent);
  console.log('Generated index.html successfully!');
} catch (error) {
  console.error('CRITICAL ERROR: Failed to generate Swagger UI assets.');
  console.error(error.message || error);
  process.exit(1);
}
