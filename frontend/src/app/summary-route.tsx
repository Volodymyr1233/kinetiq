import { useQuery } from '@tanstack/react-query'
import { useParams } from 'react-router'
import { conversationsQuery } from '@/features/messaging/api/messaging'
import { SummaryPage } from '@/features/sessionlog/pages/summary-page'
import { firstName } from '@/lib/first-name'
import { NotFoundPage } from './not-found-page'

/** The session done screen of the session in the address, with the trainer's name from the client's one conversation. */
export function SessionSummaryRoute() {
  const { id = '' } = useParams()
  // a failed read leaves the screen saying "your trainer"
  const { data } = useQuery(conversationsQuery)
  const name = data?.conversations[0]?.name

  return (
    <SummaryPage
      key={id}
      sessionId={id}
      trainerName={name === undefined ? undefined : firstName(name)}
      notFound={<NotFoundPage />}
    />
  )
}
