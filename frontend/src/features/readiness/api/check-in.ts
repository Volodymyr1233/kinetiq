import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from '@/lib/api/client'
import { problemCode } from '@/lib/api/problem'
import { SESSION_LOGS_KEY } from '@/lib/query-keys'
import type { components } from '@/lib/api/schema'

export type TodayCheckIn = components['schemas']['TodayCheckInResponse']
export type CheckIn = components['schemas']['CheckInResponse']
export type Adjustment = components['schemas']['AdjustmentResponse']
export type Answers = components['schemas']['SaveCheckInRequest']

const checkInKey = ['check-in', 'today']

/** The signed-in client's check-in today, if any, and whether one can still be saved. */
export function useCheckIn() {
  return useQuery({
    queryKey: checkInKey,
    queryFn: async () => {
      const { data, error } = await api.GET('/api/v1/check-ins/today')
      // openapi-fetch returns errors instead of throwing; throw the problem detail so the query fails with its code
      if (error !== undefined) throw error
      return data
    },
  })
}

/** Saves today's check-in, or replaces the one saved today; the answer becomes what Today shows. */
export function useSaveCheckIn() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: async (body: Answers) => {
      const { data, error } = await api.PUT('/api/v1/check-ins/today', {
        body,
      })
      if (error !== undefined) throw error
      return data
    },
    onSuccess: (data) => {
      queryClient.setQueryData(checkInKey, data)
      // different answers can change today's targets, which the logger shows
      void queryClient.invalidateQueries({ queryKey: SESSION_LOGS_KEY })
    },
    onError: (error) => {
      // a set was logged meanwhile: show what is stored, which can no longer be changed
      if (problemCode(error) === 'check-in.training-started') {
        void queryClient.invalidateQueries({ queryKey: checkInKey })
      }
    },
  })
}
