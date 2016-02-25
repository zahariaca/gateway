/**
 * Copyright 2007-2015, Kaazing Corporation. All rights reserved.
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
package org.kaazing.gateway.resource.address;

import static java.util.Arrays.asList;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utils class providing needed methods for performing network interface resolution
 *
 */
public final class ResolutionUtils {
    private static List<NetworkInterface> networkInterfaces = new ArrayList<NetworkInterface>();
    private static final Logger LOG = LoggerFactory.getLogger(ResolutionUtils.class);
    static {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                networkInterfaces.add(interfaces.nextElement());
            }
        } catch (SocketException socketEx) {
            LOG.debug("server", "Unable to resolve device URIs, processing URIs without device resolution.");
        }
    }

    private ResolutionUtils() {
        // not to be instantiated
     }

   /**
    * Method resolving host to InetAddresses
    * @param host
    * @return
    */
   public static Collection<InetAddress> getAllByName(String host, boolean allowIPv6) {
       Enumeration<NetworkInterface> networkInterfaces = cloneInterfaces(ResolutionUtils.networkInterfaces);
       List<String> resolvedAddresses = resolveDeviceAddress(host, networkInterfaces, allowIPv6);
       List<InetAddress> resolvedDeviceURIs = new ArrayList<InetAddress>();
       List<InetAddress> resolvedHosts = resolvedDeviceURIs;
       for (String resolvedAddress : resolvedAddresses) {
           try {
               resolvedHosts.addAll(asList(InetAddress.getAllByName(resolvedAddress)));
           } catch (UnknownHostException e) {
               e.printStackTrace();
           }
       }
       return resolvedHosts;
   }

   /**
    * Method performing device address resolution
    * @param deviceName
    * @param networkInterfaces
    * @param allowIPv6
    * @return
    */
   private static List<String> resolveDeviceAddress(String deviceName,
           Enumeration<NetworkInterface> networkInterfaces, boolean allowIPv6) {
        List<String> resolvedAddresses = new ArrayList<String>();
        if (deviceName.startsWith("[@") && deviceName.endsWith("]")) {
            deviceName = deviceName.substring(2, deviceName.lastIndexOf(']'));
        } else if (deviceName.startsWith("@")) {
            deviceName = deviceName.substring(1);
        }

        while (networkInterfaces.hasMoreElements()) {
            NetworkInterface networkInterface = networkInterfaces.nextElement();
            if (deviceName.toLowerCase().equals(networkInterface.getDisplayName().toLowerCase())) {
                Enumeration<InetAddress> inetAddresses = networkInterface.getInetAddresses();
                while (inetAddresses.hasMoreElements()) {
                    InetAddress inetAddress = inetAddresses.nextElement();
                    if (inetAddress instanceof Inet6Address) {
                        if (!allowIPv6) {
                            continue;
                        }
                        String inet6HostAddress = inetAddress.getHostAddress();
                        resolvedAddresses.add(String.format("[%s]", inet6HostAddress));
                    } else {
                        resolvedAddresses.add(inetAddress.getHostAddress());
                    }
                }
            }

            // add an internal URI for any sub interfaces that match the hostAddress
            Enumeration<NetworkInterface> subInterfaces = networkInterface.getSubInterfaces();
            if ((subInterfaces != null) && subInterfaces.hasMoreElements()) {
                resolvedAddresses.addAll(resolveDeviceAddress(deviceName, subInterfaces, allowIPv6));
            }
        }

        return resolvedAddresses;
    }

   /**
    * Method cloning list of strings (used for network interfaces)
    * @param interfaces
    * @return
    */
   private static Enumeration<NetworkInterface> cloneInterfaces(List<NetworkInterface> interfaces) {
       return Collections.enumeration(new ArrayList<>(interfaces));
   }

}
