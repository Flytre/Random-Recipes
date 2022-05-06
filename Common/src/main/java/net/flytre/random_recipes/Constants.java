package net.flytre.random_recipes;

import net.flytre.flytre_lib.api.config.ConfigHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Constants {
    public static final String MOD_ID = "random_recipes";
    public static final String MOD_NAME = "Random Recipes";
    public static final Logger LOG = LogManager.getLogger(MOD_NAME);
    public static ConfigHandler<Config> RECIPE_CONFIG = new ConfigHandler<>(new Config(), "scrambled_recipes");

}