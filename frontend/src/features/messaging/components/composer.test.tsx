import { fireEvent, render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { Composer } from './composer'

function renderComposer() {
  const onSend = vi.fn()
  render(<Composer name="Marek" draftKey="client" onSend={onSend} />)
  return {
    onSend,
    field: screen.getByRole('textbox', { name: 'Message Marek' }),
    send: screen.getByRole('button', { name: 'Send message' }),
  }
}

const ABOUT = {
  setLogId: '00000000-0000-4000-b000-000000000001',
  sessionLogId: '00000000-0000-4000-a000-000000000001',
  sessionName: 'Lower body',
  sessionDate: '2026-11-07',
  exerciseName: 'Back squat',
  entryPosition: 0,
  setNumber: 3,
  rpe: 9,
}

describe('Composer', () => {
  it('shows the set asked about above the field, focuses the field, and still needs text to send', () => {
    render(
      <Composer name="Marek" draftKey="client" ask={ABOUT} onSend={vi.fn()} />,
    )

    expect(screen.getByText('About')).toBeInTheDocument()
    expect(
      screen.getByText(/^Lower body · .*7.* · Back squat, set 3 · RPE 9$/),
    ).toBeInTheDocument()
    expect(screen.getByRole('textbox', { name: 'Message Marek' })).toHaveFocus()
    expect(screen.getByRole('button', { name: 'Send message' })).toBeDisabled()
  })

  it('sends the set with the next message only', async () => {
    const onSend = vi.fn()
    render(
      <Composer name="Marek" draftKey="client" ask={ABOUT} onSend={onSend} />,
    )
    const field = screen.getByRole('textbox', { name: 'Message Marek' })

    await userEvent.type(field, 'Too heavy?{Enter}')
    expect(screen.queryByText('About')).not.toBeInTheDocument()
    await userEvent.type(field, 'Thanks{Enter}')

    expect(onSend.mock.calls).toEqual([
      ['Too heavy?', ABOUT],
      ['Thanks', undefined],
    ])
  })

  it('takes the set off the message with its remove button and returns focus to the field', async () => {
    const onSend = vi.fn()
    render(
      <Composer name="Marek" draftKey="client" ask={ABOUT} onSend={onSend} />,
    )

    await userEvent.click(
      screen.getByRole('button', { name: 'Remove the set from this message' }),
    )

    expect(screen.queryByText('About')).not.toBeInTheDocument()
    const field = screen.getByRole('textbox', { name: 'Message Marek' })
    expect(field).toHaveFocus()
    await userEvent.type(field, 'Hello{Enter}')
    expect(onSend).toHaveBeenCalledExactlyOnceWith('Hello', undefined)
  })

  it('keeps what was not sent, text and set, for the same conversation only', async () => {
    const first = render(
      <Composer name="Marek" draftKey="client" ask={ABOUT} onSend={vi.fn()} />,
    )
    await userEvent.type(
      screen.getByRole('textbox', { name: 'Message Marek' }),
      'Half written',
    )
    first.unmount()

    const again = render(
      <Composer name="Marek" draftKey="client" onSend={vi.fn()} />,
    )
    const field = screen.getByRole('textbox', { name: 'Message Marek' })
    expect(field).toHaveValue('Half written')
    expect(screen.getByText('About')).toBeInTheDocument()
    // arriving without a new set is not asking to write
    expect(field).not.toHaveFocus()
    again.unmount()

    render(<Composer name="Marek" draftKey="other" onSend={vi.fn()} />)
    expect(screen.getByRole('textbox', { name: 'Message Marek' })).toHaveValue(
      '',
    )
    expect(screen.queryByText('About')).not.toBeInTheDocument()
  })

  it('disables Send while the field is empty or holds only spaces', async () => {
    const { field, send } = renderComposer()

    expect(send).toBeDisabled()
    await userEvent.type(field, '   ')
    expect(send).toBeDisabled()
    await userEvent.type(field, 'x')
    expect(send).toBeEnabled()
  })

  it('sends the trimmed text on Enter, clears the field and keeps focus', async () => {
    const { field, onSend } = renderComposer()

    await userEvent.type(field, '  hello {Enter}')

    expect(onSend).toHaveBeenCalledExactlyOnceWith('hello', undefined)
    expect(field).toHaveValue('')
    expect(field).toHaveFocus()
  })

  it('sends with the button and returns focus to the field', async () => {
    const { field, send, onSend } = renderComposer()

    await userEvent.type(field, 'hello')
    await userEvent.click(send)

    expect(onSend).toHaveBeenCalledExactlyOnceWith('hello', undefined)
    expect(field).toHaveFocus()
  })

  it('adds a line on Shift+Enter and sends nothing', async () => {
    const { field, onSend } = renderComposer()

    await userEvent.type(field, 'one{Shift>}{Enter}{/Shift}two')

    expect(field).toHaveValue('one\ntwo')
    expect(onSend).not.toHaveBeenCalled()
  })

  it('sends nothing on Enter while the field is empty', async () => {
    const { field, onSend } = renderComposer()

    await userEvent.type(field, '{Enter}')

    expect(onSend).not.toHaveBeenCalled()
  })

  it('sends nothing on an Enter that confirms composed text', () => {
    const { field, onSend } = renderComposer()
    fireEvent.change(field, { target: { value: 'にほん' } })

    fireEvent.keyDown(field, { key: 'Enter', isComposing: true })

    expect(onSend).not.toHaveBeenCalled()
    expect(field).toHaveValue('にほん')
  })

  it('sends nothing on the Enter that Safari reports after a composition', () => {
    const { field, onSend } = renderComposer()
    fireEvent.change(field, { target: { value: 'にほん' } })

    fireEvent.keyDown(field, { key: 'Enter', keyCode: 229 })

    expect(onSend).not.toHaveBeenCalled()
  })

  it('stops pasting and typing at 2000 characters', async () => {
    const { field } = renderComposer()
    await userEvent.click(field)

    await userEvent.paste('a'.repeat(2100))
    expect(field).toHaveValue('a'.repeat(2000))

    await userEvent.type(field, 'b')
    expect(field).toHaveValue('a'.repeat(2000))
  })
})
