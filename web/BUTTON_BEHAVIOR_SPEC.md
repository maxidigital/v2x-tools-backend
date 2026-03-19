# Especificación de Comportamiento de Botones - V2X.tools

## Estados Estándar para TODOS los Botones

### 1. Estado Normal (Enabled)
- Cada botón tiene su color base específico
- Texto siempre blanco
- Cursor: pointer

### 2. Estado Hover  
- **REGLA UNIVERSAL**: Siempre debe oscurecerse al pasar el mouse
- El color debe ser un tono más oscuro del color base
- Texto permanece blanco
- Nunca debe cambiar a blanco o color más claro

### 3. Estado Disabled
- **REGLA UNIVERSAL**: Siempre gris (#bdc3c7)
- Texto gris más claro
- Cursor: not-allowed
- Sin efecto hover

## Colores por Tipo de Botón

### Botones de ACCIÓN PRINCIPAL (Convert, Copy, Generate)
- **Base**: #3498db (celeste)
- **Hover**: #2980b9 (celeste oscuro)
- **Disabled**: #bdc3c7 (gris)

### Botones de TAMAÑO DE FUENTE (A-, A+)
- **Base**: #5DADE2 (celeste claro)
- **Hover**: #3498db (celeste estándar - más oscuro)
- **Disabled**: #bdc3c7 (gris)

### Botones de ELIMINACIÓN (Clear, Clear All)
- **Base**: #e74c3c (rojo)
- **Hover**: #c0392b (rojo oscuro)
- **Disabled**: #bdc3c7 (gris)

### Botones de EXPANDIR/VER (Open in new window)
- **Base**: #2ecc71 (verde)
- **Hover**: #27ae60 (verde oscuro)
- **Disabled**: #bdc3c7 (gris)

### Botones de COMPARTIR (Share via email)
- **Base**: #9c27b0 (púrpura)
- **Hover**: #7b1fa2 (púrpura oscuro)
- **Disabled**: #bdc3c7 (gris)

## Implementación CSS

Todos los hovers DEBEN usar `!important` para sobrescribir Tailwind:

```css
#botonId:hover {
  background-color: [COLOR_OSCURO] !important;
  color: white !important;
}
```

## REGLA DE ORO

**NUNCA un botón debe volverse más claro en hover. SIEMPRE debe oscurecerse.**