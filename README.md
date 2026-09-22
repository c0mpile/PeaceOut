# PeaceOut

PeaceOut is a Paper plugin that provides configurable protection, utility, storage, and quality-of-life features for individual players.

Players can enable or disable available features through an in-game menu or commands. Most features are controlled by a master switch, while backpack storage and automatic backpack pickup operate independently from that switch.

## Requirements

- Paper 1.21.x or the server version supported by Paper API `26.2`
- Java 25 or newer
- A permissions plugin such as LuckPerms

The project currently uses the Paper API dependency:

```kotlin
io.papermc.paper:paper-api:26.2.build.+
```

## Features

### Protection

The following settings provide protection from specific hazards or hostile behavior:

- **Drowning protection**
  - Prevents drowning damage.
- **Mob targeting protection**
  - Prevents hostile mobs from targeting the player.
- **No food drain**
  - Prevents the player's food level from decreasing.
- **Health regeneration**
  - This setting is present in the configuration and menus. The current listener does not implement additional regeneration behavior.
- **No fall damage**
  - Prevents fall damage.
- **No lava damage**
  - Prevents lava damage.
- **No fire damage**
  - Prevents fire and fire tick damage.

### Utility

- **Infinite durability**
  - Prevents item durability damage.
- **Infinite fireworks**
  - Prevents fireworks from being consumed when boosting with an Elytra.
- **Keep inventory**
  - Keeps the player's inventory and experience after death.
- **Drop vacuum**
  - Collects nearby item entities into the player's ordinary inventory.
  - Collects nearby experience orbs.
  - Scans within an 8-block radius.
  - Runs automatically every two server ticks.
  - Only uses the normal player inventory. It does not place items into backpacks.
- **Experience multiplier**
  - Changes the amount of experience granted by experience events.
  - The accepted range is `0.25x` through `10.0x`.
- **Block-break speed**
  - Changes the player's block-breaking speed multiplier.
  - The accepted range is `0.25x` through `10.0x`.

### Storage and convenience

- **Trash can**
  - Opens a temporary inventory where items can be placed and discarded when the inventory is closed.
- **Backpacks**
  - Provides access to personal backpack inventories.
  - Players can have up to 24 backpacks, depending on their permissions.
- **Automatic backpack pickup**
  - Attempts to place picked-up items into the normal inventory first.
  - If the normal inventory cannot hold the entire item stack, the remaining items are placed into the player's backpacks.
  - Backpack slot `49` is reserved for navigation and is never used for storage.
  - This feature is separate from the drop vacuum.
  - This feature does not require the ordinary-feature master switch to be enabled.

## Master switch

The master switch controls ordinary PeaceOut features.

When the master switch is disabled, the following types of features stop working:

- Protection settings
- Hunger protection
- Durability protection
- Firework preservation
- Keep inventory
- Drop vacuum
- Other ordinary listener-based features

The following features are handled separately:

- Backpacks
- Automatic backpack pickup
- Trash access

The master switch can be changed in the settings menu or with:

```text
/peaceout on
/peaceout off
/peaceout toggle
```

## Commands

### `/peaceout`

Opens the personal PeaceOut settings menu.

Alias:

```text
/po
```

Examples:

```text
/peaceout
/po
```

### `/peaceout status`

Displays the current state of the player's settings, including:

- Master switch status
- Individual toggle settings
- Experience multiplier
- Block-break speed multiplier
- Available backpack count

Example:

```text
/peaceout status
```

### `/peaceout <setting>`

Toggles a setting.

Examples:

```text
/peaceout drowning
/peaceout drop-vacuum
/peaceout keep-inventory
/peaceout backpack-pickup
```

A setting can also be explicitly enabled or disabled:

```text
/peaceout drowning on
/peaceout drowning off
/peaceout drop-vacuum enable
/peaceout drop-vacuum disable
```

Accepted enable values include:

```text
on
true
enable
enabled
```

Accepted disable values include:

```text
off
false
disable
disabled
```

### Multiplier commands

Experience multiplier aliases:

```text
/peaceout xp
/peaceout experience
/peaceout experience-multiplier
```

Block-break speed aliases:

```text
/peaceout block-speed
/peaceout break-speed
/peaceout block-break-speed
```

View the current multiplier:

```text
/peaceout xp
/peaceout block-speed
```

Set a multiplier:

```text
/peaceout xp 2.0
/peaceout block-speed 1.5
```

Valid multiplier values are between `0.25` and `10.0`.

### `/peaceout admin`

Opens the administrator menu.

The admin menu allows administrators to:

- View players recorded in the plugin configuration
- Open another player's settings
- Change that player's master switch
- Toggle that player's settings
- Change that player's multipliers

### `/trash`

Opens the temporary trash inventory.

The player must have the `peaceout.trash` permission and the trash setting must be enabled.

Items placed in the trash inventory are discarded when the inventory is closed.

### `/bp`

Opens the backpack selector.

Examples:

```text
/bp
/bp 1
/bp 4
```

Using `/bp` without a number opens the backpack selector. Using `/bp <number>` opens a specific unlocked backpack.

The player must have:

- The `peaceout.backpack` permission
- At least one backpack permission
- The `backpack` setting enabled

## Permissions

### Personal settings

```text
peaceout.use
```

Allows the player to use personal PeaceOut settings and ordinary PeaceOut commands.

This permission is required for the ordinary listener-based features, including the drop vacuum and most protection features.

### Administrator access

```text
peaceout.admin
```

Allows access to:

```text
/peaceout admin
```

This permission defaults to server operators.

### Trash

```text
peaceout.trash
```

Allows the player to use:

```text
/trash
```

The player must also enable the trash setting.

### Backpacks

```text
peaceout.backpack
```

Allows access to backpack functionality.

The player must also have at least one numbered backpack permission.

### Numbered backpack permissions

Each numbered permission grants access up to that backpack number.

```text
peaceout.backpacks.1
peaceout.backpacks.2
peaceout.backpacks.3
peaceout.backpacks.4
peaceout.backpacks.5
peaceout.backpacks.6
peaceout.backpacks.7
peaceout.backpacks.8
peaceout.backpacks.9
peaceout.backpacks.10
peaceout.backpacks.11
peaceout.backpacks.12
peaceout.backpacks.13
peaceout.backpacks.14
peaceout.backpacks.15
peaceout.backpacks.16
peaceout.backpacks.17
peaceout.backpacks.18
peaceout.backpacks.19
peaceout.backpacks.20
peaceout.backpacks.21
peaceout.backpacks.22
peaceout.backpacks.23
peaceout.backpacks.24
```

The plugin checks from `24` down to `1` and uses the highest numbered permission found.

For example:

```text
peaceout.backpacks.1
```

gives access to one backpack.

```text
peaceout.backpacks.5
```

gives access to backpacks 1 through 5 in the menu.

The player still needs `peaceout.backpack` before the backpack commands and menus can be used.

## Example LuckPerms setup

Grant a player access to ordinary PeaceOut features:

```text
/lp user PlayerName permission set peaceout.use true
```

Allow the player to use the trash can:

```text
/lp user PlayerName permission set peaceout.trash true
```

Allow the player to use backpacks:

```text
/lp user PlayerName permission set peaceout.backpack true
```

Give the player access to five backpacks:

```text
/lp user PlayerName permission set peaceout.backpacks.5 true
```

Allow a player to use the administrator menu:

```text
/lp user PlayerName permission set peaceout.admin true
```

## Configuration

The default configuration is stored in `config.yml`.

Current defaults:

```yaml
default-enabled: true

defaults:
  drowning: false
  targeting: false
  hunger: false
  regeneration: false
  fall: false
  lava: false
  fire: false
  durability: false
  fireworks: false
  keep-inventory: false
  drop-vacuum: false
  trash: false
  backpack: false
  backpack-pickup: false

  experience-multiplier: 1.0
  block-break-speed: 1.0

players: {}
```

### `default-enabled`

Controls the initial state of the master switch for new players.

```yaml
default-enabled: true
```

This value is applied when a player is first initialized. Changing it later does not automatically change the master switch for players who already have saved settings.

### `defaults`

The values under `defaults` are copied into a player's settings the first time that player joins.

For example:

```yaml
defaults:
  drop-vacuum: true
  experience-multiplier: 1.5
```

This enables the drop vacuum and sets the initial experience multiplier for new players.

Existing player settings are stored separately under:

```yaml
players:
  <player-uuid>:
```

Changing a default does not overwrite those saved player settings.

## Backpack storage

Backpack contents are stored in the player's configuration section.

The plugin reserves slot `49` in every backpack inventory for navigation. That slot:

- Displays the backpack navigation button
- Cannot be used for automatic backpack storage
- Is removed from saved backpack contents
- Is cleared when backpack contents are loaded

Backpack contents are stored under paths similar to:

```yaml
players:
  00000000-0000-0000-0000-000000000000:
    backpacks:
      1:
        - ...
```

## Menu behavior

The personal settings menu provides:

- Individual feature toggles
- Master switch control
- Multiplier selection
- Status display
- Backpack access
- Menu close control

Feature icons remain consistent whether a setting is enabled or disabled. The setting's state is shown in the item name and lore instead of replacing the icon with dye or a barrier.

The backpack selector only displays backpacks the player has unlocked. Locked backpack positions remain empty.

## Building the project

Clone the repository, then run:

```bash
gradle clean build
```

The compiled plugin JAR is produced in:

```text
build/libs/
```

The JAR uses the base name:

```text
PeaceOut
```

Copy the generated JAR into the server's `plugins` directory and restart the server.

## Project details

The Gradle project currently uses:

- Group: `c0mpile`
- Java compilation release: Java 25
- Paper API: `26.2.build.+`

The version in `build.gradle.kts` and the version in `plugin.yml` should be kept synchronized when preparing a release.
