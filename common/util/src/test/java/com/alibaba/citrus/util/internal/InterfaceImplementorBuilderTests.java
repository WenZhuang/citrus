/*
 * Copyright (c) 2002-2012 Alibaba Group Holding Limited.
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

package com.alibaba.citrus.util.internal;

import static com.alibaba.citrus.test.TestUtil.*;
import static org.junit.Assert.*;

import java.io.IOException;
import java.io.Serializable;
import java.net.URL;
import java.net.URLClassLoader;
import javax.servlet.ServletContext;

import org.junit.Test;

public class InterfaceImplementorBuilderTests {
    private InterfaceImplementorBuilder builder;

    @Test
    public void classLoader() {
        ClassLoader cl;

        // default class loader
        cl = Thread.currentThread().getContextClassLoader();
        builder = new InterfaceImplementorBuilder().addInterface(Serializable.class).setOverrider(new Object());
        assertSame(cl, builder.getClassLoader());
        assertSame(cl, builder.toObject().getClass().getClassLoader());

        // specified class loader
        cl = new URLClassLoader(new URL[0]);
        builder = new InterfaceImplementorBuilder(cl).addInterface(Serializable.class).setOverrider(new Object());
        assertSame(cl, builder.getClassLoader());
        assertSame(cl, builder.toObject().getClass().getClassLoader());
    }

    @Test
    public void interfaces() {
        try {
            new InterfaceImplementorBuilder().toObject();
            fail();
        } catch (IllegalArgumentException e) {
            assertThat(e, exception("no interface specified"));
        }
    }

    @Test
    public void baseObject() {
        // no baseObject
        ServletContext sc = (ServletContext) new InterfaceImplementorBuilder().addInterface(ServletContext.class).setOverrider(new Object()).toObject();

        try {
            sc.getAttribute("key");
            fail();
        } catch (UnsupportedOperationException e) {
            assertThat(e, exception("ServletContext.getAttribute(String)"));
        }

        // wrong type of baseObject
        try {
            new InterfaceImplementorBuilder().addInterface(Runnable.class).setBaseObject("string").setOverrider(new Object()).toObject();
            fail();
        } catch (IllegalArgumentException e) {
            assertThat(e, exception("string is not of interface java.lang.Runnable"));
        }

        try {
            new InterfaceImplementorBuilder().addInterface(Runnable.class).addInterface(ServletContext.class).setBaseObject(new Runnable() {
                public void run() {
                }

                @Override
                public String toString() {
                    return "myrunnable";
                }
            }).setOverrider(new Object()).toObject();
            fail();
        } catch (IllegalArgumentException e) {
            assertThat(e, exception("myrunnable is not of interface javax.servlet.ServletContext"));
        }
    }

    @Test
    public void overrider() {
        // no overrider
        try {
            new InterfaceImplementorBuilder().addInterface(ServletContext.class).toObject();
        } catch (IllegalArgumentException e) {
            assertThat(e, exception("no overrider specified"));
        }
    }

    @Test
    public void setOverriderSetProxyObjectMethodName() {
        final Object[] holder = new Object[1];

        // default method
        ServletContext sc = (ServletContext) new InterfaceImplementorBuilder().addInterface(ServletContext.class).setOverrider(new Object() {
            public void setThisProxy(ServletContext proxy) {
                holder[0] = proxy;
            }
        }).toObject();

        assertSame(holder[0], sc);

        // specified method name
        sc = (ServletContext) new InterfaceImplementorBuilder().addInterface(ServletContext.class).setOverrider(new Object() {
            public void setMyProxy(ServletContext proxy) {
                holder[0] = proxy;
            }
        }).setOverriderSetProxyObjectMethodName("setMyProxy").toObject();

        assertSame(holder[0], sc);

        // super param type
        sc = (ServletContext) new InterfaceImplementorBuilder().addInterface(ServletContext.class).setOverrider(new Object() {
            public void setThisProxy(Object proxy) {
                holder[0] = proxy;
            }
        }).toObject();

        assertSame(holder[0], sc);

        // wrong param type
        holder[0] = null;
        sc = (ServletContext) new InterfaceImplementorBuilder().addInterface(ServletContext.class).setOverrider(new Object() {
            public void setThisProxy(String proxy) {
                holder[0] = proxy;
            }
        }).toObject();

        assertSame(null, holder[0]);

        // no param type
        holder[0] = null;
        sc = (ServletContext) new InterfaceImplementorBuilder().addInterface(ServletContext.class).setOverrider(new Object() {
            public void setThisProxy() {
            }
        }).toObject();

        assertSame(null, holder[0]);

        // call failed
        sc = (ServletContext) new InterfaceImplementorBuilder().addInterface(ServletContext.class).setOverrider(new Object() {
            public void setThisProxy(Object proxy) {
                throw new IllegalArgumentException();
            }
        }).toObject();

        assertSame(null, holder[0]);
    }

    @Test
    public void toObject() {
        final Object[] holder = new Object[1];

        builder = new InterfaceImplementorBuilder().addInterface(ServletContext.class).setOverrider(new Object() {
            public void setThisProxy(Object proxy) {
                holder[0] = proxy;
            }
        });

        ServletContext sc1 = (ServletContext) builder.toObject();
        assertSame(sc1, holder[0]);

        ServletContext sc2 = (ServletContext) builder.toObject();
        assertSame(sc2, holder[0]);

        assertNotSame(sc1, sc2);
        assertSame(sc1.getClass(), sc2.getClass());
    }

    @Test
    public void invokeSuper() {
        Runnable newObject = (Runnable) new InterfaceImplementorBuilder().addInterface(Runnable.class).setBaseObject(new Runnable() {
            public void run() {
            }

            @Override
            public int hashCode() {
                return 123;
            }

            @Override
            public boolean equals(Object obj) {
                return true;
            }

            @Override
            public String toString() {
                return "haha";
            }
        }).setOverrider(new Object()).toObject();

        assertFalse(newObject.equals(""));
        assertFalse(123 == newObject.hashCode());
        assertFalse("haha".equals(newObject.toString()));
    }

    public static interface MyInterface1 {
        String getName();

        void throwException(Throwable e) throws Throwable;
    }

    @Test
    public void invokeBaseObject() {
        MyInterface1 newObject = (MyInterface1) new InterfaceImplementorBuilder().addInterface(MyInterface1.class).setBaseObject(new MyInterface1() {
            public String getName() {
                return "myname";
            }

            public void throwException(Throwable e) throws Throwable {
                throw e;
            }
        }).setOverrider(new Object()).toObject();

        assertEquals("myname", newObject.getName());

        try {
            newObject.throwException(new IllegalArgumentException());
            fail();
        } catch (Throwable e) {
            assertTrue(e instanceof IllegalArgumentException);
        }

        try {
            newObject.throwException(new IOException());
            fail();
        } catch (Throwable e) {
            assertTrue(e instanceof IOException);
        }
    }

    public static interface MyInterface2 {
        String getName();

        String getName(String s);

        String getName(Object o, String s);

        void throwException(Throwable e) throws Throwable;
    }

    @Test
    public void invokeOverrider() {
        MyInterface2 baseObject = new MyInterface2() {
            public String getName() {
                return "myname";
            }

            public String getName(String s) {
                return null;
            }

            public String getName(Object o, String s) {
                return null;
            }

            public void throwException(Throwable e) throws Throwable {
            }
        };

        Object overrider1 = new Object() {
            public String getName(String s) {
                return "" + s;
            }

            public String getName(Integer o, Object s) {
                return "" + o + s;
            }

            public String getName(Object o, Integer s) {
                return "" + o + s;
            }

            public void throwException(Throwable e) throws Throwable {
                throw e;
            }
        };

        MyInterface2 newObject = (MyInterface2) new InterfaceImplementorBuilder().addInterface(MyInterface2.class).setBaseObject(baseObject).setOverrider(overrider1).toObject();

        assertEquals("myname", newObject.getName()); // from baseObject
        assertEquals("another name", newObject.getName("another name")); // from overrider
        assertEquals(null, newObject.getName("my", " name")); // no overrider method matched

        try {
            newObject.throwException(new IllegalArgumentException());
            fail();
        } catch (Throwable e) {
            assertTrue(e instanceof IllegalArgumentException);
        }

        try {
            newObject.throwException(new IOException());
            fail();
        } catch (Throwable e) {
            assertTrue(e instanceof IOException);
        }

        Object overrider2 = new Object() {
            public String getName(Object o, Object s) {
                return "" + o + s;
            }
        };

        newObject = (MyInterface2) new InterfaceImplementorBuilder().addInterface(MyInterface2.class).setBaseObject(baseObject).setOverrider(overrider2).toObject();

        assertEquals("myname", newObject.getName()); // from baseObject
        assertEquals(null, newObject.getName("another name")); // no overrider method matched
        assertEquals("my name", newObject.getName("my", " name")); // from overrider, string parameter as an object
    }

    public interface TestInterface {
        String getLastName();

        String getFirstName();

        String getFirstName(String s);

        void throwException(Throwable e) throws Throwable;

        void throwException2(Throwable e) throws Throwable;
    }

    public static class TestInterfaceImpl implements TestInterface {
        public String getLastName() {
            return "Zhou";
        }

        public String getFirstName() {
            return "Michael"; // to be overrided
        }

        public String getFirstName(String s) {
            return null;
        }

        public void throwException(Throwable e) throws Throwable {
            throw e;
        }

        public void throwException2(Throwable e) throws Throwable {
            // to be overrided
        }

        @Override
        public int hashCode() {
            return 123;
        }

        @Override
        public boolean equals(Object obj) {
            return true;
        }

        @Override
        public String toString() {
            return "haha";
        }
    }
}
