/*   Copyright (c) 2015 Magnet Systems, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.magnet.mmx.tools;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.jsonSchema.JsonSchema;
import com.fasterxml.jackson.module.jsonSchema.factories.SchemaFactoryWrapper;
import com.google.common.collect.Multimap;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import org.reflections.Reflections;
import org.reflections.Store;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;


public class PojoToJsonSchema {

  private static HashMap<String, PojoSchema> schemaHashMap = new HashMap<String, PojoSchema>();
  public static void main(String[] args) {
    printAll(Thread.currentThread().getContextClassLoader());
  }

  public static void printAll(ClassLoader classLoader) {
    Reflections reflections = new Reflections("com.magnet.mmx.protocol");

    // iterate through all and print the JSON schema
    Store store = reflections.getStore();
    Map<String, Multimap<String, String>> map = store.getStoreMap();
    Set<String> entrySet = map.keySet();

    for (String key: entrySet) {
      Multimap<String, String> entries = map.get(key);
      if ("SubTypesScanner".equalsIgnoreCase(key)) {
        for (String key2: entries.keySet()) {
          // get the class and find its sub-types
          try {
            if (schemaHashMap.containsKey(key2)) {
              continue;
            }
            Class<?> clazz = classLoader.loadClass(key2);
            Set<? extends Class<?>> subTypes =
                reflections.getSubTypesOf(clazz);
            for (Class subType: subTypes) {
              if (schemaHashMap.containsKey(subType.getName())) {
                continue;
              }
              System.out.println();
              PojoSchema schema = getJsonSchemaFormat(subType);
              printSchema(schema);
              schemaHashMap.put(subType.getName(), schema);
            }

          } catch (ClassNotFoundException e) {
            e.printStackTrace();
          }
        }
      }
      System.out.println();
    }
  }

  public static class PojoProperty {
    String type;
  }
  public static class PojoSchema {
    String type;
    Map<String, PojoProperty> properties;

    @Override
    public String toString() {
      GsonBuilder gsonBuilder = new GsonBuilder();
      gsonBuilder.setPrettyPrinting();
      return gsonBuilder.create().toJson(this).toString();
    }
  }

  public static void printSchema(PojoSchema schema) {
    System.out.println(schema.toString());
  }
  public static PojoSchema getJsonSchemaFormat(Class<?> clazz) {

    PojoSchema schema = new PojoSchema();
    schema.properties = new HashMap<String, PojoProperty>();
    schema.type = clazz.getName();

    Field[] fields = clazz.getDeclaredFields();
    for (Field field: fields) {
      String fieldName;
      if (field.isAnnotationPresent(SerializedName.class)) {
        // pick up the serialized name
        SerializedName serializedName = field.getAnnotation(SerializedName.class);
        fieldName = serializedName.value();
      } else {
        fieldName = field.getName();
      }
      PojoProperty property = new PojoProperty();
      property.type = field.getType().getName();
      schema.properties.put(fieldName, property);
    }
    return schema;
  }

  /**
   * Jackson only works with proper "get" methods declared in the class object.
   * @param clazz
   * @return
   * @throws JsonProcessingException
   */
  public static String getJsonFormatWithJackson(Class<?> clazz) throws JsonProcessingException {
    ObjectMapper mapper = new ObjectMapper();
    SchemaFactoryWrapper visitor = new SchemaFactoryWrapper();
    mapper.acceptJsonFormatVisitor(clazz, visitor);
    JsonSchema schema = visitor.finalSchema();
    return (mapper.writerWithDefaultPrettyPrinter().writeValueAsString(schema));
  }

}
