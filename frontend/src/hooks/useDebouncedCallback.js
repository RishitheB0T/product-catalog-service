// BUILD FLOW 12.14 — frontend/src/hooks/useDebouncedCallback.js
// First introduced: Phase 12
// Purpose: Wraps a function so that it runs only after the calls have paused for a while ("debouncing"). Typing
//          "linen" then sends one search instead of five.
// Depends on: React's useRef, useEffect, and useMemo.
// Used by: SearchBox (12.18) and FilterPanel (12.20).
// Runtime role: Timers in the browser; nothing else.

import { useEffect, useMemo, useRef } from 'react'

// BUILD FLOW 12.14.01 — A timer that every new call restarts
// Refs hold values that survive re-renders without causing one: the pending timer, and the latest callback (so a
// timer that fires runs the newest version, with the newest props). The returned function stays the same object
// for the component's whole life; .cancel() drops a pending call, which is used when an action makes it outdated.
export function useDebouncedCallback(callback, delayMs) {
  const latest = useRef(callback)
  const timer = useRef(undefined)

  useEffect(() => {
    latest.current = callback
  })

  // A pending call must not fire after the component is gone.
  useEffect(() => () => clearTimeout(timer.current), [])

  return useMemo(() => {
    const debounced = (...args) => {
      clearTimeout(timer.current)
      timer.current = setTimeout(() => latest.current(...args), delayMs)
    }
    debounced.cancel = () => clearTimeout(timer.current)
    return debounced
  }, [delayMs])
}
