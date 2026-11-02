import { fireEvent, render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { Composer } from './composer'

function renderComposer() {
  const onSend = vi.fn()
  render(<Composer name="Marek" onSend={onSend} />)
  return {
    onSend,
    field: screen.getByRole('textbox', { name: 'Message Marek' }),
    send: screen.getByRole('button', { name: 'Send message' }),
  }
}

describe('Composer', () => {
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

    expect(onSend).toHaveBeenCalledExactlyOnceWith('hello')
    expect(field).toHaveValue('')
    expect(field).toHaveFocus()
  })

  it('sends with the button and returns focus to the field', async () => {
    const { field, send, onSend } = renderComposer()

    await userEvent.type(field, 'hello')
    await userEvent.click(send)

    expect(onSend).toHaveBeenCalledExactlyOnceWith('hello')
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
