const UUID_SHAPE = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i

/** Backend IDs are UUIDs; checking the shape first turns a backend 400 into a field error. */
export const isUuid = (value: string) => UUID_SHAPE.test(value)

/** Field error for an ID the user typed, or undefined when it is a full UUID. */
export function uuidError(value: string, what: string): string | undefined {
  if (!value) return `Enter the ${what}.`
  if (!isUuid(value)) return `Enter the full ${what}, like 0190a1b2-c3d4-7e5f-8a9b-0c1d2e3f4a5b.`
  return undefined
}
