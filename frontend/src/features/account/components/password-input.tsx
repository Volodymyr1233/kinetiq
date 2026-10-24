import { EyeIcon, EyeOffIcon } from 'lucide-react'
import { useState, type ComponentProps } from 'react'
import { useTranslation } from 'react-i18next'
import {
  InputGroup,
  InputGroupAddon,
  InputGroupButton,
  InputGroupInput,
} from '@/components/ui/input-group'

type PasswordInputProps = Omit<ComponentProps<typeof InputGroupInput>, 'type'>

/** A touch-sized password field whose show/hide button has an accessible name. */
export function PasswordInput(props: PasswordInputProps) {
  const { t } = useTranslation()
  const [shown, setShown] = useState(false)

  return (
    <InputGroup className="h-(--control-h-touch)">
      <InputGroupInput
        type={shown ? 'text' : 'password'}
        maxLength={128}
        className="h-full text-base md:text-base"
        {...props}
      />
      <InputGroupAddon align="inline-end">
        <InputGroupButton
          size="icon-sm"
          className="size-11"
          aria-label={t(
            shown
              ? 'account.signUp.hidePassword'
              : 'account.signUp.showPassword',
          )}
          onClick={() => setShown((value) => !value)}
        >
          {shown ? <EyeOffIcon /> : <EyeIcon />}
        </InputGroupButton>
      </InputGroupAddon>
    </InputGroup>
  )
}
