/**
 * Copyright 2007-2016, Kaazing Corporation. All rights reserved.
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
package org.kaazing.gateway.service.turn.rest;

import static java.nio.charset.Charset.forName;
import static org.kaazing.test.util.ITUtil.createRuleChain;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.security.KeyStore;

import javax.crypto.spec.SecretKeySpec;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.kaazing.gateway.server.test.GatewayRule;
import org.kaazing.gateway.server.test.config.GatewayConfiguration;
import org.kaazing.gateway.server.test.config.builder.GatewayConfigurationBuilder;
import org.kaazing.gateway.util.feature.EarlyAccessFeatures;
import org.kaazing.k3po.junit.annotation.Specification;
import org.kaazing.k3po.junit.rules.K3poRule;

public class TurnRestIT {
    private static final String ACCEPT_URL = "http://localhost:8000/";

    private final K3poRule robot = new K3poRule();

    private final GatewayRule gateway = new GatewayRule() {
        {
            KeyStore keyStore = null;
            File turnPasswordFile = null;
            char[] password = "ab987c".toCharArray();
            try {
                FileInputStream fileInStr =
                        new FileInputStream(System.getProperty("user.dir") + "/target/truststore/keystore.db");
                keyStore = KeyStore.getInstance("JCEKS");
                keyStore.load(fileInStr, "ab987c".toCharArray());
                keyStore.setKeyEntry("turn.shared.secret",
                        new SecretKeySpec("turnAuthenticationSharedSecret".getBytes(forName("UTF-8")), "PBEWithMD5AndDES"),
                        "1234567".toCharArray(), null);
                turnPasswordFile = new File(System.getProperty("user.dir") + "/target/truststore/turnstore.db");
                try (BufferedWriter bw = new BufferedWriter(new FileWriter(turnPasswordFile))) {
                    bw.write("1234567");
                }
            }
            catch (Exception e) {
                e.printStackTrace();
            }
            // @formatter:off
            GatewayConfiguration configuration =
                    new GatewayConfigurationBuilder()
                        .property(EarlyAccessFeatures.TURN_REST_SERVICE.getPropertyName(), "true")
                        .service()
                            .accept(ACCEPT_URL)
                            .type("turn.rest")
                            .realmName("turn")
                                .authorization()
                                .requireRole("username")
                            .done()

                            .property("generate.credentials", "class:" + DefaultCredentialsGenerator.class.getName())
                            .property("key.alias", "turn.shared.secret")
                            .property("key.password-file", turnPasswordFile.getPath())
                            .property("key.algorithm", "HmacSHA1")
                            .nestedProperty("uris")
                                .property("uri", "uri1")
                                .property("uri", "uri2")
                                .property("uri", "uri3")
                            .done()
                            .nestedProperty("options")
                                .property("credentials.ttl", "43200")
                                .property("username.separator", ":")
                            .done()
                        .done()
                        .security()
                            .keyStore(keyStore)
                            .keyStorePassword(password)
                            .realm()
                                .name("turn")
                                .description("TURN REST Login Module Test")
                                .httpChallengeScheme("Basic")
                                .loginModule()
                                    .type("class:" + TestLoginModule.class.getName())
                                    .success("requisite")
                                    .option("roles", "username")
                                .done()
                            .done()
                        .done()
                    .done();
            // @formatter:on
            init(configuration);
        }
    };

    @Rule
    public TestRule chain = createRuleChain(gateway, robot);
    
    @Specification("generate.valid.response")
    @Test
    public void generateValidResponse() throws Exception {
        robot.finish();
    }
    
    @Specification("invalid.service.parameter")
    @Test
    public void closeOnInvalidServiceParameter() throws Exception {
        robot.finish();
    }
}
