/**
 * Copyright 2013 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.netflix.servo.tag.aws;

import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.testng.Assert.assertTrue;

/**
 * AwsInjectableTag tests.
 * User: gorzell
 * Date: 1/9/12
 * Time: 9:11 PM
 */
public class AwsInjectableTagTest {
    /**
     * getContent from 169.254.169.254.
     */
    @BeforeTest(groups = { "aws" })
    public void checkEc2() throws Exception {
        URL testEc2Url = new URL("http://169.254.169.254/latest/meta-data");
        testEc2Url.getContent();
    }

    /**
     * zone comes from a valid region.
     */
    @Test(groups = { "aws" })
    public void testGetZone() throws Exception {
        String zone = AwsInjectableTag.getZone();
        assertTrue(zone.startsWith("us-") || zone.startsWith("eu-"));
    }

    /**
     * ami-id looks like a valid ami.
     */
    @Test(groups = { "aws" }, enabled = false)
    public void testAmiId() throws Exception {

        String amiId = AwsInjectableTag.getAmiId();
        assertTrue(amiId.startsWith("ami-"));
    }

    /**
     * check instance type.
     */
    @Test(groups = { "aws" })
    public void testGetInstanceType() throws Exception {
        String instanceType = AwsInjectableTag.getInstanceType();
        assertTrue(instanceType != null);
    }

    /**
     * localHostname is a domU.
     */
    @Test(groups = { "aws" })
    public void testGetLocalHostname() throws Exception {
        String localHostname = AwsInjectableTag.getLocalHostname();
        assertTrue(localHostname.startsWith("domU-"));
    }

    /**
     * privateIp.
     */
    @Test(groups = { "aws" })
    public void testGetLocalIpv4() throws Exception {
        String localIpv4 = AwsInjectableTag.getLocalIpv4();
        assertTrue(looksLikeAnIp(localIpv4));
    }

    /**
     * publicHostname.
     */
    @Test(groups = { "aws" })
    public void testGetPublicHostname() throws Exception {
        String publicHostname = AwsInjectableTag.getPublicHostname();
        assertTrue(publicHostname.startsWith("ec2-"));
    }

    /**
     * publicIp.
     */
    @Test(groups = { "aws" })
    public void testGetPublicIpv4() throws Exception {
        String publicIpv4 = AwsInjectableTag.getPublicIpv4();
        assertTrue(looksLikeAnIp(publicIpv4));
    }

    /**
     * instanceId.
     */
    @Test(groups = { "aws" })
    public void testGetInstanceId() throws Exception {
        String instanceId = AwsInjectableTag.getInstanceId();
        assertTrue(instanceId.startsWith("i-"));
    }

    private static final String IPADDRESS_REGEX = "^([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\."
        + "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\."
        + "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\."
        + "([01]?\\d\\d?|2[0-4]\\d|25[0-5])$";
    private static final Pattern IP_PATTERN = Pattern.compile(IPADDRESS_REGEX);

    /**
     * Helper function to check whether a string looks like an IP.
     */
    private boolean looksLikeAnIp(String ip) {
        Matcher matcher = IP_PATTERN.matcher(ip);
        return matcher.matches();
    }
}
