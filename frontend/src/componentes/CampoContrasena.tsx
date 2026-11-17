import { useState, type ComponentProps } from 'react'
import Campo from './Campo'

/** Campo de contraseña con casilla "Mostrar contraseña" (sin autocompletar el token ni guardar nada). */
export default function CampoContrasena(props: ComponentProps<typeof Campo>) {
  const [visible, setVisible] = useState(false)
  return (
    <div className="space-y-1">
      <Campo {...props} type={visible ? 'text' : 'password'} />
      <label className="flex min-h-6 items-center gap-2 text-sm text-slate-600 dark:text-slate-400">
        <input type="checkbox" checked={visible} onChange={(e) => setVisible(e.target.checked)} />
        Mostrar contraseña
      </label>
    </div>
  )
}
