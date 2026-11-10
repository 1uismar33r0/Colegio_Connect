/** A sidebar section whose screen is built by a later issue. */
export function SectionPlaceholder({ title, issue }: { title: string; issue?: number }) {
  return (
    <>
      <h1>{title}</h1>
      <p>
        {issue
          ? `This section is not available yet. It is built in issue #${issue}.`
          : 'This section is not available yet. It is planned for a later milestone.'}
      </p>
    </>
  )
}
