// Values the discovery feed accepts (#18, PR #127). OpenAPI types these filters as plain strings,
// so the lists live here. Labels match the backend's display labels in FeedItem.type and
// FeedItem.sourceLabel. This module is never mocked, so tests see the real lists.

export const OPPORTUNITY_CATEGORIES = [
  { value: 'JOB', label: 'Job' },
  { value: 'INTERNSHIP', label: 'Internship' },
  { value: 'COOP', label: 'Co-op' },
  { value: 'RESEARCH_POSITION', label: 'Research position' },
  { value: 'RESEARCH_PARTICIPATION', label: 'Research participation' },
  { value: 'CAPSTONE', label: 'Capstone sponsorship' },
] as const

export const SOURCE_LABELS = [
  { value: 'VERIFIED_UPRM_INSTITUTION', label: 'Verified UPRM institution' },
  { value: 'APPROVED_EXTERNAL_COMPANY', label: 'Approved external company' },
  { value: 'VERIFIED_STUDENT_PROJECT', label: 'Verified student project' },
] as const

export const FEED_PAGE_SIZE = 20
