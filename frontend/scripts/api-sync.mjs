// Tải tài liệu OpenAPI của từng service về src/api/schema/<service>.json rồi sinh kiểu
// TypeScript cạnh nó (<service>.d.ts). Cả hai file được commit, nên build không cần backend.
//
//   npm run api:sync    cần identity-service (8081) và registry-service (8082) đang chạy
//   npm run api:types   chỉ sinh lại kiểu từ JSON đã có
//   --only=registry     chỉ một service; HP_REGISTRY_PORT=18082 (HP_<SERVICE>_PORT) đổi cổng
import { mkdirSync, readFileSync, writeFileSync } from 'node:fs'
import path from 'node:path'

import openapiTS, { astToString } from 'openapi-typescript'

const port = (name, fallback) => Number(process.env[`HP_${name.toUpperCase()}_PORT`] ?? fallback)
const only = process.argv.find((arg) => arg.startsWith('--only='))?.slice('--only='.length)
const services = Object.fromEntries(
  Object.entries({ identity: port('identity', 8081), registry: port('registry', 8082) }).filter(([name]) => !only || name === only),
)
const schemaDir = path.resolve(import.meta.dirname, '../src/api/schema')
const typesOnly = process.argv.includes('--types-only')

/** Cùng một object với các khoá theo thứ tự chữ cái, để hai lần tải cho cùng một file. */
function sortKeys(object) {
  return Object.fromEntries(Object.entries(object ?? {}).sort(([a], [b]) => a.localeCompare(b)))
}

async function download(name, port) {
  const url = `http://localhost:${port}/v3/api-docs`
  const response = await fetch(url).catch(() => null)
  if (!response?.ok) {
    throw new Error(`Không tải được ${url}. ${name}-service đã chạy chưa?`)
  }
  const doc = await response.json()
  delete doc.servers
  doc.paths = sortKeys(doc.paths)
  if (doc.components?.schemas) {
    doc.components.schemas = sortKeys(doc.components.schemas)
  }
  return doc
}

mkdirSync(schemaDir, { recursive: true })
for (const [name, port] of Object.entries(services)) {
  const jsonFile = path.join(schemaDir, `${name}.json`)
  if (!typesOnly) {
    writeFileSync(jsonFile, `${JSON.stringify(await download(name, port), null, 2)}\n`)
  }
  const doc = JSON.parse(readFileSync(jsonFile, 'utf8'))
  const types = astToString(await openapiTS(doc))
  writeFileSync(
    path.join(schemaDir, `${name}.d.ts`),
    `// Sinh từ ${name}.json bằng \`npm run api:sync\`. Không sửa tay.\n\n${types}`,
  )
  console.log(`${name}: ${Object.keys(doc.paths).length} đường dẫn`)
}
