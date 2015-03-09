/**
 * Copyright (c) 2007-2014 Kaazing Corporation. All rights reserved.
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.kaazing.gateway.server.test.config;

import java.security.KeyStore;

public abstract class SuppressibleSecurityConfiguration implements
        SuppressibleConfiguration {

    public abstract Suppressible<KeyStore> getKeyStore();

    public abstract void setKeyStore(Suppressible<KeyStore> keyStore);

    public abstract Suppressible<char[]> getKeyStorePassword();

    public abstract void setKeyStorePassword(Suppressible<char[]> keyStorePassword);

    public abstract Suppressible<KeyStore> getTrustStore();

    public abstract void setTrustStore(Suppressible<KeyStore> trustStore);

}