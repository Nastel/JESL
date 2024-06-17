/*
 * Copyright 2014-2024 JKOOL, LLC.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * JPMS definition for {@code "com.jkoolcloud.jesl"} module.
 */
module com.jkoolcloud.jesl {
    requires java.base;
    requires transitive java.xml;
    requires com.jkoolcloud.tnt4j.core;
    requires org.apache.commons.codec;
    requires org.apache.commons.lang3;
    requires org.apache.commons.text;
    requires commons.beanutils;
    requires org.apache.httpcomponents.core5.httpcore5;
    requires org.apache.httpcomponents.client5.httpclient5;

    exports com.jkoolcloud.jesl.net;
    exports com.jkoolcloud.jesl.net.http;
    exports com.jkoolcloud.jesl.net.http.apache;
    exports com.jkoolcloud.jesl.net.security;
    exports com.jkoolcloud.jesl.net.socket;
    exports com.jkoolcloud.jesl.net.ssl;
    exports com.jkoolcloud.jesl.simulator;
    exports com.jkoolcloud.jesl.simulator.tnt4j;
    exports com.jkoolcloud.jesl.tnt4j.sink;
}