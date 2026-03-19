import { create } from 'zustand'
import { persist } from 'zustand/middleware'

export type ThemeId = 'default' | 'kieser' | 'coral' | 'slate'

export interface ThemeOption {
  id: ThemeId
  name: string
  description: string
  preview: { sidebar: string; accent: string; bg: string }
}

export const themes: ThemeOption[] = [
  {
    id: 'default',
    name: 'Fitagend',
    description: 'Fresh teal — the default look',
    preview: { sidebar: '#032922', accent: '#17b085', bg: '#edfcf6' },
  },
  {
    id: 'kieser',
    name: 'Kieser',
    description: 'Navy blue & gold — clinical precision',
    preview: { sidebar: '#0c182e', accent: '#b8963e', bg: '#f0f4fa' },
  },
  {
    id: 'coral',
    name: 'Coral',
    description: 'Warm coral — energetic fitness vibe',
    preview: { sidebar: '#340f0a', accent: '#e84e32', bg: '#fff1ed' },
  },
  {
    id: 'slate',
    name: 'Slate',
    description: 'Cool slate — modern & minimal',
    preview: { sidebar: '#020617', accent: '#6366f1', bg: '#f8fafc' },
  },
]

interface ThemeState {
  themeId: ThemeId
  setTheme: (id: ThemeId) => void
}

function applyTheme(id: ThemeId) {
  const root = document.documentElement
  root.classList.remove('theme-kieser', 'theme-coral', 'theme-slate')
  if (id !== 'default') {
    root.classList.add(`theme-${id}`)
  }
}

export const useThemeStore = create<ThemeState>()(
  persist(
    (set) => ({
      themeId: 'default',
      setTheme: (id: ThemeId) => {
        applyTheme(id)
        set({ themeId: id })
      },
    }),
    {
      name: 'gym-theme',
      onRehydrateStorage: () => {
        return (state: ThemeState | undefined) => {
          if (state?.themeId) {
            applyTheme(state.themeId)
          }
        }
      },
    },
  ),
)
