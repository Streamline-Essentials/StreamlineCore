package net.streamline.api.utils;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.streamline.api.SLAPI;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;
import java.util.TreeMap;
import java.util.UUID;

public class UUIDUtils {
    public static TreeMap<String, String> cachedUUIDs = new TreeMap<>();
    public static TreeMap<String, String> cachedNames = new TreeMap<>();

    public static String getCachedUUID(String username) {
        if (username.equals("%")) return username;
        if (! username.contains("-")) return username;

        if (cachedUUIDs.containsKey(username)) {
            return cachedUUIDs.get(username);
        } else {
            String name = getName(username);
            cachedUUIDs.put(username, name);
            return name;
        }
    }

    public static String getCachedName(String uuid) {
        if (uuid.equals("%")) return uuid;
        if (! uuid.contains("-")) return uuid;

        if (cachedNames.containsKey(uuid)) {
            return cachedNames.get(uuid);
        } else {
            String name = getName(uuid);
            cachedNames.put(uuid, name);
            return name;
        }
    }

    static public String getUUID(String username) {
        if (username.contains("-")) return getName(username);

        if (SLAPI.getInstance().getPlatform().getGeyserHolder().isPresent()) {
            String r = SLAPI.getInstance().getPlatform().getGeyserHolder().getUUID(username);
            if (r != null) return r;
        }

        username = username.toLowerCase(Locale.ROOT);
        try {
            String JSONString = "";

            HttpURLConnection connection = (HttpURLConnection) new URL("https://api.mojang.com/users/profiles/minecraft/" + username).openConnection();
            InputStream is = connection.getInputStream();

            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            String line;
            while ((line = br.readLine()) != null) {
                JSONString = line;
            }

            JsonElement obj = JsonParser.parseString(JSONString);

            JsonObject jo = (JsonObject) obj;

            String id = jo.get("id").getAsString();

            return makeDashedUUID(id);
            //return UUID.fromString(id);
        } catch (Exception e){
            e.printStackTrace();
        }
        return "error";
    }

    public static String getName(String uuid) {
        if (! uuid.contains("-")) return getUUID(uuid);

        if (SLAPI.getInstance().getPlatform().getGeyserHolder().isPresent()) {
            String r = SLAPI.getInstance().getPlatform().getGeyserHolder().getName(uuid);
            if (r != null) return r;
        }

        try {
            String JSONString = "";

            HttpURLConnection connection = (HttpURLConnection) new URL("https://api.mojang.com/user/profiles/" + uuid + "/names").openConnection();
            InputStream is = connection.getInputStream();

            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            String line;
            while ((line = br.readLine()) != null) {
                JSONString = line;
            }

            Object obj = JsonParser.parseString(JSONString);
            JsonArray jo = (JsonArray) obj;
            String last = jo.get(jo.size() - 1).toString();
            Object job = JsonParser.parseString(last);
            JsonObject njo = (JsonObject) job;

            return njo.get("name").toString().replace("\"", "");
        } catch (Exception e){
            e.printStackTrace();
        }
        return "error";
    }

    public static String makeDashedUUID(String unformatted){
        StringBuilder formatted = new StringBuilder();
        int i = 1;
        for (Character character : unformatted.toCharArray()){
            if (i == 9 || i == 13 || i == 17 || i == 21){
                formatted.append("-").append(character);
            } else {
                formatted.append(character);
            }
            i++;
        }

        return formatted.toString();
    }

    public static String swapUUID(String uuid){
        if (uuid.contains("-")){
            return stripUUID(uuid);
        } else {
            return makeDashedUUID(uuid);
        }
    }

    public static String stripUUID(String uuid) {
        return uuid.replace("-", "");
    }

    public static String swapToUUID(String thingThatMightBeAName){
        String uuid = thingThatMightBeAName;

        if (! thingThatMightBeAName.contains("-") && ! (thingThatMightBeAName.equals("%"))) {
            uuid = getCachedUUID(thingThatMightBeAName);
        }

        return uuid;
    }

    public static String swapToName(String thingThatMightBeAUUID){
        String name = thingThatMightBeAUUID;

        if (thingThatMightBeAUUID.equals("%")) {
            return thingThatMightBeAUUID;
        }

        if (thingThatMightBeAUUID.contains("-")) {
            name = getCachedName(thingThatMightBeAUUID);
        }

        return name;
    }
}
