package net.flytre.random_recipes;

import java.util.HashMap;
import java.util.Map;

public class Config {

    private Map<String, String> map;

    public Config() {
        this.map = new HashMap<>();
    }

    public Map<String, String> getMap() {
        return map;
    }

    public void setMap(Map<String, String> map) {
        this.map = map;
    }
}


