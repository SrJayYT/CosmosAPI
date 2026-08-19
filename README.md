# CosmosAPI

Plugin de Spigot que añade monedas virtuales llamadas **cosmos**, recompensas automáticas, menús de compra, hologramas y compatibilidad con PlaceholderAPI, TAB y EconomyShopGUI.

**Autor:** devpapo  
**Plataforma:** Spigot 1.21.x  
**Build:** Maven (`pom.xml`)  
**Paquete base:** `com.devpapo.cosmosapi`

## Descarga

Descarga la última versión desde las [releases de GitHub](https://github.com/SrJayYT/CosmosAPI/releases).

## Características

### Cosmos

- Múltiples monedas virtuales llamadas cosmos.
- Datos y balances persistentes en archivos YAML.
- Nombre interno y nombre visible con colores para cada cosmo.
- Colores legacy (`&a`, `&c`, etc.) y RGB/hexadecimales.

### Recompensas automáticas

- Tiempo conectado.
- Matar jugadores.
- Matar mobs.
- Romper bloques.
- Colocar bloques.

### Menús y tiendas

- Menús configurables desde `menus.yml`.
- Inventarios de cofre, dispensador y horno.
- Productos comprables con el cosmo que elijas.
- `/cosmo menu edit <menú>` abre una GUI para editar los productos directamente.
- `/cosmo view` muestra un menú paginado con los menús creados y los balances del jugador.
- `/cosmo tops` muestra el Top 15 de cada cosmo.
- `/cosmo reload` aplica cambios de configuración sin reiniciar el servidor.

### Integraciones

- Placeholders compatibles con PlaceholderAPI y TAB.
- EconomyShopGUI puede cobrar y pagar con cualquier cosmo como moneda externa.

## Comandos

```text
/cosmo create <nombre> <tipo> <cantidad>
/cosmo displayname <cosmo> <nombre_visible>
/cosmo delete <nombre>
/cosmo give <cosmo> <jugador> <cantidad>
/cosmo set <cosmo> <jugador> <cantidad>
/cosmo send <cosmo> <jugador> <cantidad>
/cosmo menu create <nombre> <tipo> <tamano>
/cosmo menu open <nombre>
/cosmo menu edit <nombre>
/cosmo menu delete <nombre>
/cosmo view
/cosmo tops
/cosmo reload
/cosmo help
```

`/cosmos` funciona como alias de `/cosmo`.

## Permisos

```text
cosmos.admin
cosmos.send
cosmos.view
```

## Placeholders

Instala PlaceholderAPI en el servidor. CosmosAPI detecta PlaceholderAPI al iniciar y registra los placeholders automáticamente; no hace falta descargar una expansión mediante `/papi ecloud`.

| Placeholder | Resultado |
| --- | --- |
| `%cosmosapi_<cosmo>%` | Saldo del jugador en ese cosmo. |
| `%cosmosapi_<cosmo>_displayname%` | Nombre visible y coloreado del cosmo. |
| `%cosmosapi_cosmos%` | IDs de todos los cosmos, separados por comas. |

El identificador es el nombre interno creado con `/cosmo create`, no el nombre visible. Por ejemplo, tras ejecutar:

```text
/cosmo create gemas mob_kill 2
```

el placeholder del saldo será:

```text
%cosmosapi_gemas%
```

### Ejemplo para TAB

```yaml
scoreboards:
  default:
    title: '&d&lCOSMOS'
    lines:
      - '&fGemas: &d%cosmosapi_gemas%'
      - '&fMonedas: &6%cosmosapi_monedas%'
```

Puedes probar un placeholder dentro del juego con:

```text
/papi parse me %cosmosapi_gemas%
```

## EconomyShopGUI: cobrar con cosmos

Cada cosmo creado se registra en EconomyShopGUI como una moneda externa.

Formato de la moneda:

```text
EXTERNAL:CosmosAPI_<id-del-cosmo>
```

Por ejemplo, si el cosmo tiene el ID `kills`, usa:

```yaml
economy: EXTERNAL:CosmosAPI_kills
```

### Ejemplo de objeto de tienda

```yaml
items:
  diamond_sword:
    material: DIAMOND_SWORD
    buy-price: 25
    sell-price: 10
    economy: EXTERNAL:CosmosAPI_kills
```

Con esta configuración:

- Comprar una espada cuesta 25 cosmos de `kills`.
- Vender una espada entrega 10 cosmos de `kills`.
- No se utiliza dinero de Vault para ese objeto.

### Ejemplo con otro cosmo

```yaml
items:
  golden_apple:
    material: GOLDEN_APPLE
    buy-price: 5
    sell-price: 2
    economy: EXTERNAL:CosmosAPI_gems
```

### Requisitos de EconomyShopGUI

1. Instala CosmosAPI y EconomyShopGUI en la carpeta `plugins`.
2. Crea el cosmo primero en CosmosAPI.
3. Reinicia el servidor para que EconomyShopGUI detecte la moneda externa.
4. Añade `economy: EXTERNAL:CosmosAPI_<id>` al objeto o sección de tienda que deba usar cosmos.

Los IDs deben coincidir exactamente. Un cosmo con ID `kills` siempre se configura como:

```yaml
economy: EXTERNAL:CosmosAPI_kills
```

Los precios de cosmos son enteros. Si EconomyShopGUI recibe un precio decimal, se redondea hacia arriba.

## Archivos

| Archivo | Descripción |
| --- | --- |
| `config.yml` | Configuración general y mensajes. |
| `cosmos.yml` | Definiciones de las monedas virtuales. |
| `menus.yml` | Menús y productos de tienda. |
| `players.yml` | Balances de los jugadores. |

## Desarrollo

El proyecto utiliza **Maven** mediante `pom.xml`.