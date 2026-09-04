# CosmosAPI

CosmosAPI añade monedas virtuales configurables llamadas **cosmos**. Cada cosmo puede obtenerse automáticamente por acciones del juego, enviarse entre jugadores, usarse en los menús internos del plugin o como moneda de compra/venta en EconomyShopGUI.

| Dato | Valor |
| --- | --- |
| Versión | `1.0.2` |
| Plataforma | Spigot/Paper 1.21.x |
| Java | 17 |
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
9. [EconomyShopGUI](#economyshopgui)
10. [Hologramas](#hologramas)
11. [Configuración y archivos](#configuración-y-archivos)

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
| EconomyShopGUI | Comprar y vender usando cosmos como moneda. |
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
/cosmo giveall <cosmo> <cantidad>
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
```

`giveall` solo acepta cantidades positivas. `/cosmo give` y `/cosmo set` aceptan números negativos.

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
- No se puede enviar mediante `/cosmo send`.
- No puede usarse para compras ni ventas en EconomyShopGUI.
- Las condiciones asociadas no retiran saldo.

Los administradores todavía pueden usar `/cosmo give`, `/cosmo set` y `/cosmo giveall` para ajustar balances mientras está desactivado. Esto permite preparar saldos o realizar mantenimiento antes de activarlo otra vez.

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
| `/cosmo view` | Abre la vista de balances internos. Requiere menús activados. |
| `/cosmo menus [página]` | Abre el directorio de menús internos. Requiere menús activados. |
| `/cosmo tops` | Abre el selector de rankings internos. Requiere menús activados. |

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

`%cosmos_<cosmo>%` es un marcador especial dentro de esa vista: se sustituye por el ID del cosmo que representa cada ítem. Para referirte a un cosmo concreto, usa directamente `%cosmos_gemas%`.

## EconomyShopGUI

EconomyShopGUI puede usar cualquier cosmo como moneda externa para **comprar y vender**. Esta integración no depende de los menús internos de CosmosAPI.

> Puedes mantener `menus.enabled: false` y usar normalmente los menús de EconomyShopGUI. Esa opción solo desactiva `/cosmo view`, `/cosmo menus`, `/cosmo tops` y las tiendas internas de CosmosAPI.

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

4. Recarga EconomyShopGUI con `/sreload`. Si acabas de cambiar cosmos desde CosmosAPI, ejecuta primero `/cosmo reload` y después `/sreload`.

### Ejemplo de artículo

Para el cosmo cuyo ID es `gemas`:

```yml
items:
  diamond_sword:
    material: DIAMOND_SWORD
    buy-price: 25
    sell-price: 10
    economy: EXTERNAL:CosmosAPI_gemas
```

Resultado:

- Comprar una espada cuesta **25 Gemas**.
- Vender una espada entrega **10 Gemas**.
- El artículo no utiliza el dinero de Vault.

Los precios de cosmos deben ser números enteros no negativos. Los decimales se rechazan para impedir redondeos inesperados al cobrar o pagar. Si EconomyShopGUI no reconoce una moneda, comprueba que el ID sea correcto, que el cosmo exista y ejecuta `/sreload`.

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
```

Ejecuta `/cosmo reload` después de cambiar `config.yml`, `cosmos.yml`, `conditions.yml` o `menus.yml`.

| Archivo | Descripción |
| --- | --- |
| `config.yml` | Ajustes generales, anti-multicuenta, menús y hologramas. |
| `cosmos.yml` | Definiciones de cosmos y hologramas. |
| `conditions.yml` | Condiciones que retiran cosmos. |
| `menus.yml` | Vista de balances, rankings y tiendas internas. |
| `players.yml` | Balances y datos de tiempo de los jugadores. |
| `messages.yml` | Mensajes editables del plugin. |

## Desarrollo

El proyecto se compila exclusivamente con **Maven** mediante `pom.xml`:

```bash
mvn package
```

El JAR se genera en `target/`. Este proyecto no usa Gradle y no requiere ningún archivo `build.gradle`.