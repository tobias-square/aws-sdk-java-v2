/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.awssdk.protocol.tests;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.protocol.ProtocolTestSuiteLoader;
import software.amazon.awssdk.protocol.runners.ProtocolTestRunner;

public class Ec2ProtocolTest extends ProtocolTestBase {

    private static final ProtocolTestSuiteLoader testSuiteLoader = new ProtocolTestSuiteLoader();
    private static ProtocolTestRunner testRunner;

    @BeforeAll
    public static void setupFixture() {
        testRunner = new ProtocolTestRunner("/models/ec2-2016-03-11-intermediate.json");
    }

    @Test
    public void run() throws Exception {
        testRunner.runTests(testSuiteLoader.load("ec2-suite.json"));
    }
}
