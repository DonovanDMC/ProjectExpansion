# Project Expansion
An unofficial expansion of [ProjectE](https://www.curseforge.com/minecraft/mc-mods/projecte).
## This mod is not officially supported or endorsed by ProjectE
Do not contact them for support. If you want
support, [open an issue](https://github.com/DonovanDMC/ProjectExpansion/issues/new) on the github, or ask about it in
the `#bug-reports` channel or our [Discord Server](https://discord.gg/SwxbbmMKQH) - make sure to mention what version of
the mod you're using!

This is based on [ProjectEX](https://www.curseforge.com/minecraft/mc-mods/projectex-forge), which seems to be mostly
abandoned at this point. This has been rewritten from the ground up, and has some new things.

If you want to get updates instantly or just generally chat with developers, come see us in our [Discord Server](https://discord.gg/SwxbbmMKQH).

### Where To Find Releases
- [Github Releases](https://github.com/DonovanDMC/ProjectExpansion/releases)
- [CurseForge](https://www.curseforge.com/minecraft/mc-mods/project-expansion)
- [Modrinth](https://modrinth.com/mod/project-expansion)


See the [wiki](https://github.com/DonovanDMC/ProjectExpansion/wiki) for more information about the mod.

### Refining Recipes
The EMC Refinery's recipes are data driven, so datapacks and other mods can add their own under `data/<namespace>/recipe/`.
Mods using datagen can use `RefiningRecipeBuilder`.
```json
{
	"type": "projectexpansion:refining",
	"ingredient": { "tag": "c:raw_materials/ianthium" },
	"count": 1,
	"result": { "id": "projectexpansion:ianthium_ingot" },
	"emc": 64,
	"time": 100
}
```
- `ingredient`: any ingredient, including tags and NeoForge's custom ingredients
- `count` (default `1`): how many of the ingredient each refine uses
- `result`: an item stack, which can have a `count` and `components`
- `emc`: the EMC it costs, paid a bit each tick while it runs
- `time` (default `100`): how many ticks it takes

NeoForge's `neoforge:conditions` work as with any other recipe. ProjectE maps refining recipes, adding the EMC cost to the value of
the result; that can be turned off with the Refining Mapper's `include_emc_cost` option in ProjectE's mapping config.

### Credits
The EMC Refinery's textures are recoloured from [Modern Industrialization](https://github.com/AztechMC/Modern-Industrialization)'s machine hull and furnace (CC0).
Ianthium's ore, raw, ingot and block textures are recoloured from JoeEnderman's [Assorted Minecraft style textures](https://opengameart.org/content/assorted-minecraft-style-textures-defaultmineraltinpng) (CC0), and its nugget from Modern Industrialization's nugget template (CC0).
