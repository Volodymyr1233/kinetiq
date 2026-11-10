import { useOutletContext } from 'react-router'
import { ClientProgressSection } from '@/features/progress/components/client-progress-section'
import { ClientOverviewTab } from '@/features/sessionlog/pages/client-overview-tab'
import type { components } from '@/lib/api/schema'

type Client = components['schemas']['ClientResponse']

/** The client page's Overview tab, opened by the client's progress once their plan is published. */
export function ClientOverviewRoute() {
  const client = useOutletContext<Client>()

  return (
    <ClientOverviewTab
      progress={
        // only a published plan has progress; the cards below say when there is none
        client.planState === 'PUBLISHED' && (
          // a new section per client, so one client's figures never stand in while another's load
          <ClientProgressSection key={client.id} clientId={client.id} />
        )
      }
    />
  )
}
