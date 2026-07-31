const fs = require('fs');
const path = require('path');

const wwwDir = path.join(__dirname, 'www');

// Clean and create www directory
if (fs.existsSync(wwwDir)) {
  fs.rmSync(wwwDir, { recursive: true, force: true });
}
fs.mkdirSync(wwwDir, { recursive: true });

function copyRecursive(src, dest) {
  const stats = fs.statSync(src);
  if (stats.isDirectory()) {
    fs.mkdirSync(dest, { recursive: true });
    for (const file of fs.readdirSync(src)) {
      copyRecursive(path.join(src, file), path.join(dest, file));
    }
  } else {
    fs.copyFileSync(src, dest);
  }
}

// Files and folders to copy to www
const itemsToCopy = ['index.html', 'manifest.json', 'sw.js', 'css', 'js', 'icons'];

for (const item of itemsToCopy) {
  const srcPath = path.join(__dirname, item);
  const destPath = path.join(wwwDir, item);
  if (fs.existsSync(srcPath)) {
    copyRecursive(srcPath, destPath);
  }
}

console.log('✅ Web assets successfully packaged into www/ folder!');
