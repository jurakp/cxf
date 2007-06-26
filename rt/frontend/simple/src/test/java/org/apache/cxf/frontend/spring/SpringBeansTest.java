/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.cxf.frontend.spring;

import java.util.List;

import junit.framework.Assert;

import org.apache.cxf.binding.BindingConfiguration;
import org.apache.cxf.binding.soap.Soap12;
import org.apache.cxf.binding.soap.SoapBindingConfiguration;
import org.apache.cxf.binding.soap.saaj.SAAJInInterceptor;
import org.apache.cxf.binding.soap.saaj.SAAJOutInterceptor;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.endpoint.NullConduitSelector;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.frontend.ServerFactoryBean;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.interceptor.LoggingInInterceptor;
import org.apache.cxf.interceptor.LoggingOutInterceptor;
import org.apache.cxf.service.factory.HelloService;
import org.junit.Test;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class SpringBeansTest extends Assert {
    
    @Test
    public void testServers() throws Exception {
        ClassPathXmlApplicationContext ctx = 
            new ClassPathXmlApplicationContext(new String[] {"/org/apache/cxf/frontend/spring/servers.xml"});

        ServerFactoryBean bean = (ServerFactoryBean) ctx.getBean("simple");
        assertNotNull(bean);

        bean = (ServerFactoryBean) ctx.getBean("inlineSoapBinding");
        assertNotNull(bean);
        
        BindingConfiguration bc = bean.getBindingConfig();
        assertTrue(bc instanceof SoapBindingConfiguration);
        SoapBindingConfiguration sbc = (SoapBindingConfiguration) bc;
        assertTrue(sbc.getVersion() instanceof Soap12);
    }
    
    @Test
    public void testClients() throws Exception {
        ClassPathXmlApplicationContext ctx = 
            new ClassPathXmlApplicationContext(new String[] {"/org/apache/cxf/frontend/spring/clients.xml"});

        Object bean = ctx.getBean("client1.proxyFactory");
        assertNotNull(bean);
        
        HelloService greeter = (HelloService) ctx.getBean("client1");
        assertNotNull(greeter);
        
        Client client = ClientProxy.getClient(greeter);
        assertNotNull("expected ConduitSelector", client.getConduitSelector());
        assertTrue("unexpected ConduitSelector",
                   client.getConduitSelector() instanceof NullConduitSelector);
        
        List<Interceptor> inInterceptors = client.getInInterceptors();
        boolean saaj = false;
        boolean logging = false;
        for (Interceptor<?> i : inInterceptors) {
            if (i instanceof SAAJInInterceptor) {
                saaj = true;
            } else if (i instanceof LoggingInInterceptor) {
                logging = true;
            }
        }
        assertTrue(saaj);
        assertTrue(logging);
        
        saaj = false;
        logging = false;
        for (Interceptor<?> i : client.getOutInterceptors()) {
            if (i instanceof SAAJOutInterceptor) {
                saaj = true;
            } else if (i instanceof LoggingOutInterceptor) {
                logging = true;
            }
        }
        assertTrue(saaj);
        assertTrue(logging);
    }
}
