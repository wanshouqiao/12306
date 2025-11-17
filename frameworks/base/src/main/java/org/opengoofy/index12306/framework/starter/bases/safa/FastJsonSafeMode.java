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

package org.opengoofy.index12306.framework.starter.bases.safa;

import org.springframework.beans.factory.InitializingBean;

/**
 * FastJson安全模式，开启后关闭类型隐式传递
 * 什么是类型隐式传递？ 举个例子：
 * 比如你有一个 Person 类，里面有 name 和 age 两个属性，
 * 然后你用 FastJson 将 Person 对象序列化成 JSON 字符串，
 * 然后你再用 FastJson 将 JSON 字符串反序列化成 Person 对象，
 * 这个时候，如果你没有开启安全模式，那么你就可以通过反射的方式
 * 访问 Person 对象的 name 和 age 属性，这就是类型隐式传递。
 * 开启安全模式后，你就可以禁止这种行为，从而防止安全问题。
 * 难道不能访问name和age吗？不能，因为name和age是私有的，你不能直接访问。
 * 但是你可以通过反射的方式访问，这就是类型隐式传递。
 * 开启安全模式后，你就可以禁止这种行为，从而防止安全问题。
 */
public class FastJsonSafeMode implements InitializingBean {

    @Override
    public void afterPropertiesSet() throws Exception {
        System.setProperty("fastjson2.parser.safeMode", "true");
    }
}
