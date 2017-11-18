package cookiework.encryptedvideoview.util;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;

/**
 * Created by Administrator on 2017/01/12.
 */

public class JsonUtil {
    public static <T> T convertJsonToObject(String json, Class<? extends T> targetClass){
        Gson gson = new Gson();
        return gson.fromJson(json, targetClass);
    }

    public static <T> ArrayList<T> convertJsonToArray(String json, Class<? extends T> targetClass){
        Gson gson = new Gson();
        JsonParser parser = new JsonParser();
        JsonArray jsonArray = parser.parse(json).getAsJsonArray();
        ArrayList<T> array = new ArrayList<T>(jsonArray.size());
        for(JsonElement element : jsonArray){
            array.add(gson.fromJson(element, targetClass));
        }
        return array;
    }

    public static JsonObject getJsonObj(String json){
        Gson gson = new Gson();
        JsonParser parser = new JsonParser();
        JsonObject jsonObject = parser.parse(json).getAsJsonObject();
        return jsonObject;
    }
}
