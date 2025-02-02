package de.tu_darmstadt.seemoo.nfcgate.util;

import android.content.Context;
import android.os.Build;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import de.tu_darmstadt.seemoo.nfcgate.R;

public final class DeviceNames {
    private final String mJsonByDevice;

    public DeviceNames(Context context) {
        mJsonByDevice = loadJsonByDevice(context);
    }

    public String formatCurrentDeviceName() {
        return formatDeviceName(Build.DEVICE, Build.MODEL);
    }

    public String formatDeviceName(String deviceName, String modelName) {
        String marketName = getMarketName(deviceName, modelName);
        if (marketName == null || modelName.equalsIgnoreCase(marketName))
            return String.format("%s [%s]", modelName, deviceName);

        return String.format("%s [%s] (%s)", modelName, deviceName, marketName);
    }

    public String getMarketName(String deviceName, String modelName) {
        JSONArray devices = findDevices(deviceName);
        if (devices != null)
            return findMarketName(devices, modelName);

        return null;
    }

    private String loadJsonByDevice(Context context) {
        // read the full resource as a string
        try (InputStream is = context.getResources().openRawResource(R.raw.by_device)) {
            return new String(FileUtils.readAllBytes(is), StandardCharsets.UTF_8);
        } catch (IOException ignored) { }

        return "";
    }

    /// look for device entries matching the device name in the map (multiple matches possible)
    private JSONArray findDevices(String searchDeviceName) {
        JSONTokener tokener = new JSONTokener(mJsonByDevice);

        /*
         * Traverse huge JSON object efficiently by only loading small
         * chunks at a time into memory.
         *
         * Example snippet from the JSON object:
         * "bullhead": [
         *  {
         *   "brand": "Blackshark",
         *   "name": "Shark 1S",
         *   "model": "AWM-A0"
         *  },
         *  {
         *   "brand": "LGE",
         *   "name": "Nexus 5X",
         *   "model": "Nexus 5X"
         *  }
         * ],
         */

        try {
            // enter the root JSON object
            tokener.skipPast("{");
            // while there are devices in the map
            while (tokener.more()) {
                // parse the object key, a quoted string (e.g. "bullhead")
                String deviceName = (String) tokener.nextValue();
                // skip to object value
                tokener.skipPast(":");
                // parse the object value, a JSON array
                JSONArray devices = (JSONArray) tokener.nextValue();
                // skip to next object key (or exhaust the input)
                tokener.skipPast(",");

                // if the object key matches the device name, return the object value
                if (searchDeviceName.equalsIgnoreCase(deviceName))
                    return devices;
            }
        }
        catch (JSONException ignored) { }

        // null on error or not found
        return null;
    }

    /// find one device market name matching the model in device entries returned by findDevices
    private String findMarketName(JSONArray devices, String searchModel) {
        try {
            for (int i = 0; i < devices.length(); i++) {
                JSONObject device = devices.getJSONObject(i);

                // skip malformed device entries
                if (device.has("name") && device.has("model")) {
                    String name = device.getString("name");
                    String model = device.getString("model");

                    // if the model matches and the market name is valid, return this market name
                    if (searchModel.equalsIgnoreCase(model) && !name.isBlank())
                        return name;
                }
            }
        }
        catch (JSONException ignored) { }

        // null on error or not found
        return null;
    }
}
