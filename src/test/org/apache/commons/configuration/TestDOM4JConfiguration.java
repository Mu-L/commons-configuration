package org.apache.commons.configuration;

/*
 * Copyright 2001-2004 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
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

import java.io.File;

import junit.framework.TestCase;

/**
 * test for loading and saving xml properties files
 *
 * @version $Id: TestDOM4JConfiguration.java,v 1.3 2004/02/27 17:41:34 epugh Exp $
 */
public class TestDOM4JConfiguration extends TestCase
{
    /** The File that we test with */
    private String testProperties = new File("conf/test.xml").getAbsolutePath();
    private String testBasePath = new File("conf").getAbsolutePath();
    private DOM4JConfiguration conf;

    protected void setUp() throws Exception
    {
        conf = new DOM4JConfiguration(new File(testProperties));
    }

    public void testGetProperty() throws Exception
    {
        assertEquals("value", conf.getProperty("element"));
    }

    public void testGetComplexProperty() throws Exception
    {
        assertEquals("I'm complex!", conf.getProperty("element2.subelement.subsubelement"));
    }

    public void testSettingFileNames() throws Exception
    {
        conf = new DOM4JConfiguration();
        conf.setFileName(testProperties);
        assertEquals(testProperties.toString(), conf.getFileName());

        conf.setBasePath(testBasePath);
        conf.setFileName("hello.xml");
        assertEquals("hello.xml", conf.getFileName());
        assertEquals(testBasePath.toString(), conf.getBasePath());
        assertEquals(new File(testBasePath, "hello.xml"), conf.getFile());

        conf.setBasePath(testBasePath);
        conf.setFileName("/subdir/hello.xml");
        assertEquals("/subdir/hello.xml", conf.getFileName());
        assertEquals(testBasePath.toString(), conf.getBasePath());
        assertEquals(new File(testBasePath, "/subdir/hello.xml"), conf.getFile());
    }

    public void testLoad() throws Exception
    {
        conf = new DOM4JConfiguration();
        conf.setFileName(testProperties);
        conf.load();

        assertEquals("I'm complex!", conf.getProperty("element2.subelement.subsubelement"));
    }
    
    public void testLoadWithBasePath() throws Exception
    {
        conf = new DOM4JConfiguration();
        
        conf.setFileName("test.xml");
        conf.setBasePath(testBasePath);
        conf.load();

        assertEquals("I'm complex!", conf.getProperty("element2.subelement.subsubelement"));
    }
}
