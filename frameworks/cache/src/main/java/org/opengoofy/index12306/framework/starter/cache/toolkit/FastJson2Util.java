/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.opengoofy.index12306.framework.starter.cache.toolkit;

import com.alibaba.fastjson2.util.ParameterizedTypeImpl;

import java.lang.reflect.Type;

/**
 * FastJson2 工具类
 */
/**
 * FastJson2Util 主要用于根据传入的 Type 类型数组，动态构建嵌套类型的 {@link java.lang.reflect.Type}，
 * 以便在 fastjson2 序列化与反序列化时能够正确处理如 List<T>、Map<K,V>、List<Map<K,V>> 等复杂泛型类型。
 *
 * 用途：
 * - 序列化和反序列化带有泛型的集合或对象时，传递具体的类类型，防止类型擦除带来的转换异常。
 * - 动态构建类型，在反射、缓存等场景下方便类型参数传递与类型正确性保证。
 *
 * 示例用法：
 * <pre>{@code
 *    // 构建 List<String> 的 Type
 *    Type type = FastJson2Util.buildType(List.class, String.class);
 *    // 构建 Map<String, Integer> 的 Type
 *    Type type = FastJson2Util.buildType(Map.class, String.class, Integer.class);
 * }</pre>
 */
public final class FastJson2Util {

    /**
     * 根据传入的 types 构建嵌套的 ParameterizedTypeImpl。
     * 常见用法如 List<T>、Map<K, V> 等泛型集合类型的构造。
     * 
     * @param types 类型参数，如 buildType(List.class, String.class)
     * @return 构建后的嵌套类型，便于 fastjson2 进行正确的序列化/反序列化
     */
    public static Type buildType(Type... types) {
        ParameterizedTypeImpl beforeType = null;
        if (types != null && types.length > 0) {
            if (types.length == 1) {
                // 构建单一类型
                return new ParameterizedTypeImpl(new Type[]{null}, null, types[0]);
            }
            // 从后向前依次包裹，形成多层嵌套泛型类型
            for (int i = types.length - 1; i > 0; i--) {
                beforeType = new ParameterizedTypeImpl(
                        new Type[]{beforeType == null ? types[i] : beforeType}, 
                        null, 
                        types[i - 1]
                ); 
            }
        }
        return beforeType;
    }
}
