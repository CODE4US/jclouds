/**
 * Licensed to jclouds, Inc. (jclouds) under one or more
 * contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  jclouds licenses this file
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
package org.jclouds.hpcloud.objectstorage;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.net.URI;
import java.util.Set;

import org.jclouds.hpcloud.objectstorage.domain.ContainerCDNMetadata;
import org.jclouds.hpcloud.objectstorage.options.ListCDNContainerOptions;
import org.jclouds.openstack.swift.CommonSwiftClientLiveTest;
import org.jclouds.openstack.swift.domain.SwiftObject;
import org.testng.annotations.Test;

/**
 * 
 * @author Adrian Cole
 */
@Test(groups = "live", testName = "HPCloudObjectStorageClientLiveTest")
public class HPCloudObjectStorageClientLiveTest extends CommonSwiftClientLiveTest<HPCloudObjectStorageClient> {
   
   public HPCloudObjectStorageClientLiveTest(){
      provider = "hpcloud-objectstorage";
   }
   
   @Override
   public HPCloudObjectStorageClient getApi() {
      return view.unwrap(HPCloudObjectStorageApiMetadata.CONTEXT_TOKEN).getApi();
   }

   @Override
   protected void testGetObjectContentType(SwiftObject getBlob) {
      assertEquals(getBlob.getInfo().getContentType(), "application/x-www-form-urlencoded");
   }

   // CDN service due to go live Q1 2012
   @Test(enabled = true)
   public void testCDNOperations() throws Exception {
      final long minimumTTL = 60 * 60; // The minimum TTL is 1 hour

      // Create two new containers for testing
      final String containerNameWithCDN = getContainerName();
      final String containerNameWithoutCDN = getContainerName();
      try {
         try {
            getApi().getCDNExtension().get().disableCDN(containerNameWithCDN);
            getApi().getCDNExtension().get().disableCDN(containerNameWithoutCDN);
         } catch (Exception e) {
            e.printStackTrace();
         }
         ContainerCDNMetadata cdnMetadata = null;

         // Enable CDN with PUT for one container
         final URI cdnUri = getApi().getCDNExtension().get().enableCDN(containerNameWithCDN);
         assertTrue(cdnUri != null);

         // Confirm CDN is enabled via HEAD request and has default TTL
         cdnMetadata = getApi().getCDNExtension().get().getCDNMetadata(containerNameWithCDN);

         assertTrue(cdnMetadata.isCDNEnabled());

         assertEquals(cdnMetadata.getCDNUri(), cdnUri);

         cdnMetadata = getApi().getCDNExtension().get().getCDNMetadata(containerNameWithoutCDN);
         assert cdnMetadata == null || !cdnMetadata.isCDNEnabled() : containerNameWithoutCDN
                  + " should not have metadata";

         assert getApi().getCDNExtension().get().getCDNMetadata("DoesNotExist") == null;

         // List CDN metadata for containers, and ensure all CDN info is
         // available for enabled
         // container
         Set<ContainerCDNMetadata> cdnMetadataList = getApi().getCDNExtension().get().listCDNContainers();
         assertTrue(cdnMetadataList.size() >= 1);

         final long initialTTL = cdnMetadata.getTTL();
         assertTrue(cdnMetadataList.contains(new ContainerCDNMetadata(containerNameWithCDN, true, initialTTL, cdnUri)));

         /*
          * Test listing with options FIXFIX cdnMetadataList =
          * getApi().listCDNContainers(ListCDNContainerOptions.Builder.enabledOnly());
          * assertTrue(Iterables.all(cdnMetadataList, new Predicate<ContainerCDNMetadata>() { public
          * boolean apply(ContainerCDNMetadata cdnMetadata) { return cdnMetadata.isCDNEnabled(); }
          * }));
          */

         cdnMetadataList = getApi().getCDNExtension().get().listCDNContainers(
                  ListCDNContainerOptions.Builder.afterMarker(
                           containerNameWithCDN.substring(0, containerNameWithCDN.length() - 1)).maxResults(1));
         assertEquals(cdnMetadataList.size(), 1);

         // Enable CDN with PUT for the same container, this time with a custom
         // TTL
         long ttl = 4000;
         getApi().getCDNExtension().get().enableCDN(containerNameWithCDN, ttl);

         cdnMetadata = getApi().getCDNExtension().get().getCDNMetadata(containerNameWithCDN);

         assertTrue(cdnMetadata.isCDNEnabled());
   
         assertEquals(cdnMetadata.getTTL(), ttl);

         // Check POST by updating TTL settings
         ttl = minimumTTL;
         getApi().getCDNExtension().get().updateCDN(containerNameWithCDN, minimumTTL);

         cdnMetadata = getApi().getCDNExtension().get().getCDNMetadata(containerNameWithCDN);
         assertTrue(cdnMetadata.isCDNEnabled());

         assertEquals(cdnMetadata.getTTL(), minimumTTL);

         // Confirm that minimum allowed value for TTL is 3600, lower values are
         // ignored.
         getApi().getCDNExtension().get().updateCDN(containerNameWithCDN, 3599L);
         cdnMetadata = getApi().getCDNExtension().get().getCDNMetadata(containerNameWithCDN);
         assertEquals(cdnMetadata.getTTL(), 3599L);

         // Disable CDN with POST
         assertTrue(getApi().getCDNExtension().get().disableCDN(containerNameWithCDN));

         cdnMetadata = getApi().getCDNExtension().get().getCDNMetadata(containerNameWithCDN);
         assertEquals(cdnMetadata.isCDNEnabled(), false);
      } catch (Exception e) {
         e.printStackTrace();
      } finally {
         recycleContainer(containerNameWithCDN);
         recycleContainer(containerNameWithoutCDN);
      }
   }

}
