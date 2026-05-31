# Historia 6.2 — Ícono personalizado de la aplicación

**Fecha**: 2026-05-30  
**Estado**: Completa

## Qué se hizo

- Reemplazado el ícono genérico de Android Studio por la imagen provista por el usuario (`proyect/assets/icon_source.png`, 1024×1024 con canal alfa).
- Fondo del ícono adaptativo: color sólido `#1E293B`.
- Imagen del ícono escalada al **safe zone** (288×288 px) centrada en un canvas de 432×432 px (108dp a xxxhdpi).

## Archivos generados/modificados

| Archivo | Acción | Detalle |
|---------|--------|---------|
| `res/drawable-nodpi/ic_launcher_fg.png` | Creado | 432×432, ícono en safe zone, fondo transparente |
| `res/drawable/ic_launcher_foreground.xml` | Modificado | Bitmap drawable apuntando a `ic_launcher_fg` |
| `res/drawable/ic_launcher_background.xml` | Modificado | Color sólido `#1E293B` |
| `mipmap-mdpi/ic_launcher.webp` | Reemplazado | 48×48, fondo `#1E293B` + ícono al 80% |
| `mipmap-hdpi/ic_launcher.webp` | Reemplazado | 72×72 |
| `mipmap-xhdpi/ic_launcher.webp` | Reemplazado | 96×96 |
| `mipmap-xxhdpi/ic_launcher.webp` | Reemplazado | 144×144 |
| `mipmap-xxxhdpi/ic_launcher.webp` | Reemplazado | 192×192 |
| `mipmap-mdpi/ic_launcher_round.webp` | Reemplazado | 48×48, máscara circular + transparencia |
| `mipmap-hdpi/ic_launcher_round.webp` | Reemplazado | 72×72 |
| `mipmap-xhdpi/ic_launcher_round.webp` | Reemplazado | 96×96 |
| `mipmap-xxhdpi/ic_launcher_round.webp` | Reemplazado | 144×144 |
| `mipmap-xxxhdpi/ic_launcher_round.webp` | Reemplazado | 192×192 |

## Herramienta usada

ImageMagick 6.9 (preinstalado en el sistema).

## Notas

- Los íconos adaptativos (API 26+) usan background `#1E293B` + foreground PNG en safe zone → el sistema aplica la máscara del launcher (squircle, círculo, etc.) automáticamente.
- Los WebP legacy (Android < 8.0) tienen el ícono compuesto directamente con el fondo sólido.
- Los round WebP legacy tienen transparencia en las esquinas para launchers que piden ícono circular.
- La capa `monochrome` en `ic_launcher.xml` sigue apuntando al mismo foreground bitmap; funcional en Android 13+ aunque sin optimización de tintado vectorial.
- Build: `assembleDebug` → BUILD SUCCESSFUL.
