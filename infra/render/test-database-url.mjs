// Focused, network-free regression test of the parser copied into the image.
// Usage: node infra/render/test-database-url.mjs colegio-connect:render-url-test
import { spawnSync } from 'node:child_process'

const image = process.argv[2]
if (!image) throw new Error('Pass the locally built image tag')
const password = 'synthetic:@/?!%'
const cases = [
  ['portless Render URL', 'postgresql://user:p%40ss%2Fword@db.internal/cole_test', 'jdbc:postgresql://db.internal:5432/cole_test?sslmode=require'],
  ['explicit port', 'postgresql://user:p%40ss@db.internal:6543/cole_test', 'jdbc:postgresql://db.internal:6543/cole_test?sslmode=require'],
  ['postgres alias and query', 'postgres://user:p%3Aword@db.internal/cole_test?sslmode=require', 'jdbc:postgresql://db.internal:5432/cole_test?sslmode=require'],
  ['raw at in userinfo', 'postgresql://user:p@ss@db.internal/cole_test', 'jdbc:postgresql://db.internal:5432/cole_test?sslmode=require'],
  ['bracketed IPv6', 'postgresql://user:p%40ss@[::1]/cole_test', 'jdbc:postgresql://[::1]:5432/cole_test?sslmode=require'],
  ['invalid scheme', 'https://user:pass@db.internal/cole_test', null],
  ['missing host', 'postgresql://user:pass@/cole_test', null],
  ['blank explicit port', 'postgresql://user:pass@db.internal:/cole_test', null],
  ['nonnumeric port', 'postgresql://user:pass@db.internal:abc/cole_test', null],
  ['out-of-range port', 'postgresql://user:pass@db.internal:65536/cole_test', null],
  ['missing database', 'postgresql://user:pass@db.internal/', null],
  ['wrong database reference', 'postgresql://user:pass@db.internal/other_db', null],
  ['invalid host', 'postgresql://user:pass@db;bad.internal/cole_test', null],
]

for (const [label, url, expected] of cases) {
  const result = spawnSync('docker', [
    'run', '--rm', '--network', 'none', '--entrypoint', 'sh',
    '-e', `DATABASE_URL=${url}`, '-e', 'DB_NAME=cole_test',
    '-e', 'DB_USER=synthetic', '-e', `DB_PASSWORD=${password}`,
    '-e', `EXPECTED_PASSWORD=${password}`,
    image, '-c', 'set -eu; . /app/parse-database-url.sh; [ "$SPRING_DATASOURCE_USERNAME" = synthetic ]; [ "$SPRING_DATASOURCE_PASSWORD" = "$EXPECTED_PASSWORD" ]; printf "%s" "$SPRING_DATASOURCE_URL"',
  ], { encoding: 'utf8', timeout: 30000 })
  const output = `${result.stdout ?? ''}${result.stderr ?? ''}`
  if (output.includes(password) || output.includes('p%40ss')) {
    throw new Error(`${label}: credential appeared in output`)
  }
  if (expected === null ? result.status === 0 : result.status !== 0 || result.stdout !== expected) {
    throw new Error(`${label}: unexpected parser result (exit ${result.status})`)
  }
}
console.log(`PASS ${cases.length} synthetic PostgreSQL URL cases; credentials absent from output`)
