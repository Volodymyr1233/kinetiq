import { useEffect, useRef, type ReactNode } from 'react'
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from '@/components/ui/card'
import { cn } from '@/lib/utils'

/** Lays out an account page as one centred card under the brand mark, from 320px up. */
export function AuthLayout({ children }: { children: ReactNode }) {
  return (
    <div className="bg-background flex min-h-svh flex-col items-center px-4 pt-8 pb-12">
      <p className="flex items-center gap-2.5 pb-6 text-base font-semibold tracking-tight">
        <span
          aria-hidden="true"
          className="bg-brand text-brand-foreground grid size-7 place-items-center rounded-lg font-mono text-sm"
        >
          K
        </span>
        Kinetiq
      </p>
      {/* a long name or email without spaces wraps instead of widening the page */}
      <main className="flex w-full max-w-[400px] flex-col gap-4 wrap-anywhere">
        {children}
      </main>
    </div>
  )
}

export function AuthFooter({ children }: { children: ReactNode }) {
  return (
    <p className="text-muted-foreground text-center text-xs [&_a]:underline [&_a]:underline-offset-3">
      {children}
    </p>
  )
}

type AuthCardProps = {
  title: string
  description?: ReactNode
  icon?: ReactNode
  iconClassName?: string
  // after a state change, keyboard and screen reader users continue from the new heading
  focusHeading?: boolean
  children?: ReactNode
}

export function AuthCard({
  title,
  description,
  icon,
  iconClassName,
  focusHeading = false,
  children,
}: AuthCardProps) {
  const heading = useRef<HTMLHeadingElement>(null)

  useEffect(() => {
    if (focusHeading) heading.current?.focus()
  }, [focusHeading])

  return (
    <Card className="gap-5">
      <CardHeader className="gap-1">
        {icon && (
          <span
            className={cn(
              'bg-muted text-foreground mb-4 grid size-11 place-items-center rounded-full',
              iconClassName,
            )}
          >
            {icon}
          </span>
        )}
        <CardTitle className="text-2xl leading-8 font-semibold tracking-tight">
          <h1 ref={heading} tabIndex={-1} className="outline-none">
            {title}
          </h1>
        </CardTitle>
        {description && <CardDescription>{description}</CardDescription>}
      </CardHeader>
      {children && <CardContent className="gap-5">{children}</CardContent>}
    </Card>
  )
}
