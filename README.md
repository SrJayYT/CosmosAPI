# CosmosAPI

CosmosAPI añade monedas virtuales configurables llamadas **cosmos**. Cada cosmo puede obtenerse automáticamente por acciones del juego, enviarse entre jugadores, usarse en los menús internos del plugin o como moneda de compra/venta en EconomyShopGUI.

| Dato | Valor |
| --- | --- |
| Versión | `1.0.3` |
| Plataforma | Spigot/Paper 1.21.x |
| Java | 21 |
| Autor | devpapo |
| Build | **Maven** (`pom.xml`) |
| Paquete base | `com.devpapo.cosmosapi` |

## Índice

1. [Instalación](#instalación)
2. [Conceptos básicos](#conceptos-básicos)
3. [Crear y administrar cosmos](#crear-y-administrar-cosmos)
4. [Activar y desactivar cosmos](#activar-y-desactivar-cosmos)
5. [Recompensas automáticas y condiciones](#recompensas-automáticas-y-condiciones)
6. [Comandos para jugadores](#comandos-para-jugadores)
7. [Menús internos](#menús-internos)
8. [Placeholders](#placeholders)
9. [Tiendas Cosmos por archivos](#tiendas-cosmos-por-archivos)
10. [EconomyShopGUI](#economyshopgui-721)
11. [PlayerKits2](#playerkits2)
12. [Hologramas](#hologramas)
13. [Permisos](#permisos)
14. [Configuración y archivos](#configuración-y-archivos)
15. [Desarrollo](#desarrollo)

## Instalación

1. Descarga el JAR desde las [releases de GitHub](https://github.com/SrJayYT/CosmosAPI/releases).
2. Coloca `CosmosAPI.jar` en la carpeta `plugins/` de tu servidor.
3. Inicia o reinicia el servidor para generar los archivos de configuración.
4. Crea tu primer cosmo con un usuario que tenga `cosmos.admin`.

```text
/cosmo create gemas MOB_KILL 2
/cosmo displayname gemas &dGemas
```

El primer comando crea el ID interno `gemas`. El segundo define el nombre que verán los jugadores: **Gemas** en color morado.

### Dependencias opcionales

| Plugin | Para qué sirve |
| --- | --- |
| PlaceholderAPI | Usar `%cosmos_...%` en otros plugins, TAB y menús. |
| EconomyShopGUI `7.2.1` | Comprar y vender usando cosmos como moneda. |
| PlayerKits2 | Cobrar cosmos al reclamar kits configurados. |
| DecentHolograms | Crear hologramas de rankings. |
| TAB | Mostrar los placeholders de CosmosAPI en scoreboards o tablist. |

CosmosAPI funciona sin estas dependencias; solo se desactivan las funciones correspondientes.

## Conceptos básicos

Un cosmo tiene:

- **ID interno:** se usa en comandos, archivos, placeholders y EconomyShopGUI. Solo acepta letras, números, `_` y `-`, entre 3 y 24 caracteres. Ejemplo: `gemas`.
- **Nombre visual:** texto con colores que se muestra al jugador. Ejemplo: `&dGemas`.
- **Tipo de recompensa:** evento que entrega el cosmo automáticamente.
- **Cantidad:** cosmos entregados por cada evento.

Los balances son números enteros `long`. Pueden quedar negativos por condiciones de retiro o por `/cosmo give` con una cantidad negativa. El formato visual es compacto: `1334` se muestra como `1,33k` y `2435464` como `2,43M`.

## Crear y administrar cosmos

`/cosmos` es un alias completo de `/cosmo`.

### Crear un cosmo

```text
/cosmo create <id> <tipo> <cantidad>
/cosmo create <id> TIME <cantidad> <intervalo> <unidad>
```

Ejemplos:

```text
# Entrega 2 Gemas por cada mob eliminado.
/cosmo create gemas MOB_KILL 2

# Entrega 5 Monedas por romper un bloque.
/cosmo create monedas BLOCK_BREAK 5

# Entrega 10 Tokens cada 30 minutos conectado.
/cosmo create tokens TIME 10 30 minutos

# Cambia el texto que ven los jugadores.
/cosmo displayname gemas &d&lGemas
/cosmo displayname monedas &6Monedas
/cosmo displayname tokens &bTokens
```

### Tipos de recompensa disponibles

| Tipo | Cuándo se entrega |
| --- | --- |
| `TIME` | Por tiempo conectado. Requiere intervalo y unidad. |
| `PLAYER_KILL` | Al matar a otro jugador. |
| `PLAYER_DEATH` | Al morir un jugador. |
| `BLOCK_BREAK` | Al romper un bloque. |
| `BLOCK_PLACE` | Al colocar un bloque. |
| `MOB_KILL` | Al matar una entidad que no es jugador. |
| `LIVING_ENTITY_KILL` | Al matar un jugador o una entidad viva. |
| `ALL_KILLS` | En cualquier muerte causada por un jugador. |
| `TAMED_ANIMAL_DEATH` | Cuando muere un animal domesticado por el jugador dueño. |

Para `TIME`, las unidades aceptadas son `minuto`, `dia`, `semana`, `mes` y `año`; también se aceptan las variantes en inglés y plural.

### Editar un cosmo existente

```text
/cosmo edit <cosmo> displayname <nombre visible>
/cosmo edit <cosmo> type <tipo>
/cosmo edit <cosmo> reward <cantidad>
/cosmo edit <cosmo> interval <cantidad> <unidad>
/cosmo displayname <cosmo> <nombre visible>
/cosmo delete <cosmo>
```

Ejemplos:

```text
# Cambia las Gemas para que se reciban al matar jugadores.
/cosmo edit gemas type PLAYER_KILL

# Ahora cada asesinato entrega 3 Gemas.
/cosmo edit gemas reward 3

# Configura Tokens a 15 cada hora. El plugin no tiene HOUR, usa 60 minutos.
/cosmo edit tokens interval 60 minutos

# Elimina el cosmo. Los saldos guardados no se muestran mientras no exista el cosmo.
/cosmo delete monedas
```

### Gestionar saldos como administrador

```text
/cosmo give <cosmo> <jugador> <cantidad>
/cosmo set <cosmo> <jugador> <cantidad>
/cosmo take <cosmo> <jugador> <cantidad|all>
/cosmo giveall <cosmo> <cantidad>
/cosmo takeall <cosmo> <cantidad|all>
/cosmo resetall <cosmo|all>
```

Ejemplos:

```text
# Suma 50 Gemas a Steve.
/cosmo give gemas Steve 50

# Resta 10 Gemas a Steve.
/cosmo give gemas Steve -10

# Establece exactamente 500 Gemas, incluso si antes tenía otro saldo.
/cosmo set gemas Steve 500

# Puede establecerse un saldo negativo.
/cosmo set gemas Steve -50

# Da 25 Tokens a todos los jugadores conectados y envía un anuncio global.
/cosmo giveall tokens 25

# Retira todo el saldo de Gemas a Steve.
/cosmo take gemas Steve all

# Retira todo el saldo de Tokens a todos los jugadores conectados.
/cosmo takeall tokens all
```

`giveall` solo acepta cantidades positivas. `/cosmo give` y `/cosmo set` aceptan números negativos. `take` y `takeall` admiten `all` para retirar el saldo completo sin que pueda quedar negativo.

### Reiniciar saldos masivamente

```text
/cosmo resetall <cosmo|all>
```

El comando reinicia los saldos de jugadores conectados y desconectados. Envía una confirmación interactiva al chat: pasa el cursor por el botón y haz clic en **[CONFIRMAR REINICIO]** dentro de 30 segundos. El token es personal y de un solo uso.

Por seguridad, `resetall` solo puede iniciarlo un jugador Java con `cosmos.admin`; no puede ejecutarse desde la consola ni desde jugadores Bedrock/Floodgate. `resetall all` solo reinicia los cosmos activos.

### Activar y desactivar cosmos

Cada cosmo tiene un estado independiente guardado en `cosmos.yml` mediante `enabled: true` o `enabled: false`. Los cosmos nuevos se crean activados.

```text
/cosmo status <cosmo> <enable|disable>
/cosmo list
```

Ejemplos:

```text
# Desactiva Gemas temporalmente.
/cosmo status gemas disable

# Vuelve a habilitar Gemas.
/cosmo status gemas enable

# Consulta todos los cosmos y su estado.
/cosmo list
```

`/cosmo list` muestra cada cosmo como **Activado** o **Desactivado**. Cuando un cosmo está desactivado:

- No entrega recompensas automáticas.
- No se puede enviar, dar, establecer, retirar ni reiniciar mediante los comandos de saldo.
- No puede usarse para compras ni ventas en EconomyShopGUI, PlayerKits2, tiendas internas ni tiendas por archivos.
- Las condiciones asociadas no retiran saldo.
- No se puede crear ni abrir una tienda vinculada a él.

El cosmo sigue siendo visible en `/cosmo view`, `/cosmo tops`, `/cosmo balance`, `/cosmo baltop` y `/cosmo list`, junto con sus saldos y rankings.

## Recompensas automáticas y condiciones

Las recompensas se configuran al crear o editar un cosmo. Cada vez que se produce el tipo de evento definido, el jugador recibe la cantidad configurada.

Las **condiciones** hacen lo contrario: retiran una cantidad de un cosmo cuando ocurre un evento. Son independientes de las recompensas, por lo que puedes tener ambas cosas en el mismo evento.

`/condition` es un alias de `/conditions`.

```text
/conditions create <id> <cosmo> <tipo> <cantidad>
/conditions list
/conditions info <id>
/conditions edit <id> <cosmo|type|amount> <valor>
/conditions delete <id>
/conditions reload
```

Ejemplos:

```text
# Cada bloque roto descuenta 1 Energía.
/conditions create desgaste energia BLOCK_BREAK 1

# Cada muerte del jugador descuenta 20 Gemas.
/conditions create muerte gemas PLAYER_DEATH 20

# Consulta la condición y cambia su importe.
/conditions info muerte
/conditions edit muerte amount 10
```

Las condiciones admiten todos los tipos de evento excepto `TIME`. Si el jugador no tiene suficiente saldo, la condición puede llevarlo a un balance negativo.

### Protección contra multicuentas

En `config.yml`:

```yml
anti-alt:
  same-ip-player-kill: true
```

Al activarlo, matar a un jugador con la misma IP no entrega recompensas `PLAYER_KILL` ni aplica condiciones `PLAYER_KILL` al asesino. Después ejecuta `/cosmo reload`.

## Comandos para jugadores

| Comando | Uso |
| --- | --- |
| `/cosmo list` | Lista los cosmos configurados y su tipo de recompensa. |
| `/cosmo balance <cosmo>` | Muestra tu saldo de ese cosmo sin abrir un menú. |
| `/cosmo baltop <cosmo>` | Muestra los 15 mayores saldos del cosmo. |
| `/cosmo send <cosmo> <jugador> <cantidad>` | Envía una cantidad positiva a un jugador conectado. |
| `/cosmo view` | Abre la vista de balances internos. Disponible incluso con `menus.enabled: false`. |
| `/cosmo menus [página]` | Abre el directorio de menús internos. Requiere menús activados. |
| `/cosmo tops` | Abre el selector de rankings internos. Disponible incluso con `menus.enabled: false`. |

Ejemplos:

```text
/cosmo balance gemas
/cosmo baltop gemas
/cosmo send gemas Alex 15
```

No puedes enviarte cosmos a ti mismo. El destinatario debe estar conectado y debes tener saldo suficiente. Los comandos `balance` y `baltop` siguen funcionando aunque los menús internos estén desactivados.

## Menús internos

Los menús internos son opcionales. Para activarlos, cambia `config.yml`:

```yml
menus:
  enabled: true
```

Después ejecuta:

```text
/cosmo reload
```

### Crear una tienda interna

```text
/cosmo menu create <nombre> <cofre|dispensador|horno|soporte|tolva> <tamaño>
```

Tamaños válidos:

| Tipo | Tamaño |
| --- | --- |
| `cofre` | 9, 18, 27, 36, 45 o 54 |
| `dispensador` | 9 |
| `horno` | 3 |
| `soporte` | 5 |
| `tolva` | 5 |

Ejemplo completo:

```text
# Crea una tienda de 27 espacios.
/cosmo menu create tienda cofre 27

# Con un diamante en la mano, configura el slot 13 para venderlo por 10 Gemas.
/cosmo menu item tienda 13 gemas 10

# Cambia el título y abre la tienda para probarla.
/cosmo menu displayname tienda &8Tienda de Gemas
/cosmo menu open tienda
```

Para editar libremente los objetos de la tienda, ejecuta `/cosmo menu edit tienda`, coloca o modifica los ítems y cierra el inventario. Después asigna a cada artículo su moneda y precio:

```text
/cosmo menu item <menú> <slot> <cosmo> <precio>
```

Los precios internos deben ser enteros positivos. Un jugador solo recibe el ítem si tiene saldo suficiente y espacio en su inventario.

### Comandos y visibilidad de tiendas

Una tienda puede configurarse desde `menus.yml` con un comando público:

```yml
menus:
  tienda:
    name: tienda
    displayname: '&8Tienda de Gemas'
    command: tienda
    hidden: false
    type: CHEST
    size: 27
    items:
      '13':
        material: DIAMOND
        amount: 1
        name: '&bDiamante'
        lore:
          - '&7Precio: &d10 Gemas'
        cosmo: gemas
        price: 10
```

Con esta configuración, `/tienda` abre el menú para todos los jugadores. Tras cambiar `command` o editar `menus.yml`, usa `/cosmo reload`.

```text
/cosmo menu enabled tienda
/cosmo menu disabled tienda
/cosmo menu status
/cosmo menu delete tienda
```

`hidden: true` evita que el menú aparezca en `/cosmo menus`, pero mantiene disponible su comando público. Desactivar un menú con `/cosmo menu disabled` también evita que pueda abrirse.

### Personalizar `menus.yml`

El archivo permite modificar títulos, ítems decorativos, materiales, lore, navegación y las tiendas. Las posiciones comienzan en `0`: la esquina superior izquierda de un cofre es el slot `0`.

Después de modificar el archivo, aplica los cambios con `/cosmo reload`.

## Placeholders

Instala PlaceholderAPI para usar los placeholders fuera de CosmosAPI. La expansión se registra automáticamente; no necesitas descargar nada con `/papi ecloud`.

| Placeholder | Resultado |
| --- | --- |
| `%cosmos_<cosmo>%` | Saldo abreviado del jugador. |
| `%cosmos_balance_<cosmo>%` | Alias explícito del saldo abreviado. |
| `%cosmos_<cosmo>_displayname%` | Nombre visual y coloreado del cosmo. |
| `%cosmos_cosmos%` | IDs de todos los cosmos separados por comas. |
| `%cosmosapi_<cosmo>%` | Formato antiguo compatible para el saldo. |

Reemplaza `<cosmo>` por el ID interno, no por el nombre visual:

```text
/cosmo create gemas MOB_KILL 2
```

Ejemplos válidos:

```text
%cosmos_gemas%
%cosmos_balance_gemas%
%cosmos_gemas_displayname%
```

Puedes comprobarlo dentro del juego:

```text
/papi parse me %cosmos_gemas%
```

### Ejemplo para TAB

```yml
scoreboards:
  default:
    title: '&d&lCOSMOS'
    lines:
      - '&fGemas: &d%cosmos_gemas%'
      - '&fTokens: &b%cosmos_tokens%'
```

### Placeholders en los menús de CosmosAPI

Todos los nombres y lore configurables de `menus.yml` procesan PlaceholderAPI. También existen valores internos:

| Valor | Uso |
| --- | --- |
| `{menu}` | Nombre del menú. |
| `{cosmo-name}` | Nombre visual del cosmo actual. |
| `{balance}` | Saldo que se está mostrando. |
| `{page}` | Número de página. |
| `{position}` | Posición en el ranking. |
| `{player}` | Nombre del jugador en el ranking. |

Ejemplo para el ítem de un cosmo en la vista de balances:

```yml
cosmo-item:
  material: NETHER_STAR
  name: '{cosmo-name}'
  lore:
    - '&7Saldo: &f%cosmos_<cosmo>%'
    - '&7Jugador: &f%player_name%'
```

Para el saldo del Cosmo que representa cada ítem, usa `{balance}`. Para referirte a un cosmo concreto, usa directamente `%cosmos_gemas%`.

## Tiendas Cosmos por archivos

CosmosAPI incluye tiendas independientes para usar cosmos sin requerir EconomyShopGUI. Al iniciar se crean estas carpetas:

```text
plugins/CosmosAPI/
├── sections/
├── shops/
└── inventory/
```

Una sección define el comando, la moneda y la tienda que se abrirá. Por ejemplo, `sections/general.yml` abre `shops/general.yml` con `/cosmotienda`:

```yml
enable: true
command: cosmotienda
shop: general
permission: ''
disabled-worlds: []
inventory: default
economy: EXTERNAL:CosmosAPI:gemas
```

También puedes usar directamente `currency: gemas` en el archivo de tienda. El valor después de `CosmosAPI:` debe ser el ID interno de un cosmo existente.

Los artículos se definen en `shops/general.yml` usando los campos básicos habituales de EconomyShopGUI:

```yml
enabled: true
name: '&d&lTienda de Gemas'
inventory: default

items:
  DIAMOND:
    material: DIAMOND
    amount: 1
    name: '&b&lDiamante'
    lore:
      - '&7Un diamante brillante.'
    buy: 10
    sell: 5
    slot: 20
```

- `buy` es el coste de compra y `sell` el pago por venta. Usa `-1` para desactivar una de las acciones.
- `slot` es opcional; sin él, los artículos se colocan automáticamente en el área central.
- Clic izquierdo compra y clic derecho vende la cantidad indicada en `amount`.
- `inventory/default.yml` controla título, tamaño, decoración, saldo y botones de página. Puedes crear más diseños y seleccionarlos con `inventory: <id>`.

Ejecuta `/cosmo reload` después de editar los archivos. La recarga cierra las tiendas abiertas para que ningún jugador complete una compra con precios o artículos ya reemplazados.

## EconomyShopGUI 7.2.1

CosmosAPI es compatible con **EconomyShopGUI 7.2.1** y puede usar cualquier cosmo como moneda externa para **comprar y vender**. Esta integración no depende de los menús internos de CosmosAPI.

> Puedes mantener `menus.enabled: false` y usar normalmente los menús de EconomyShopGUI. Esa opción solo desactiva las tiendas y los comandos públicos configurados en `menus.yml`; no desactiva `/cosmo view` ni `/cosmo tops`.

### Configurar una moneda Cosmos

1. Instala CosmosAPI y EconomyShopGUI en `plugins/`.
2. Crea el cosmo antes de configurarlo en la tienda:

   ```text
   /cosmo create gemas MOB_KILL 2
   /cosmo displayname gemas &dGemas
   ```

3. En la configuración del artículo o sección de EconomyShopGUI, define la economía externa con este formato exacto:

   ```text
   EXTERNAL:CosmosAPI_<id-del-cosmo>
   ```

4. Reinicia el servidor una vez tras instalar o actualizar CosmosAPI. CosmosAPI se inicia antes que EconomyShopGUI para registrar `CosmosAPI_<id>` como economía externa. Después puedes recargar la configuración de EconomyShopGUI con `/sreload`.

### Ejemplo de artículo

Para el cosmo cuyo ID es `gemas`:

```yml
pages:
  page1:
    items:
      '10':
        material: FIREWORK_ROCKET
        buy: 5
        sell: 2
        economy: EXTERNAL:CosmosAPI_gemas
```

Resultado:

- Comprar un cohete cuesta **5 gemas**.
- Vender un cohete entrega **2 gemas**.
- El artículo no utiliza el dinero de Vault.

Los placeholders de idioma de EconomyShopGUI `%buyPrice%` y `%sellPrice%` usan el mismo formato compacto que el scoreboard, seguido del **ID interno** del cosmo. Por ejemplo, un precio de `10244` se mostrará como `10,2k gemas`. El `displayName` del cosmo no se usa para formatear estos precios.

En EconomyShopGUI `7.2.1`, usa exclusivamente `buy:` y `sell:` como el resto de artículos de la tienda. No uses `buy-price` ni `sell-price` para los artículos de CosmosAPI. Tampoco escribas `buy-price::` o `sell-price::`: el doble `:` invalida el precio y EconomyShopGUI mostrará el objeto sin opciones de compra o venta.

Para un artículo que solo se pueda comprar, omite `sell:`:

```yml
'10':
  material: FIREWORK_ROCKET
  buy: 5
  economy: EXTERNAL:CosmosAPI_test
```

Los precios de cosmos deben ser números enteros no negativos. Los decimales se rechazan para impedir redondeos inesperados al cobrar o pagar. Si EconomyShopGUI no reconoce una moneda, comprueba que el ID sea correcto, que el cosmo exista y ejecuta `/cosmo reload` seguido de `/sreload`.

## PlayerKits2

CosmosAPI puede cobrar un cosmo cuando un jugador reclama un kit de [PlayerKits2](https://github.com/Ajneb97/PlayerKits2). La integración es opcional: instala ambos plugins y activa los kits que quieras cobrar en `config.yml`.

```yml
playerkits2:
  enabled: true
  kits:
    starter:
      cosmo: gemas
      price: 10244
```

- El ID bajo `kits:` debe coincidir con el nombre del kit de PlayerKits2.
- `cosmo` debe ser un cosmo existente y activo; `price` debe ser un entero positivo.
- Configura el precio nativo del kit de PlayerKits2 en `0` para evitar un doble cobro.
- Funciona al reclamar con `/kit <kit>`, `/kit claim <kit>` y desde el menú de PlayerKits2.
- CosmosAPI retira el importe antes de entregar el kit y lo devuelve si PlayerKits2 rechaza la entrega, por ejemplo por cooldown, permisos, requisitos o inventario lleno.
- El mensaje de confirmación muestra el importe abreviado, por ejemplo `10,2k`.

Después de modificar esta sección, ejecuta `/cosmo reload`.

## Hologramas

Los hologramas requieren DecentHolograms y esta opción activa en `config.yml`:

```yml
holograms:
  enabled: true
```

Comandos:

```text
/cosmo hologram generate <id> <cosmo>
/cosmo hologram move <id>
/cosmo hologram title <id> <título>
/cosmo hologram delete <id>
/cosmo hologram list
```

Ejemplo:

```text
# Ponte en la ubicación deseada y genera el top de Gemas.
/cosmo hologram generate topgemas gemas

# Personaliza el título.
/cosmo hologram title topgemas &d&lTOP GEMAS

# Muévelo a tu posición actual si lo necesitas.
/cosmo hologram move topgemas
```

## Permisos

| Permiso | Descripción | Predeterminado |
| --- | --- | --- |
| `cosmos.admin` | Crear, editar, eliminar cosmos; administrar saldos, menús y hologramas. | OP |
| `cosmos.send` | Enviar cosmos a otros jugadores. | Sí |
| `cosmos.view` | Consultar saldos, tops, menús y rankings públicos. | Sí |
| `cosmos.conditions` | Administrar condiciones de retiro. | OP |

## Configuración y archivos

### `config.yml`

```yml
memory-cleanup:
  # Minutos entre cada liberación de memoria. Usa 0 para desactivarla.
  interval-minutes: 30

anti-alt:
  # Impide recompensas y condiciones PLAYER_KILL entre jugadores con la misma IP.
  same-ip-player-kill: false

menus:
  # Habilita las interfaces y tiendas internas de CosmosAPI.
  enabled: false

holograms:
  # Habilita los hologramas de ranking mediante DecentHolograms.
  enabled: true

playerkits2:
  # Cobra cosmos al reclamar kits de PlayerKits2. El precio nativo del kit debe ser 0.
  enabled: false
  kits:
    # starter:
    #   cosmo: gemas
    #   price: 100
```

Ejecuta `/cosmo reload` después de cambiar `config.yml`, `cosmos.yml`, `conditions.yml` o `menus.yml`.

| Archivo | Descripción |
| --- | --- |
| `config.yml` | Ajustes generales, anti-multicuenta, menús y hologramas. |
| `cosmos.yml` | Definiciones de cosmos y hologramas. |
| `conditions.yml` | Condiciones que retiran cosmos. |
| `menus.yml` | Vista de balances, rankings y tiendas internas. |
| `sections/` | Secciones públicas para las tiendas Cosmos por archivos. |
| `shops/` | Artículos, precios y páginas de las tiendas Cosmos por archivos. |
| `inventory/` | Diseños reutilizables para las tiendas Cosmos por archivos. |
| `players.yml` | Balances y datos de tiempo de los jugadores. |
| `messages.yml` | Mensajes editables del plugin. |

## Desarrollo

El proyecto se compila exclusivamente con **Maven** mediante `pom.xml`:

```bash
mvn package
```

El JAR se genera en `target/`. Este proyecto no usa Gradle y no requiere ningún archivo `build.gradle`.