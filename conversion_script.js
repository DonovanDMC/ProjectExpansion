// this is a KubeJS script that removes all ProjectEX recipes, and adds recipes
// to convert ProjectEX items to ProjectExpansion items.
// place this in the kubejs/server_scripts directory
// we remove the recipes to avoid infinite emc exploits
settings.logAddedRecipes = true
settings.logRemovedRecipes = true
settings.logSkippedRecipes = false
settings.logErroringRecipes = true

onEvent("recipes", event => {
    event.remove({mod: "projectex"});

    event.shapeless("projectexpansion:personal_link", ["projectex:energy_link"]);
    event.shapeless("projectexpansion:arcane_tablet", ["projectex:arcane_tablet"]);

    event.shapeless("projectexpansion:final_star_shard", ["projectex:final_star_shard"]);
    event.shapeless("projectexpansion:final_star", ["projectex:final_star"]);

    event.shapeless("projectexpansion:basic_collector", ["projectex:basic_collector"]);
    event.shapeless("projectexpansion:dark_collector", ["projectex:dark_collector"]);
    event.shapeless("projectexpansion:red_collector", ["projectex:red_collector"]);
    event.shapeless("projectexpansion:magenta_collector", ["projectex:magenta_collector"]);
    event.shapeless("projectexpansion:pink_collector", ["projectex:pink_collector"]);
    event.shapeless("projectexpansion:purple_collector", ["projectex:purple_collector"]);
    event.shapeless("projectexpansion:violet_collector", ["projectex:violet_collector"]);
    event.shapeless("projectexpansion:blue_collector", ["projectex:blue_collector"]);
    event.shapeless("projectexpansion:cyan_collector", ["projectex:cyan_collector"]);
    event.shapeless("projectexpansion:green_collector", ["projectex:green_collector"]);
    event.shapeless("projectexpansion:lime_collector", ["projectex:lime_collector"]);
    event.shapeless("projectexpansion:yellow_collector", ["projectex:yellow_collector"]);
    event.shapeless("projectexpansion:orange_collector", ["projectex:orange_collector"]);
    event.shapeless("projectexpansion:white_collector", ["projectex:white_collector"]);
    event.shapeless("projectexpansion:fading_collector", ["projectex:fading_collector"]);
    event.shapeless("projectexpansion:final_collector", ["projectex:final_collector"]);

    event.shapeless("projectexpansion:basic_compressed_collector", ["projectex:basic_compressed_collector"]);
    event.shapeless("projectexpansion:dark_compressed_collector", ["projectex:dark_compressed_collector"]);
    event.shapeless("projectexpansion:red_compressed_collector", ["projectex:red_compressed_collector"]);
    event.shapeless("projectexpansion:magenta_compressed_collector", ["projectex:magenta_compressed_collector"]);
    event.shapeless("projectexpansion:pink_compressed_collector", ["projectex:pink_compressed_collector"]);
    event.shapeless("projectexpansion:purple_compressed_collector", ["projectex:purple_compressed_collector"]);
    event.shapeless("projectexpansion:violet_compressed_collector", ["projectex:violet_compressed_collector"]);
    event.shapeless("projectexpansion:blue_compressed_collector", ["projectex:blue_compressed_collector"]);
    event.shapeless("projectexpansion:cyan_compressed_collector", ["projectex:cyan_compressed_collector"]);
    event.shapeless("projectexpansion:green_compressed_collector", ["projectex:green_compressed_collector"]);
    event.shapeless("projectexpansion:lime_compressed_collector", ["projectex:lime_compressed_collector"]);
    event.shapeless("projectexpansion:yellow_compressed_collector", ["projectex:yellow_compressed_collector"]);
    event.shapeless("projectexpansion:orange_compressed_collector", ["projectex:orange_compressed_collector"]);
    event.shapeless("projectexpansion:white_compressed_collector", ["projectex:white_compressed_collector"]);
    event.shapeless("projectexpansion:fading_compressed_collector", ["projectex:fading_compressed_collector"]);
    event.shapeless("projectexpansion:final_compressed_collector", ["projectex:final_compressed_collector"]);

    event.shapeless("projectexpansion:basic_relay", ["projectex:basic_relay"]);
    event.shapeless("projectexpansion:dark_relay", ["projectex:dark_relay"]);
    event.shapeless("projectexpansion:red_relay", ["projectex:red_relay"]);
    event.shapeless("projectexpansion:magenta_relay", ["projectex:magenta_relay"]);
    event.shapeless("projectexpansion:pink_relay", ["projectex:pink_relay"]);
    event.shapeless("projectexpansion:purple_relay", ["projectex:purple_relay"]);
    event.shapeless("projectexpansion:violet_relay", ["projectex:violet_relay"]);
    event.shapeless("projectexpansion:blue_relay", ["projectex:blue_relay"]);
    event.shapeless("projectexpansion:cyan_relay", ["projectex:cyan_relay"]);
    event.shapeless("projectexpansion:green_relay", ["projectex:green_relay"]);
    event.shapeless("projectexpansion:lime_relay", ["projectex:lime_relay"]);
    event.shapeless("projectexpansion:yellow_relay", ["projectex:yellow_relay"]);
    event.shapeless("projectexpansion:orange_relay", ["projectex:orange_relay"]);
    event.shapeless("projectexpansion:white_relay", ["projectex:white_relay"]);
    event.shapeless("projectexpansion:fading_relay", ["projectex:fading_relay"]);
    event.shapeless("projectexpansion:final_relay", ["projectex:final_relay"]);

    event.shapeless("projectexpansion:basic_power_flower", ["projectex:basic_power_flower"]);
    event.shapeless("projectexpansion:dark_power_flower", ["projectex:dark_power_flower"]);
    event.shapeless("projectexpansion:red_power_flower", ["projectex:red_power_flower"]);
    event.shapeless("projectexpansion:magenta_power_flower", ["projectex:magenta_power_flower"]);
    event.shapeless("projectexpansion:pink_power_flower", ["projectex:pink_power_flower"]);
    event.shapeless("projectexpansion:purple_power_flower", ["projectex:purple_power_flower"]);
    event.shapeless("projectexpansion:violet_power_flower", ["projectex:violet_power_flower"]);
    event.shapeless("projectexpansion:blue_power_flower", ["projectex:blue_power_flower"]);
    event.shapeless("projectexpansion:cyan_power_flower", ["projectex:cyan_power_flower"]);
    event.shapeless("projectexpansion:green_power_flower", ["projectex:green_power_flower"]);
    event.shapeless("projectexpansion:lime_power_flower", ["projectex:lime_power_flower"]);
    event.shapeless("projectexpansion:yellow_power_flower", ["projectex:yellow_power_flower"]);
    event.shapeless("projectexpansion:orange_power_flower", ["projectex:orange_power_flower"]);
    event.shapeless("projectexpansion:white_power_flower", ["projectex:white_power_flower"]);
    event.shapeless("projectexpansion:fading_power_flower", ["projectex:fading_power_flower"]);
    event.shapeless("projectexpansion:final_power_flower", ["projectex:final_power_flower"]);

    event.shapeless("projectexpansion:magenta_matter", ["projectex:magenta_matter"]);
    event.shapeless("projectexpansion:pink_matter", ["projectex:pink_matter"]);
    event.shapeless("projectexpansion:purple_matter", ["projectex:purple_matter"]);
    event.shapeless("projectexpansion:violet_matter", ["projectex:violet_matter"]);
    event.shapeless("projectexpansion:blue_matter", ["projectex:blue_matter"]);
    event.shapeless("projectexpansion:cyan_matter", ["projectex:cyan_matter"]);
    event.shapeless("projectexpansion:green_matter", ["projectex:green_matter"]);
    event.shapeless("projectexpansion:lime_matter", ["projectex:lime_matter"]);
    event.shapeless("projectexpansion:yellow_matter", ["projectex:yellow_matter"]);
    event.shapeless("projectexpansion:orange_matter", ["projectex:orange_matter"]);
    event.shapeless("projectexpansion:white_matter", ["projectex:white_matter"]);
    event.shapeless("projectexpansion:fading_matter", ["projectex:fading_matter"]);

    event.shapeless("projectexpansion:magnum_star_ein", ["projectex:magnum_star_ein"]);
    event.shapeless("projectexpansion:magnum_star_zwei", ["projectex:magnum_star_zwei"]);
    event.shapeless("projectexpansion:magnum_star_drei", ["projectex:magnum_star_drei"]);
    event.shapeless("projectexpansion:magnum_star_vier", ["projectex:magnum_star_vier"]);
    event.shapeless("projectexpansion:magnum_star_sphere", ["projectex:magnum_star_sphere"]);
    event.shapeless("projectexpansion:magnum_star_omega", ["projectex:magnum_star_omega"]);

    event.shapeless("projectexpansion:colossal_star_ein", ["projectex:colossal_star_ein"]);
    event.shapeless("projectexpansion:colossal_star_zwei", ["projectex:colossal_star_zwei"]);
    event.shapeless("projectexpansion:colossal_star_drei", ["projectex:colossal_star_drei"]);
    event.shapeless("projectexpansion:colossal_star_vier", ["projectex:colossal_star_vier"]);
    event.shapeless("projectexpansion:colossal_star_sphere", ["projectex:colossal_star_sphere"]);
    event.shapeless("projectexpansion:colossal_star_omega", ["projectex:colossal_star_omega"]);
})