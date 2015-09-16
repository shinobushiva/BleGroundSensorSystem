/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package me.shingaki.blesensorgroundsystem;

import java.util.HashMap;

/**
 * This class includes a small subset of standard GATT attributes for demonstration purposes.
 */
public class GroundSensorGattAttributes {
    private static HashMap<String, String> attributes = new HashMap();

    // 対象のサービスUUID.
    public static final String SERVICE_UUID = "713D0000-503E-4C75-BA94-3148F18D941E";
    // 温度キャラクタリスティックUUID.
    public static final String TEMPERATURE_CHARACTERISTIC_UUID = "713D0006-503E-4C75-BA94-3148F18D941E";
    // 湿度キャラクタリスティックUUID.
    public static final String HUMIDITY_CHARACTERISTIC_UUID = "713D0004-503E-4C75-BA94-3148F18D941E";
    // キャラクタリスティック設定UUID(固定値).
    public static final String CHARACTERISTIC_CONFIG_UUID = "00002902-0000-1000-8000-00805f9b34fb";

    static {
        // Sample Services.
        attributes.put(SERVICE_UUID, "Ground Sensor Service");

        // Sample Characteristics.
        attributes.put(TEMPERATURE_CHARACTERISTIC_UUID, "Temperature Measurement");
        attributes.put(HUMIDITY_CHARACTERISTIC_UUID, "Humidity Measurement");
    }

    public static String lookup(String uuid, String defaultName) {
        String name = attributes.get(uuid);
        return name == null ? defaultName : name;
    }
}
