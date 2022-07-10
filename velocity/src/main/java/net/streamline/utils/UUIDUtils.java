package net.streamline.utils;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.streamline.base.Streamline;

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
        if (username.contains("-")) return username;

        try {
            String finalUsername = username.replace("\"", "").toLowerCase(Locale.ROOT);
            String uuid = cachedUUIDs.get(finalUsername);
            if (uuid != null && (uuid.contains("-") || uuid.equals("%"))) return uuid;
            cachedUUIDs.put(finalUsername, getUUID(finalUsername));
            return cachedUUIDs.get(finalUsername);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return "error";
    }

    public static String getCachedName(String uuid) {
        if (uuid.equals("%")) return uuid;
        if (! uuid.contains("-")) return uuid;

        try {
            String name = cachedNames.get(uuid);
            if (name != null && name.length() > 0) return name;
            cachedNames.put(uuid, getName(uuid));
            return cachedUUIDs.get(uuid);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return "error";
    }

    static public String getUUID(String username) {
        if (username.contains("-")) return getName(username);

        if (Streamline.getGeyserHolder().isPresent()) {
            String r = Streamline.getGeyserHolder().getUUID(username);
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

            JsonElement obj = new JsonParser().parse(JSONString);

            JsonObject jo = (JsonObject) obj;

            String id = jo.get("id").getAsString();

            String uuid = makeDashedUUID(id);

            return uuid;
            //return UUID.fromString(id);
        } catch (Exception e){
            e.printStackTrace();
        }
        return UUID.randomUUID().toString();
    }

    public static String getName(String uuid) {
        if (! uuid.contains("-")) return getUUID(uuid);

        if (Streamline.getGeyserHolder().isPresent()) {
            String r = Streamline.getGeyserHolder().getName(uuid);
            if (r != null) return r;
        }

        try {
            String JSONString = "";

            HttpURLConnection connection = (HttpURLConnection) new URL("https://api.mojang.com/user/profiles/" + uuid + "/names").openConnection();
            InputStream is = connection.getInputStream();

            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            String line = null;
            while ((line = br.readLine()) != null) {
                JSONString = line;
            }

            Object obj = new JsonParser().parse(JSONString);
            JsonArray jo = (JsonArray) obj;
            String last = jo.get(jo.size() - 1).toString();
            Object job = new JsonParser().parse(last);
            JsonObject njo = (JsonObject) job;

            return njo.get("name").toString();
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
