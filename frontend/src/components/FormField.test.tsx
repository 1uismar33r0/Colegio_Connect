import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { FormField } from './FormField'

describe('FormField', () => {
  it('links the label to the input', () => {
    render(<FormField label="Email" type="email" />)
    expect(screen.getByLabelText('Email')).toHaveAttribute('type', 'email')
  })

  it('announces hint and error, and marks the input invalid', () => {
    render(<FormField label="Email" hint="Use your @upr.edu address" error="Email is required" />)
    const input = screen.getByLabelText('Email')
    expect(input).toHaveAttribute('aria-invalid', 'true')
    expect(input).toHaveAccessibleDescription('Use your @upr.edu address Email is required')
  })

  it('is not marked invalid without an error', () => {
    render(<FormField label="Email" />)
    expect(screen.getByLabelText('Email')).not.toHaveAttribute('aria-invalid')
  })
})
