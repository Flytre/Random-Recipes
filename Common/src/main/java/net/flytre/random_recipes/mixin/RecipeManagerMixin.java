package net.flytre.random_recipes.mixin;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonElement;
import net.flytre.random_recipes.Config;
import net.flytre.random_recipes.Constants;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.*;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.profiler.Profiler;
import org.apache.logging.log4j.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.*;

@Mixin(value = RecipeManager.class, priority = 1000000)
public class RecipeManagerMixin {


    @Unique
    private static final Set<RecipeType<?>> SCRAMBLED_RECIPE_TYPES = new HashSet<>(Arrays.asList(RecipeType.CRAFTING, RecipeType.SMELTING, RecipeType.BLASTING, RecipeType.SMOKING));

    @Shadow
    private Map<RecipeType<?>, Map<Identifier, Recipe<?>>> recipes;

    private static ShapedRecipe withOutput(ShapedRecipe recipe, ItemStack output) {
        return new ShapedRecipe(recipe.getId(), "", recipe.getWidth(), recipe.getHeight(), recipe.getIngredients(), output);
    }

    private static ShapelessRecipe withOutput(ShapelessRecipe recipe, ItemStack output) {
        return new ShapelessRecipe(recipe.getId(), "", output, recipe.getIngredients());
    }

    private static SmeltingRecipe withOutput(SmeltingRecipe recipe, ItemStack output) {
        return new SmeltingRecipe(recipe.getId(), "", recipe.getIngredients().get(0), output, recipe.getExperience(), recipe.getCookTime());
    }

    private static SmokingRecipe withOutput(SmokingRecipe recipe, ItemStack output) {
        return new SmokingRecipe(recipe.getId(), "", recipe.getIngredients().get(0), output, recipe.getExperience(), recipe.getCookTime());
    }

    private static BlastingRecipe withOutput(BlastingRecipe recipe, ItemStack output) {
        return new BlastingRecipe(recipe.getId(), "", recipe.getIngredients().get(0), output, recipe.getExperience(), recipe.getCookTime());
    }

    private static <K, V> void shuffleMap(Map<K, V> map) {
        List<V> valueList = new ArrayList<>(map.values());
        Collections.shuffle(valueList);
        Iterator<V> valueIt = valueList.iterator();
        for (Map.Entry<K, V> e : map.entrySet()) {
            e.setValue(valueIt.next());
        }
    }

    private static void toConfig(Map<Recipe<?>, Recipe<?>> scrambled) {
        Config cfg = Constants.RECIPE_CONFIG.getConfig();
        Map<String, String> cfgMap = new HashMap<>();
        for (Map.Entry<Recipe<?>, Recipe<?>> entry : scrambled.entrySet()) {
            cfgMap.put(entry.getKey().getId().toString(), entry.getValue().getId().toString());
        }
        cfg.setMap(cfgMap);
        Constants.RECIPE_CONFIG.save(cfg);
    }

    @Inject(method = "apply", at = @At("RETURN"), require = 1)
    public void random_recipes$scramble(Map<Identifier, JsonElement> map, ResourceManager resourceManager, Profiler profiler, CallbackInfo ci) {

        Constants.LOG.log(Level.INFO,"Started Scrambling Recipes!");

        Constants.RECIPE_CONFIG.handle();
        Config cfg = Constants.RECIPE_CONFIG.getConfig();
        Map<Recipe<?>, Recipe<?>> scrambled = cfg.getMap().size() == 0 ? scramble() : fromString(cfg.getMap());
        applyScramble(scrambled);

        Constants.LOG.log(Level.INFO,"Finished Scrambling Recipes!");

    }

    private Recipe<?> fromId(Identifier id) {
        for (Map<Identifier, Recipe<?>> values : this.recipes.values()) {
            if (values.containsKey(id))
                return values.get(id);
        }
        throw new RuntimeException("Recipe with id " + id + " does not exist!");
    }

    private Map<Recipe<?>, Recipe<?>> fromString(Map<String, String> string) {
        Map<Recipe<?>, Recipe<?>> vals = new HashMap<>();
        for (Map.Entry<String, String> entry : string.entrySet()) {
            Recipe<?> key = fromId(new Identifier(entry.getKey()));
            Recipe<?> val = fromId(new Identifier(entry.getValue()));
            vals.put(key, val);
        }
        return vals;
    }

    private Map<Recipe<?>, Recipe<?>> scramble() {
        Map<RecipeType<?>, Map<Identifier, Recipe<?>>> old = this.recipes;

        //Get all outputs to scramble
        Map<Recipe<?>, Recipe<?>> recipeOutputMap = new HashMap<>();

        for (RecipeType<?> type : SCRAMBLED_RECIPE_TYPES) {
            for (Recipe<?> recipe : old.get(type).values())
                if (recipe instanceof ShapedRecipe || recipe instanceof ShapelessRecipe || recipe instanceof SmeltingRecipe || recipe instanceof BlastingRecipe || recipe instanceof SmokingRecipe)
                    recipeOutputMap.put(recipe, recipe);
        }

        //Actually scramble them
        shuffleMap(recipeOutputMap);
        toConfig(recipeOutputMap);

        return recipeOutputMap;
    }
    

    private void applyScramble(Map<Recipe<?>, Recipe<?>> recipeOutputMap) {
        Map<RecipeType<?>, Map<Identifier, Recipe<?>>> old = this.recipes;


        Set<Recipe<?>> shuffledRecipes = new HashSet<>();
        for (Map.Entry<Recipe<?>, Recipe<?>> entry : recipeOutputMap.entrySet())
            if (entry.getKey() instanceof ShapedRecipe)
                shuffledRecipes.add(withOutput((ShapedRecipe) entry.getKey(), entry.getValue().getOutput()));
            else if (entry.getKey() instanceof ShapelessRecipe)
                shuffledRecipes.add(withOutput((ShapelessRecipe) entry.getKey(), entry.getValue().getOutput()));
            else if (entry.getKey() instanceof SmeltingRecipe)
                shuffledRecipes.add(withOutput((SmeltingRecipe) entry.getKey(), entry.getValue().getOutput()));
            else if (entry.getKey() instanceof BlastingRecipe)
                shuffledRecipes.add(withOutput((BlastingRecipe) entry.getKey(), entry.getValue().getOutput()));
            else if (entry.getKey() instanceof SmokingRecipe)
                shuffledRecipes.add(withOutput((SmokingRecipe) entry.getKey(), entry.getValue().getOutput()));


        //map them back to identifiers and save?
        Map<RecipeType<?>, Map<Identifier, Recipe<?>>> copy = new HashMap<>(old);

        for (RecipeType<?> type : SCRAMBLED_RECIPE_TYPES) {
            Map<Identifier, Recipe<?>> current = old.get(type);
            Map<Identifier, Recipe<?>> remapped = new HashMap<>(current);
            for (Recipe<?> recipe : shuffledRecipes)
                if (recipe.getType() == type)
                    remapped.put(recipe.getId(), recipe);
            copy.put(type, remapped);
        }
        this.recipes = ImmutableMap.copyOf(copy);

    }
}
