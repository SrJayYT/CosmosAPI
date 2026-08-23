# CosmosAPI

Plugin para Spigot que añade monedas virtuales configurables llamadas **cosmos**, recompensas automáticas, condiciones de retiro, menús de tienda, rankings holográficos e integración con PlaceholderAPI, TAB y EconomyShopGUI.

**Autor:** devpapo  
**Plataforma:** Spigot 1.21.x  
**Java:** 17  
**Build:** Maven (`pom.xml`)  
**Paquete base:** `com.devpapo.cosmosapi`

## Descarga

Descarga la última versión desde las [releases de GitHub](https://github.com/SrJayYT/CosmosAPI/releases).

## Características

### Cosmos y balances

- Crea múltiples monedas virtuales con un ID interno y nombre visual con colores.
- Balances persistentes en YAML (`players.yml`).
- Los saldos pueden ser positivos o negativos.
- Formato compacto automático para balances: `1334` → `1,33k`, `2435464` → `2,43M` y sus equivalentes negativos.
- Sufijos disponibles hasta decillón (`Dc`), dentro del límite numérico de Java `long`.

### Recompensas y condiciones

- Recompensas por tiempo conectado, matar jugadores, matar mobs, romper bloques y colocar bloques.
- Condiciones que retiran cosmos al ejecutar los mismos eventos compatibles.
- Protección opcional contra multicuentas: puede impedir recompensas y condiciones `PLAYER_KILL` entre jugadores con la misma IP.

### Menús, tiendas y rankings

- Menús de compra configurables desde `menus.yml`.
- Inventarios de cofre, dispensador, horno, soporte para pociones y tolva.
- Editor de artículos directamente dentro del juego.
- Vista de balances, directorio de menús y tops paginados.
- Hologramas de ranking mediante DecentHolograms.
- Los menús y hologramas se pueden activar o desactivar globalmente desde `config.yml`.

### Integraciones

- Placeholders para PlaceholderAPI y TAB.
- EconomyShopGUI puede cobrar y pagar con cualquier cosmo como moneda externa.

## Comandos

`/cosmos` funciona como alias de `/cosmo`.

### Jugadores

```text
/cosmo send <cosmo> <jugador> <cantidad>
/cosmo view
/cosmo menus [página]
/cosmo tops
/cosmo list
```

`/cosmo send` solo acepta cantidades positivas y no permite enviarte cosmos a ti mismo.

### Administración de cosmos

```text
/cosmo create <nombre> <tipo> <cantidad>
/cosmo create <nombre> TIME <cantidad> <intervalo> <minuto|dia|semana|mes|año>
/cosmo edit <cosmo> <displayname|type|reward|interval> <valor>
/cosmo displayname <cosmo> <nombre visible>
/cosmo delete <cosmo>
/cosmo give <cosmo> <jugador> <cantidad>
/cosmo giveall <cosmo> <cantidad>
/cosmo set <cosmo> <jugador> <cantidad>
/cosmo reload
```

`/cosmo give` y `/cosmo set` aceptan cantidades negativas. Ejemplos:

```text
/cosmo give pichula Jugador -10
/cosmo set pichula Jugador -50
```

`/cosmo giveall <cosmo> <cantidad>` entrega una cantidad positiva a todos los jugadores conectados, publica un anuncio global y reproduce una melodía para todos.

### Administración de menús

Los menús deben estar habilitados en `config.yml`.

```text
/cosmo menu create <nombre> <cofre|dispensador|horno|soporte|tolva> <tamaño>
/cosmo menu open <nombre>
/cosmo menu edit <nombre>
/cosmo menu item <menú> <slot> <cosmo> <precio>
/cosmo menu displayname <menú> <nombre>
/cosmo menu status
/cosmo menu delete <nombre>
```

### Administración de hologramas

Los hologramas deben estar habilitados en `config.yml` y requieren DecentHolograms.

```text
/cosmo hologram generate <id> <cosmo>
/cosmo hologram move <id>
/cosmo hologram title <id> <título>
/cosmo hologram delete <id>
/cosmo hologram list
```

### Condiciones

`/condition` funciona como alias de `/conditions`.

```text
/conditions create <id> <cosmo> <tipo> <cantidad>
/conditions list
/conditions info <id>
/conditions edit <id> <cosmo|type|amount> <valor>
/conditions delete <id>
/conditions reload
```

## Permisos

| Permiso | Descripción | Predeterminado |
| --- | --- | --- |
| `cosmos.admin` | Administra cosmos, saldos, menús y hologramas. | OP |
| `cosmos.send` | Envía cosmos a otros jugadores. | Sí |
| `cosmos.view` | Consulta menús, tops y saldos propios. | Sí |
| `cosmos.conditions` | Administra condiciones de retiro. | OP |

## Configuración

### `config.yml`

```yaml
memory-cleanup:
  interval-minutes: 30

anti-alt:
  # Impide recompensas y condiciones PLAYER_KILL entre cuentas con la misma IP.
  same-ip-player-kill: false

menus:
  enabled: false

holograms:
  enabled: true
```

- `anti-alt.same-ip-player-kill`: al activarlo, un asesinato entre jugadores con la misma IP no entrega la recompensa `PLAYER_KILL` ni ejecuta condiciones de ese tipo.
- `menus.enabled`: controla todas las interfaces, tiendas y comandos públicos de menús. Su valor predeterminado es `false`.
- `holograms.enabled`: controla los hologramas de ranking. Su valor predeterminado es `true`.
- Ejecuta `/cosmo reload` después de cambiar la configuración.

## Placeholders

Instala PlaceholderAPI. CosmosAPI registra sus placeholders automáticamente al iniciar; no necesitas descargar una expansión con `/papi ecloud`.

| Placeholder | Resultado |
| --- | --- |
| `%cosmos_<cosmo>%` | Saldo abreviado del jugador para ese cosmo. |
| `%cosmos_<cosmo>_displayname%` | Nombre visible y coloreado del cosmo. |
| `%cosmos_cosmos%` | IDs de todos los cosmos, separados por comas. |

El ID es el nombre interno asignado al crear el cosmo, no su nombre visible. Por ejemplo:

```text
/cosmo create gemas MOB_KILL 2
```

Usa los siguientes placeholders:

```text
%cosmos_gemas%
%cosmos_gemas_displayname%
```

El identificador anterior `%cosmosapi_<cosmo>%` sigue disponible para no romper configuraciones existentes, pero se recomienda usar `%cosmos_<cosmo>%`.

### Ejemplo para TAB

```yaml
scoreboards:
  default:
    title: '&d&lCOSMOS'
    lines:
      - '&fGemas: &d%cosmos_gemas%'
      - '&fMonedas: &6%cosmos_monedas%'
```

Puedes probar un placeholder desde el juego:

```text
/papi parse me %cosmos_gemas%
```

### Placeholders en `menus.yml`

Dentro de la vista de cosmos, usa `{cosmo-name}` únicamente para el nombre visual. Para el saldo del cosmo mostrado usa `%cosmos_<cosmo>%`:

```yaml
cosmo-item:
  name: '{cosmo-name}'
  lore:
    - '&7Saldo: &f%cosmos_<cosmo>%'
```

## EconomyShopGUI: cobrar con cosmos

Cada cosmo creado se registra como una moneda externa de EconomyShopGUI.

```text
EXTERNAL:CosmosAPI_<id-del-cosmo>
```

Por ejemplo, para un cosmo con ID `kills`:

```yaml
items:
  diamond_sword:
    material: DIAMOND_SWORD
    buy-price: 25
    sell-price: 10
    economy: EXTERNAL:CosmosAPI_kills
```

- Comprar la espada cuesta 25 cosmos de `kills`.
- Vender la espada entrega 10 cosmos de `kills`.
- No se utiliza dinero de Vault para ese objeto.

### Requisitos

1. Instala CosmosAPI y EconomyShopGUI en la carpeta `plugins`.
2. Crea el cosmo antes de configurarlo en EconomyShopGUI.
3. Reinicia el servidor para que EconomyShopGUI detecte la moneda externa.
4. Añade `economy: EXTERNAL:CosmosAPI_<id>` al artículo o sección correspondiente.

Los precios son enteros positivos. Si EconomyShopGUI proporciona un precio decimal, CosmosAPI lo redondea hacia arriba.

## Archivos

| Archivo | Descripción |
| --- | --- |
| `config.yml` | Ajustes generales, anti-multicuenta, menús y hologramas. |
| `cosmos.yml` | Definiciones de cosmos y hologramas. |
| `conditions.yml` | Condiciones que retiran cosmos. |
| `menus.yml` | Menús, tiendas y artículos. |
| `players.yml` | Balances de jugadores. |
| `messages.yml` | Mensajes editables del plugin. |

## Desarrollo

El proyecto se compila exclusivamente con **Maven** mediante `pom.xml`:

```bash
mvn package
```

El JAR generado se encontrará en `target/`. No se utiliza Gradle ni se requiere un archivo `build.gradle`.