import { Avatar, AvatarFallback } from '@/components/ui/avatar'

// first letters of the first and last word, as on the boards ("Marek Nowak" → "MN")
function initials(name: string) {
  const words = name.trim().split(/\s+/).filter(Boolean)
  const letters =
    words.length > 1 ? [words[0], words[words.length - 1]] : words.slice(0, 1)
  return letters.map((word) => word[0]!.toLocaleUpperCase()).join('')
}

/** A person's initials; the name is shown beside it, so the avatar itself is hidden from screen readers. */
export function AccountAvatar({
  name,
  size,
}: {
  name: string
  size?: 'default' | 'lg' | 'xl'
}) {
  return (
    <Avatar size={size} aria-hidden="true">
      <AvatarFallback className="text-foreground font-semibold">
        {initials(name)}
      </AvatarFallback>
    </Avatar>
  )
}
