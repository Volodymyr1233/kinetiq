import { Children, type ReactNode } from 'react'
import { cn } from '@/lib/utils'

/**
 * Values of one line, such as a set's targets, apart by middle dots as the design system writes them
 * ("3 × 5 · 100 kg · RPE 8"). The dots are hidden from assistive technology, which reads the values as they are.
 */
export function ValueList({
  children,
  className,
}: {
  children: ReactNode
  className?: string
}) {
  return (
    // every value leads with its dot; the list starts one dot to the left and clips it, so no line begins with a dot
    <span className={cn('block overflow-hidden', className)}>
      <span className="-ml-4.5 flex flex-wrap gap-y-0.5">
        {Children.toArray(children).map((value, index) => (
          <span key={index} className="flex">
            <span aria-hidden className="w-4.5 shrink-0 text-center">
              ·
            </span>
            {value}
          </span>
        ))}
      </span>
    </span>
  )
}
