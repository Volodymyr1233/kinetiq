import { createBrowserRouter } from 'react-router'
import { HomePage } from '@/app/home-page'

// Route groups /trainer/* and /app/* are added with their features, lazy-loaded.
export const router = createBrowserRouter([{ path: '/', Component: HomePage }])
