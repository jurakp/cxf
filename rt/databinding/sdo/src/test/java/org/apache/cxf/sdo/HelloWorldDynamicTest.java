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

package org.apache.cxf.sdo;


import javax.jws.WebService;
import javax.xml.namespace.QName;

import org.w3c.dom.Node;

import org.apache.cxf.frontend.ServerFactoryBean;

import org.junit.Before;
import org.junit.Test;

/**
 * 
 */
public class HelloWorldDynamicTest extends AbstractSDOTest {


    @Before 
    public void setUp() throws Exception {
        super.setUp();
        createService(Server.class, new Server(), "TestService", null);
    }
    
    
    @WebService(targetNamespace = "http://apache.org/hello_world_soap_http",
                name = "Greeter",
                serviceName = "TestService",
                endpointInterface = "helloworld.dynamic_types.ws.Greeter")
    public static class Server {
        public java.lang.String sayHi() {
            return "Hi!";
        }

        public void pingMe() {
        }

        public void greetMeOneWay(String s) {
        }

        public java.lang.String greetMe(String s) {
            return "Hello " + s;            
        }
    }
    
    
    protected ServerFactoryBean createServiceFactory(Class serviceClass, 
                                                     Object serviceBean, 
                                                     String address, 
                                                     QName name,
                                                     SDODataBinding binding) {
        ServerFactoryBean sf = super.createServiceFactory(serviceClass, serviceBean, address, name, binding);
        sf.setWsdlLocation(HelloWorldStaticTest.class
                               .getResource("/wsdl_sdo/HelloService_dynamic.wsdl").toString());
        sf.setServiceName(new QName("http://apache.org/hello_world_soap_http", "SOAPService"));
        sf.setEndpointName(new QName("http://apache.org/hello_world_soap_http", "SoapPort"));
        return sf;
    }
    
    @Test
    public void testBasicInvoke() throws Exception {
        Node response = invoke("TestService", "bean11.xml");
        addNamespace("ns1", "http://apache.org/hello_world_soap_http/types");
        assertValid("/s:Envelope/s:Body/ns1:greetMeResponse", response);
        assertValid("//ns1:greetMeResponse/ns1:responseType", response);
        assertValid("//ns1:greetMeResponse/ns1:responseType[text()='Hello World']", response);
    }
    
    @Test
    public void testWSDL() throws Exception {
        Node doc = getWSDLDocument("TestService");
        assertNotNull(doc);
        assertValid("/wsdl:definitions/wsdl:types/xsd:schema"
                    + "[@targetNamespace='http://apache.org/hello_world_soap_http/types']", 
                    doc);
    }
}
