import { mkdir, writeFile } from 'node:fs/promises'

const target = process.env.VITE_API_TARGET ?? 'http://localhost:8080'
const res = await fetch(`${target}/v3/api-docs`)
if (!res.ok) {
  console.error(`Failed to fetch ${target}/v3/api-docs: ${res.status} ${res.statusText}`)
  process.exit(1)
}
const spec = await res.json()
await mkdir('openapi', { recursive: true })
await writeFile('openapi/openapi.json', JSON.stringify(spec, null, 2) + '\n')
console.log(`Saved OpenAPI ${spec.openapi} spec (${Object.keys(spec.paths ?? {}).length} paths)`)
