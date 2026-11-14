import {
  ArrowRightIcon,
  CheckIcon,
  EyeOffIcon,
  InfoIcon,
  LaptopIcon,
  ListChecksIcon,
  SendIcon,
  ShieldCheckIcon,
  SmartphoneIcon,
  Trash2Icon,
  TriangleAlertIcon,
  UserPlusIcon,
  WifiOffIcon,
  type LucideIcon,
} from 'lucide-react'
import { useEffect, useState, type ReactNode } from 'react'
import { useTranslation } from 'react-i18next'
import { Link } from 'react-router'
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert'
import { Avatar, AvatarFallback } from '@/components/ui/avatar'
import { Badge } from '@/components/ui/badge'
import { Button, buttonVariants } from '@/components/ui/button'
import {
  Card,
  CardAction,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Progress } from '@/components/ui/progress'
import { Separator } from '@/components/ui/separator'
import { SettingToggleGroup } from '@/features/account/components/setting-toggle-group'
import { THEMES, useTheme } from '@/lib/theme'
import { cn } from '@/lib/utils'

const WRAP = 'mx-auto w-full max-w-[1200px] px-4 md:px-8'
const SECTION = cn(WRAP, 'border-t py-16 md:py-24')
const LABEL = 'text-muted-foreground font-mono text-xs tracking-wider uppercase'
const H2 = 'text-3xl font-semibold tracking-tight text-balance md:text-4xl'
// the hero's parts enter one after another; with reduced motion only the fade is left
const ENTER =
  'animate-in fade-in motion-safe:slide-in-from-bottom-3 fill-mode-both duration-200 ease-out'

const STEP_ICONS = [UserPlusIcon, ListChecksIcon, SendIcon]
const FACT_ICONS = [ShieldCheckIcon, EyeOffIcon, Trash2Icon]
const CHECK_IN = ['sleep', 'soreness', 'stress'] as const
const ANSWERS = [5, 2, 2]
const SCORE = 83

/** The public start page: what Kinetiq does for a trainer and their clients, and the way to sign up. */
export function HomePage() {
  const { t } = useTranslation()

  return (
    <div className="bg-background flex min-h-svh flex-col">
      <header className="border-b">
        <div
          className={cn(WRAP, 'flex h-16 items-center justify-between gap-4')}
        >
          <BrandMark />
          <Button asChild variant="ghost" size="lg">
            <Link to="/sign-in">{t('home.signIn')}</Link>
          </Button>
        </div>
      </header>

      <main className="flex-1">
        <section
          aria-labelledby="home-title"
          className={cn(
            WRAP,
            'grid gap-10 pt-10 pb-16 lg:grid-cols-[minmax(0,5fr)_minmax(0,7fr)] lg:items-center lg:gap-12 lg:pt-18 lg:pb-24',
          )}
        >
          <div className="flex max-w-[560px] flex-col items-start gap-5">
            <span className={cn(LABEL, ENTER)}>{t('home.label')}</span>
            <h1
              id="home-title"
              className={cn(
                'text-display-sm md:text-display font-semibold tracking-tight text-balance',
                ENTER,
                'delay-80',
              )}
            >
              {t('home.title')}{' '}
              <span className="text-muted-foreground">
                {t('home.titleMuted')}
              </span>
            </h1>
            <p
              className={cn(
                'text-muted-foreground max-w-[480px] text-base md:text-lg',
                ENTER,
                'delay-160',
              )}
            >
              {t('home.lead')}
            </p>
            <SignUp className={cn(ENTER, 'delay-240')} />
          </div>
          <HeroVisual className={cn(ENTER, 'delay-320')} />
        </section>

        <HowItWorks />
        <TwoSides />
        <PlanChanges />
        <Closing />
      </main>

      <Footer />
    </div>
  )
}

function BrandMark({ small = false }: { small?: boolean }) {
  return (
    <p
      className={cn(
        'flex items-center gap-2.5 font-semibold tracking-tight',
        small ? 'text-sm' : 'text-base',
      )}
    >
      <span
        aria-hidden="true"
        className={cn(
          'bg-brand text-brand-foreground grid place-items-center rounded-lg font-mono',
          small ? 'size-6 text-xs' : 'size-7 text-sm',
        )}
      >
        K
      </span>
      Kinetiq
    </p>
  )
}

function SignUp({ className }: { className?: string }) {
  const { t } = useTranslation()
  return (
    <div
      className={cn('mt-1 flex w-full flex-col items-start gap-2.5', className)}
    >
      <Button asChild variant="brand" size="touch" className="w-full sm:w-auto">
        <Link to="/sign-up">
          {t('home.createAccount')}
          <ArrowRightIcon data-icon="inline-end" aria-hidden />
        </Link>
      </Button>
      <p className="text-muted-foreground text-sm">{t('home.free')}</p>
    </div>
  )
}

/** A picture made of the app's own components: described once, out of the tab order and the accessibility tree. */
function Mockup({
  label,
  className,
  children,
}: {
  label: string
  className?: string
  children: ReactNode
}) {
  return (
    <figure role="img" aria-label={label} className={cn('m-0', className)}>
      <div inert className="contents">
        {children}
      </div>
    </figure>
  )
}

function HeroVisual({ className }: { className?: string }) {
  const { t } = useTranslation()
  return (
    <Mockup
      label={t('home.heroVisual')}
      className={cn(
        'bg-muted relative h-[480px] overflow-hidden rounded-xl lg:h-[560px]',
        className,
      )}
    >
      <Card className="absolute top-8 left-6 hidden w-[calc(100%-200px)] lg:flex">
        <CardHeader>
          <p className={LABEL}>{t('home.desk.date')}</p>
          <CardTitle className="text-2xl font-semibold">
            {t('home.desk.title')}
          </CardTitle>
          <CardAction>
            <Badge variant="outline">{t('home.desk.weeks')}</Badge>
          </CardAction>
        </CardHeader>
        <CardContent className="gap-3.5">
          <div className="grid grid-cols-3 gap-2.5">
            <Tile label={t('home.desk.active')} value="14" />
            <Tile
              label={t('home.desk.done')}
              value="23"
              after={t('home.desk.of', { total: 38 })}
            />
            <Tile label={t('home.desk.waiting')} value="4" />
          </div>
          <p className="font-medium">{t('home.desk.attention')}</p>
          <Alert variant="warning" role="none">
            <TriangleAlertIcon />
            <AlertTitle>{t('home.desk.rpeTitle')}</AlertTitle>
            <AlertDescription>{t('home.desk.rpeText')}</AlertDescription>
          </Alert>
          <Alert variant="info" role="none">
            <InfoIcon />
            <AlertTitle>{t('home.desk.draftTitle')}</AlertTitle>
            <AlertDescription>{t('home.desk.draftText')}</AlertDescription>
          </Alert>
        </CardContent>
      </Card>

      <div className="bg-card absolute inset-x-0 top-8 mx-auto h-[600px] w-72 rounded-4xl border p-2 shadow-xl lg:top-24 lg:right-6 lg:left-auto lg:mx-0">
        <div className="bg-background flex h-full flex-col gap-2.5 overflow-hidden rounded-3xl border px-3 py-4">
          <div>
            <p className={LABEL}>{t('home.phone.date')}</p>
            <p className="text-2xl font-semibold tracking-tight">
              {t('home.phone.title')}
            </p>
          </div>
          <PhoneCheckIn />
          <Card size="sm">
            <CardHeader>
              <CardTitle>{t('home.phone.session')}</CardTitle>
              <CardAction className="text-muted-foreground text-sm">
                {t('home.phone.duration')}
              </CardAction>
            </CardHeader>
            <CardContent className="gap-2">
              <Target
                name={t('home.phone.squat')}
                value={t('home.phone.squatTarget')}
              />
              <Target
                name={t('home.phone.deadlift')}
                value={t('home.phone.deadliftTarget')}
              />
            </CardContent>
          </Card>
        </div>
      </div>
    </Mockup>
  )
}

function Tile({
  label,
  value,
  after,
}: {
  label: string
  value: string
  after?: string
}) {
  return (
    <Card size="sm" className="gap-1">
      <CardContent className="gap-1">
        <p className="text-muted-foreground text-xs">{label}</p>
        <p className="num text-metric-sm font-semibold">
          {value}
          {after && (
            <span className="text-muted-foreground ml-1 text-sm font-normal">
              {after}
            </span>
          )}
        </p>
      </CardContent>
    </Card>
  )
}

function Target({ name, value }: { name: string; value: string }) {
  return (
    <div className="flex items-center justify-between gap-2 text-sm">
      <span className="truncate">{name}</span>
      <span className="num text-muted-foreground shrink-0">{value}</span>
    </div>
  )
}

/** The check-in fills one answer at a time, then the readiness counts up once; with reduced motion it starts done. */
function useCheckInDemo() {
  const [still] = useState(
    () => window.matchMedia?.('(prefers-reduced-motion: reduce)').matches,
  )
  const [step, setStep] = useState(still ? 3 : 0)
  const [score, setScore] = useState(still ? SCORE : 0)

  useEffect(() => {
    if (still) return
    const timers = [500, 750, 1000].map((delay, index) =>
      setTimeout(() => setStep(index + 1), delay),
    )
    let frame = 0
    const count = setTimeout(() => {
      const start = performance.now()
      const tick = (now: number) => {
        const progress = Math.min(1, (now - start) / 600)
        setScore(Math.round(SCORE * (1 - (1 - progress) ** 3)))
        if (progress < 1) frame = requestAnimationFrame(tick)
      }
      frame = requestAnimationFrame(tick)
    }, 1050)
    return () => {
      timers.forEach(clearTimeout)
      clearTimeout(count)
      cancelAnimationFrame(frame)
    }
  }, [still])

  return { step, score }
}

function PhoneCheckIn() {
  const { t } = useTranslation()
  const { step, score } = useCheckInDemo()
  return (
    <Card size="sm">
      <CardHeader>
        <CardTitle>{t('readiness.checkIn.title')}</CardTitle>
      </CardHeader>
      <CardContent className="gap-2.5">
        {CHECK_IN.map((question, row) => (
          <div key={question} className="flex flex-col gap-1.5">
            <div className="flex items-baseline justify-between gap-2 text-xs">
              <span className="font-medium">
                {t(`readiness.checkIn.${question}.label`)}
              </span>
              <span className="text-muted-foreground text-right">
                {t(`readiness.checkIn.${question}.scale`)}
              </span>
            </div>
            <div className="grid grid-cols-5 gap-1.5">
              {[1, 2, 3, 4, 5].map((value) => (
                <span
                  key={value}
                  className={cn(
                    'num grid h-9 place-items-center rounded-md border text-sm transition-colors duration-200',
                    step > row &&
                      ANSWERS[row] === value &&
                      'bg-primary text-primary-foreground border-primary',
                  )}
                >
                  {value}
                </span>
              ))}
            </div>
          </div>
        ))}
        {step >= 3 && (
          <div
            className={cn(
              'bg-ready-high-muted flex items-center gap-3 rounded-lg px-3 py-2.5',
              ENTER,
            )}
          >
            <span className="bg-ready-high size-3 shrink-0 rounded-full" />
            <span className="num text-metric font-semibold">{score}</span>
            <div className="flex min-w-0 flex-col">
              <span className="font-semibold">
                {t('readiness.card.band.READY.name')}
              </span>
              <span className="text-xs">
                {t('readiness.card.band.READY.line')}
              </span>
            </div>
          </div>
        )}
      </CardContent>
    </Card>
  )
}

function SectionHead({
  id,
  label,
  title,
  children,
  className,
}: {
  id: string
  label: string
  title: string
  children?: ReactNode
  className?: string
}) {
  return (
    <div
      className={cn(
        'reveal mb-8 flex max-w-[640px] flex-col gap-3 md:mb-12',
        className,
      )}
    >
      <span className={LABEL}>{label}</span>
      <h2 id={id} className={H2}>
        {title}
      </h2>
      {children}
    </div>
  )
}

function HowItWorks() {
  const { t } = useTranslation()
  const steps = t('home.how.steps', { returnObjects: true }) as {
    title: string
    text: string
  }[]
  return (
    <section aria-labelledby="home-how" className={SECTION}>
      <SectionHead
        id="home-how"
        label={t('home.how.label')}
        title={t('home.how.title')}
      />
      <ol className="grid gap-4 md:grid-cols-3">
        {steps.map((step, index) => {
          const Icon = STEP_ICONS[index]
          return (
            <li key={step.title} className="reveal flex">
              <Card className="w-full">
                <CardHeader className="gap-3">
                  <div className="flex items-center justify-between">
                    <span className="bg-muted grid size-10 place-items-center rounded-lg">
                      <Icon className="size-5" aria-hidden />
                    </span>
                    <span className={LABEL} aria-hidden>
                      {String(index + 1).padStart(2, '0')}
                    </span>
                  </div>
                  <CardTitle>
                    <h3 className="text-lg font-semibold">{step.title}</h3>
                  </CardTitle>
                  <CardDescription className="text-base">
                    {step.text}
                  </CardDescription>
                </CardHeader>
              </Card>
            </li>
          )
        })}
      </ol>
    </section>
  )
}

function CheckList({ items }: { items: string[] }) {
  return (
    <ul className="flex flex-col gap-2.5">
      {items.map((item) => (
        <li key={item} className="grid grid-cols-[1.25rem_1fr] gap-2.5">
          <CheckIcon className="mt-px size-5" aria-hidden />
          <span>{item}</span>
        </li>
      ))}
    </ul>
  )
}

function Side({
  side,
  icon: Icon,
  children,
}: {
  side: 'desk' | 'phone'
  icon: LucideIcon
  children: ReactNode
}) {
  const { t } = useTranslation()
  const id = `home-side-${side}`
  return (
    <Card
      role="article"
      aria-labelledby={id}
      className="reveal [--card-pad:--spacing(6)]"
    >
      <CardHeader className="gap-4">
        <span className={cn(LABEL, 'flex items-center gap-2')}>
          <Icon className="size-4" aria-hidden />
          {t(`home.sides.${side}.label`)}
        </span>
        <div className="flex flex-col gap-1">
          <CardTitle>
            <h3 id={id} className="text-lg font-semibold">
              {t(`home.sides.${side}.title`)}
            </h3>
          </CardTitle>
          <CardDescription className="text-base">
            {t(`home.sides.${side}.text`)}
          </CardDescription>
        </div>
      </CardHeader>
      <CardContent className="gap-5">
        <CheckList
          items={
            t(`home.sides.${side}.items`, { returnObjects: true }) as string[]
          }
        />
        <div
          inert
          aria-hidden
          className="bg-background flex flex-col gap-2 rounded-lg border p-3"
        >
          {children}
        </div>
      </CardContent>
    </Card>
  )
}

const DESK_ROWS = [
  { initials: 'AK', name: 'Anna Kowalska', done: 11, status: 'WATCH' },
  { initials: 'PZ', name: 'Piotr Zieliński', done: 7, status: 'AT_RISK' },
  { initials: 'TL', name: 'Tomasz Lewandowski', done: 12, status: 'ON_TRACK' },
] as const

const STATUS_VARIANT = {
  WATCH: 'warning',
  AT_RISK: 'destructive',
  ON_TRACK: 'secondary',
} as const

function TwoSides() {
  const { t } = useTranslation()
  return (
    <section aria-labelledby="home-sides" className={SECTION}>
      <SectionHead
        id="home-sides"
        label={t('home.sides.label')}
        title={t('home.sides.title')}
      />
      <div className="grid gap-4 lg:grid-cols-2 lg:gap-6">
        <Side side="desk" icon={LaptopIcon}>
          {DESK_ROWS.map((row, index) => (
            <div key={row.name} className="flex flex-col gap-2">
              {index > 0 && <Separator />}
              <div className="flex min-h-9 flex-wrap items-center justify-between gap-x-3 gap-y-1 text-sm">
                <span className="flex min-w-0 items-center gap-2.5">
                  <Avatar>
                    <AvatarFallback className="text-xs">
                      {row.initials}
                    </AvatarFallback>
                  </Avatar>
                  <span>{row.name}</span>
                </span>
                <span className="flex shrink-0 items-center gap-2.5">
                  <Progress
                    value={(row.done / 12) * 100}
                    className="hidden w-16 sm:block"
                  />
                  <span className="num">{row.done}/12</span>
                  <Badge variant={STATUS_VARIANT[row.status]}>
                    {t(`overview.status.${row.status}`)}
                  </Badge>
                </span>
              </div>
            </div>
          ))}
        </Side>
        <Side side="phone" icon={SmartphoneIcon}>
          <Alert variant="info" role="none">
            <WifiOffIcon />
            <AlertDescription>
              {t('sessionlog.offline.announce')}
            </AlertDescription>
          </Alert>
          <div className="flex items-center justify-between gap-2">
            <span className="font-medium">{t('home.phone.squat')}</span>
            <Badge variant="outline">{t('home.sides.phone.saved')}</Badge>
          </div>
          <SetRow index={1} done />
          <SetRow index={2} />
        </Side>
      </div>
    </section>
  )
}

function SetRow({ index, done = false }: { index: number; done?: boolean }) {
  return (
    <div className="grid grid-cols-[1.5rem_repeat(3,minmax(0,1fr))_3rem] items-center gap-2">
      <span className="num text-muted-foreground text-center text-sm">
        {index}
      </span>
      {['100', '5', '8'].map((value) => (
        <span
          key={value}
          className={cn(
            'num grid h-12 place-items-center rounded-md border text-base',
            !done && 'text-muted-foreground',
          )}
        >
          {value}
        </span>
      ))}
      <span
        className={cn(
          'grid size-12 place-items-center rounded-md border',
          done && 'bg-primary text-primary-foreground border-primary',
        )}
      >
        <CheckIcon className="size-5" />
      </span>
    </div>
  )
}

const DIFF = [
  { name: 'squat', was: '4 × 8', now: '4 × 7', delta: '−13%' },
  { name: 'legPress', was: '3 × 12', now: '3 × 11', delta: '−8%' },
  { name: 'legCurl', was: '3 × 12', now: '3 × 11', delta: '−8%' },
] as const

function PlanChanges() {
  const { t } = useTranslation()
  return (
    <section aria-labelledby="home-plan-changes" className={SECTION}>
      <div className="grid gap-8 lg:grid-cols-[minmax(0,5fr)_minmax(0,7fr)] lg:items-center lg:gap-16">
        <SectionHead
          id="home-plan-changes"
          label={t('home.planChange.label')}
          title={t('home.planChange.title')}
          className="mb-0 md:mb-0"
        >
          <p className="text-muted-foreground text-base">
            {t('home.planChange.text')}
          </p>
        </SectionHead>
        <Mockup label={t('home.planChange.visual')} className="reveal">
          <Card>
            <CardHeader>
              <p className={LABEL}>Anna Kowalska</p>
              <CardAction>
                <Badge variant="info">{t('home.planChange.waiting')}</Badge>
              </CardAction>
            </CardHeader>
            <CardContent className="gap-4">
              <div className="flex flex-col gap-0.5">
                <p className="text-lg font-semibold">
                  {t('home.planChange.change')}
                </p>
                <p className="text-muted-foreground">
                  {t('home.planChange.reason')}
                </p>
              </div>
              <div className="grid grid-cols-[minmax(0,1fr)_auto_auto_auto] items-baseline gap-x-4 gap-y-2">
                {DIFF.map((row) => (
                  <div key={row.name} className="contents">
                    <span>{t(`home.planChange.${row.name}`)}</span>
                    <span className="num text-muted-foreground line-through">
                      {row.was}
                    </span>
                    <span className="num font-medium">→ {row.now}</span>
                    <span className="num text-muted-foreground text-sm">
                      {row.delta}
                    </span>
                  </div>
                ))}
              </div>
              <Separator />
              <div className="flex flex-wrap gap-x-6 gap-y-2 text-sm">
                <span className="flex items-center gap-2">
                  <Avatar>
                    <AvatarFallback className="text-xs">MN</AvatarFallback>
                  </Avatar>
                  <CheckIcon className="text-success size-4" />
                  {t('home.planChange.accepted')}
                </span>
                <span className="text-muted-foreground flex items-center gap-2">
                  <Avatar>
                    <AvatarFallback className="text-xs">AK</AvatarFallback>
                  </Avatar>
                  {t('home.planChange.deciding')}
                </span>
              </div>
              <div className="flex flex-wrap items-center justify-between gap-3">
                <span className="text-muted-foreground text-sm">
                  {t('home.planChange.effect')}
                </span>
                <span className="flex flex-wrap gap-2">
                  <span
                    className={buttonVariants({
                      variant: 'outline',
                      size: 'sm',
                    })}
                  >
                    {t('home.planChange.keep')}
                  </span>
                  <span className={buttonVariants({ size: 'sm' })}>
                    {t('home.planChange.accept')}
                  </span>
                </span>
              </div>
            </CardContent>
          </Card>
        </Mockup>
      </div>
    </section>
  )
}

function Closing() {
  const { t } = useTranslation()
  const facts = t('home.close.facts', { returnObjects: true }) as string[]
  return (
    <section aria-labelledby="home-close" className={SECTION}>
      <div className="grid gap-8 lg:grid-cols-[minmax(0,5fr)_minmax(0,7fr)] lg:items-center lg:gap-16">
        <div className="reveal flex max-w-[560px] flex-col items-start gap-5">
          <span className={LABEL}>{t('home.close.label')}</span>
          <h2 id="home-close" className={H2}>
            {t('home.close.title')}
          </h2>
          <p className="text-muted-foreground max-w-[480px] text-base md:text-lg">
            {t('home.close.lead')}
          </p>
          <SignUp />
        </div>
        <Mockup
          label={t('home.close.visual')}
          className="reveal bg-muted relative h-[400px] overflow-hidden rounded-xl lg:h-[440px]"
        >
          <Card className="absolute inset-x-4 top-6 lg:top-10 lg:right-auto lg:left-8 lg:w-[360px]">
            <CardHeader>
              <CardTitle>{t('home.close.invite')}</CardTitle>
              <CardDescription>{t('home.close.inviteText')}</CardDescription>
            </CardHeader>
            <CardContent className="gap-3">
              <div className="flex flex-col gap-2">
                <span className="text-sm font-medium">
                  {t('home.close.email')}
                </span>
                <Input
                  readOnly
                  tabIndex={-1}
                  aria-hidden
                  value="anna.kowalska@example.com"
                />
              </div>
              <span className={cn(buttonVariants(), 'w-full')}>
                {t('home.close.send')}
              </span>
            </CardContent>
          </Card>
          <Card
            size="sm"
            className="absolute top-[244px] right-4 left-10 shadow-xl lg:top-[252px] lg:right-8 lg:left-auto lg:w-[340px]"
          >
            <CardHeader>
              <CardTitle>{t('home.close.open')}</CardTitle>
              <CardAction>
                <Badge variant="secondary" className="num">
                  2
                </Badge>
              </CardAction>
            </CardHeader>
            <CardContent className="gap-3 text-sm">
              <Invite
                email="anna.kowalska@example.com"
                badge={
                  <Badge variant="outline">{t('home.close.inThreeDays')}</Badge>
                }
              />
              <Separator />
              <Invite
                email="jakub.kaminski@example.com"
                badge={
                  <Badge variant="warning">{t('home.close.inOneDay')}</Badge>
                }
              />
            </CardContent>
          </Card>
        </Mockup>
      </div>
      <div className="reveal mt-12 flex flex-col gap-4 border-t pt-6">
        <ul
          aria-label={t('home.close.factsLabel')}
          className="text-muted-foreground grid gap-4 md:grid-cols-3"
        >
          {facts.map((fact, index) => {
            const Icon = FACT_ICONS[index]
            return (
              <li key={fact} className="grid grid-cols-[1.25rem_1fr] gap-2.5">
                <Icon className="mt-px size-5" aria-hidden />
                <span>{fact}</span>
              </li>
            )
          })}
        </ul>
        <p className="text-muted-foreground text-sm">
          <Link
            to="/privacy"
            className="text-foreground underline underline-offset-3"
          >
            {t('home.close.privacy')}
          </Link>
        </p>
      </div>
    </section>
  )
}

function Invite({ email, badge }: { email: string; badge: ReactNode }) {
  return (
    <div className="flex items-center justify-between gap-3">
      <span className="text-muted-foreground min-w-0 truncate">{email}</span>
      {badge}
    </div>
  )
}

function Footer() {
  const { t, i18n } = useTranslation()
  const { theme, setTheme } = useTheme()
  return (
    <footer className="border-t">
      <div
        className={cn(
          WRAP,
          'flex flex-wrap items-center justify-between gap-x-8 gap-y-5 pt-8 pb-12',
        )}
      >
        <div className="flex items-center gap-4">
          <BrandMark small />
          <Button asChild variant="link" size="sm" className="px-0">
            <Link to="/privacy">{t('home.footer.privacy')}</Link>
          </Button>
        </div>
        <div className="flex flex-wrap items-center gap-x-6 gap-y-4">
          <div className="flex items-center gap-2">
            <span id="home-language" className="text-muted-foreground text-sm">
              {t('language.label')}
            </span>
            <SettingToggleGroup
              aria-labelledby="home-language"
              value={i18n.resolvedLanguage}
              onValueChange={(language) => void i18n.changeLanguage(language)}
              options={(['en', 'pl'] as const).map((language) => ({
                value: language,
                label: t(`language.${language}`),
              }))}
            />
          </div>
          <div className="flex items-center gap-2">
            <span id="home-theme" className="text-muted-foreground text-sm">
              {t('theme.label')}
            </span>
            <SettingToggleGroup
              aria-labelledby="home-theme"
              value={theme}
              onValueChange={(next) =>
                setTheme(next as (typeof THEMES)[number])
              }
              options={THEMES.map((option) => ({
                value: option,
                label: t(`theme.${option}`),
              }))}
            />
          </div>
        </div>
      </div>
    </footer>
  )
}
