import clsx from 'clsx'
import type { ReactNode } from 'react'

export default function Tarjeta({
  children,
  className,
}: {
  children: ReactNode
  className?: string
}) {
  return (
    <div className={clsx('rounded-xl bg-white p-4 shadow-sm dark:bg-slate-900', className)}>
      {children}
    </div>
  )
}
