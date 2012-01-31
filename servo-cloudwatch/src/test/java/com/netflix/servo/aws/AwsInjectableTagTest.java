/*
 * #%L
 * servo-cloudwatch
 * %%
 * Copyright (C) 2011 - 2012 Netflix
 * %%
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
 * #L%
 */
package com.netflix.servo.aws;

import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.net.URL;

import static org.testng.Assert.assertTrue;

/**
 * User: gorzell
 * Date: 1/9/12
 * Time: 9:11 PM
 */
public class AwsInjectableTagTest {

    @BeforeTest(groups = {"aws"})
    public void checkEc2() throws Exception {
        URL testEc2Url = new URL("http://169.254.169.254/latest/meta-data");
        testEc2Url.getContent();
    }

    @Test(groups = {"aws"})
    public void testGetZone() throws Exception {
        String zone = AwsInjectableTag.getZone();
        assertTrue(zone.startsWith("us-") || zone.startsWith("eu-"));
    }

    @Test(groups = {"aws"}, enabled = false)
    public void testAmiId() throws Exception {

        String amiId = AwsInjectableTag.getAmiId();
        assertTrue(amiId.startsWith("ami-"));
    }

    @Test(groups = {"aws"})
    public void testGetInstanceType() throws Exception {
        String instanceType = AwsInjectableTag.getInstanceType();
    }

    @Test(groups = {"aws"})
    public void testGetLocalHostname() throws Exception {
        String localHostname = AwsInjectableTag.getLocalHostname();
        assertTrue(localHostname.startsWith("domU-"));
    }

    @Test(groups = {"aws"})
    public void testGetLocalIpv4() throws Exception {
        String localIpv4 = AwsInjectableTag.getLocalIpv4();
    }

    @Test(groups = {"aws"})
    public void testGetPublicHostname() throws Exception {
        String publicHostname = AwsInjectableTag.getPublicHostname();
        assertTrue(publicHostname.startsWith("ec2-"));
    }

    @Test(groups = {"aws"})
    public void testGetPublicIpv4() throws Exception {
        String publicIpv4 = AwsInjectableTag.getPublicIpv4();
    }

    @Test(groups = {"aws"})
    public void testGetInstanceId() throws Exception {
        String instanceId = AwsInjectableTag.getInstanceId();
        assertTrue(instanceId.startsWith("i-"));
    }
}
