/**
 *
 * Copyright (C) 2010 Cloud Conscious, LLC. <info@cloudconscious.com>
 *
 * ====================================================================
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
 * ====================================================================
 */

package org.jclouds.deltacloud.xml;

import static org.testng.Assert.assertEquals;

import java.io.InputStream;
import java.net.URI;
import java.util.Set;

import org.jclouds.deltacloud.domain.Realm;
import org.jclouds.deltacloud.domain.RealmState;
import org.jclouds.http.functions.BaseHandlerTest;
import org.testng.annotations.Test;

import com.google.common.collect.ImmutableSet;

/**
 * Tests behavior of {@code RealmsHandler}
 * 
 * @author Adrian Cole
 */
@Test(groups = "unit")
public class RealmsHandlerTest extends BaseHandlerTest {

   @Test
   public void test() {
      InputStream is = getClass().getResourceAsStream("/test_list_realms.xml");
      Set<? extends Realm> expects = ImmutableSet.of(
            new Realm(URI.create("http://fancycloudprovider.com/api/realms/us"), "us", "United States", null,
                  RealmState.AVAILABLE), new Realm(URI.create("http://fancycloudprovider.com/api/realms/eu"), "eu",
                  "Europe", null, RealmState.AVAILABLE));
      // not sure why this isn't always automatically called from surefire.
      setUpInjector();
      assertEquals(factory.create(injector.getInstance(RealmsHandler.class)).parse(is), expects);
   }
}