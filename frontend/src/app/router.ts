import { createBrowserRouter } from 'react-router'
import { HomePage } from '@/app/home-page'

// lazy route groups /trainer/* and /app/* arrive with their features
export const router = createBrowserRouter([{ path: '/', Component: HomePage }])
