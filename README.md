# PetControl (fork dla Minecraft 26.1.2)

Fork [FrankTCA/PetControl](https://github.com/FrankTCA/PetControl) zbudowany pod **Paper 26.1.2**.

PetControl allows server admins to view info on pets, change ownership, sit/stand pets, and tame a pet to themselves.

## Co zostalo zmienione w tym forku

Upstream v4.0 wywalal sie na 26.1.2 z:

```
java.lang.NoClassDefFoundError: net/minecraft/world/entity/EntityTameableAnimal
```

`EntityTameableAnimal` to **Spigot-mapowana** nazwa klasy `TamableAnimal`. Upstream budowal
jar przez `reobfJar` (reobfuskacja do mapowan Spigota) przeciwko dev-bundle **1.21.11**.
Paper od 1.20.5 chodzi w runtime na mapowaniach Mojanga, a jar zreobfuskowany pod stare
mapowania nie dostaje juz poprawnego remapu -> klasy nie ma.

Zmiany:

* dev-bundle `1.21.11-R0.1-SNAPSHOT` -> `26.1.2.build.74-stable`
* `paperweight-userdev` 2.0.0-beta.19 -> 2.0.0-beta.21
* usuniete `tasks.assemble { dependsOn(reobfJar) }` — jar wychodzi **Mojang-mapped**,
  czyli w tym samym namespace co runtime Papera
* toolchain Java 21 -> **Java 25** (wymagane przez MC 26.1)
* `settings.gradle`: plugin foojay-resolver, zeby Gradle sam sciagnal JDK 25
* `plugin.yml`: `api-version` `1.21` -> `26.1`
* CI: kazdy push do `main` = build + automatyczny release (patrz nizej)

## Instalacja

Wrzuc jar z [zakladki Releases](../../releases/latest) do `plugins/` i zrestartuj serwer.
Wymagany Paper 26.1.2 (lub nowszy z tej samej linii) na Javie 25.

## Build lokalnie

```bash
./gradlew build
```

Jar lezy w `build/libs/PetControl-<wersja>.jar`.

Podbijajac wersje Minecrafta zmieniasz tylko `ext.paperVersion` / `ext.minecraftVersion`
na gorze `build.gradle`.

## Automatyczny release

`.github/workflows/release.yml` — dowolna zmiana w kodzie pushnieta na `main` odpala
build na JDK 25 i tworzy release z tagiem `v<wersja>-build.<numer runa>` oraz gotowym
jarem w zalacznikach.

## Commands

* `/petinfo` — Shows information on the pet you are looking at
* `/transferpetowner <new owner>` — Transfers ownership of a pet
* `/tamepet [owner]` — Tames a pet to yourself (or someone else)
* `/togglesit` — Tell any pet to sit/stand
* `/healpet` — Heals the pet you are facing
* `/roam <radius> [center x] [center z] [guarded]` — pet lata po okolicy zamiast chodzic za toba
* `/follow` — powrot do trybu podazania
* `/guard` — przelacza tryb guarded
