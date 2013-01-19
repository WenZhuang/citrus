/*
 * Copyright (c) 2002-2013 Alibaba Group Holding Limited.
 * All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alibaba.citrus.springext.support;

import static com.alibaba.citrus.util.CollectionUtil.*;
import static org.junit.Assert.*;

import java.util.List;

import com.alibaba.citrus.springext.ConfigurationPoint;
import com.alibaba.citrus.springext.Contribution;
import com.alibaba.citrus.springext.support.SpringExtSchemaSet.ConfigurationPointItem;
import com.alibaba.citrus.springext.support.SpringExtSchemaSet.NamespaceItem;
import com.alibaba.citrus.springext.support.SpringExtSchemaSet.SpringPluggableItem;
import com.alibaba.citrus.test.TestEnvStatic;
import org.junit.Before;
import org.junit.Test;

public class SpringExtSchemaSetTests {
    private SpringExtSchemaSet           schemas;
    private List<SpringPluggableItem>    springItems;
    private List<SpringPluggableItem>    noSchemaItems;
    private List<ConfigurationPointItem> configurationPointItems;

    static {
        TestEnvStatic.init();
    }

    @Before
    public void init() {
        schemas = new SpringExtSchemaSet("TEST-INF/test14/cps");

        NamespaceItem[] items = schemas.getIndependentItems();

        springItems = filter(SpringPluggableItem.class, items, true);
        noSchemaItems = filter(SpringPluggableItem.class, items, false);
        configurationPointItems = filter(ConfigurationPointItem.class, items, true);
    }

    @Test
    public void dependingContributions() {
        // 依赖关系：
        // a1 -> b, c
        // b1 -> c
        // c1 -> a, h
        // d1 -> d
        // e1 -> f
        // f1 -> g
        // f2 -> h
        // g1
        assertConfigurationPointDependencies("a", "c");
        assertConfigurationPointDependencies("b", "a");
        assertConfigurationPointDependencies("c", "a", "b");
        assertConfigurationPointDependencies("d", "d");
        assertConfigurationPointDependencies("e");
        assertConfigurationPointDependencies("f", "e");
        assertConfigurationPointDependencies("g", "f");
        assertConfigurationPointDependencies("h", "c", "f");
    }

    @Test
    public void springItems() {
        boolean found = false;

        for (SpringPluggableItem item : springItems) {
            if (item.getNamespace().equals("http://www.springframework.org/schema/beans")) {
                found = true;
            }
        }

        assertTrue(found);
    }

    @Test
    public void noSchemaItems() {
        boolean found = false;

        for (SpringPluggableItem item : noSchemaItems) {
            if (item.getNamespace().equals("http://www.springframework.org/schema/p")) {
                found = true;
            }
        }

        assertTrue(found);
    }

    @Test
    public void configurationPointItems() {
        assertEquals(3, configurationPointItems.size());
        assertEquals("http://localhost/b {\n" +
                     "  b1 {\n" +
                     "    http://localhost/c {\n" +
                     "      c1 {\n" +
                     "        http://localhost/a\n" +
                     "        http://localhost/h\n" +
                     "      }\n" +
                     "    }\n" +
                     "  }\n" +
                     "}", configurationPointItems.get(0).dump());

        assertEquals("http://localhost/d", configurationPointItems.get(1).dump());

        assertEquals("http://localhost/e {\n" +
                     "  e1 {\n" +
                     "    http://localhost/f {\n" +
                     "      f1 {\n" +
                     "        http://localhost/g\n" +
                     "      }\n" +
                     "\n" +
                     "      f2 {\n" +
                     "        http://localhost/h\n" +
                     "      }\n" +
                     "    }\n" +
                     "  }\n" +
                     "}", configurationPointItems.get(2).dump());
    }

    private <I> List<I> filter(Class<I> type, NamespaceItem[] items, boolean withSchemas) {
        List<I> list = createLinkedList();

        for (NamespaceItem item : items) {
            if (type.isInstance(item) && withSchemas == !item.getSchemas().isEmpty()) {
                list.add(type.cast(item));
            }
        }

        return list;
    }

    private void assertConfigurationPointDependencies(String cpName, String... dependings) {
        ConfigurationPoint cp = schemas.getConfigurationPoints().getConfigurationPointByName(cpName);
        List<String> depList = createLinkedList();

        for (Contribution contribution : cp.getDependingContributions()) {
            depList.add(contribution.getConfigurationPoint().getName());
        }

        assertArrayEquals(dependings, depList.toArray());
    }
}