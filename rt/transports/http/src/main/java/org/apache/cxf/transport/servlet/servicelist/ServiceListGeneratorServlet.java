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
package org.apache.cxf.transport.servlet.servicelist;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.cxf.Bus;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.AbstractDestination;
import org.apache.cxf.transport.http.DestinationRegistry;

public class ServiceListGeneratorServlet extends HttpServlet {
    private DestinationRegistry destinationRegistry;
    private Bus bus;
    private String serviceListStyleSheet;
    private String title;

    public ServiceListGeneratorServlet(DestinationRegistry destinationRegistry, Bus bus) {
        this.destinationRegistry = destinationRegistry;
        this.bus = bus;
        this.title = "CXF - Service list";
    }

    public void setServiceListStyleSheet(String serviceListStyleSheet) {
        this.serviceListStyleSheet = serviceListStyleSheet;
    }

    public void setTitle(String title) {
        this.title = title;
    }
    

    @SuppressWarnings("unchecked")
    @Override
    public void service(HttpServletRequest request, 
                        HttpServletResponse response) throws ServletException, IOException {
        PrintWriter writer = response.getWriter();
        AbstractDestination[] destinations = destinationRegistry.getSortedDestinations();
        if (request.getParameter("stylesheet") != null) {
            renderStyleSheet(request, response);
            return;
        }
        List<String> privateEndpoints;
        Map<String, String> atomMap;
        
        if (bus != null) {
            privateEndpoints = (List<String>)bus.getProperty("org.apache.cxf.private.endpoints");
            // TODO : we may introduce a bus extension instead

            atomMap = (Map<String, String>)bus
                .getProperty("org.apache.cxf.extensions.logging.atom.pull");
        } else {
            privateEndpoints = new ArrayList<String>();
            atomMap = new HashMap<String, String>();
        }
        
        AbstractDestination[] soapEndpoints = getSOAPEndpoints(destinations, privateEndpoints);
        AbstractDestination[] restEndpoints = getRestEndpoints(destinations, privateEndpoints);
        ServiceListWriter serviceListWriter;
        if ("false".equals(request.getParameter("formatted"))) {
            boolean renderWsdlList = "true".equals(request.getParameter("wsdlList"));
            serviceListWriter = new UnformattedServiceListWriter(renderWsdlList);
        } else {
            String styleSheetPath;
            if (serviceListStyleSheet != null) {
                styleSheetPath = request.getContextPath() + "/" + serviceListStyleSheet;
                
            } else {
                styleSheetPath = request.getRequestURI() + "/?stylesheet=1";
            }
            serviceListWriter = new FormattedServiceListWriter(styleSheetPath, title, atomMap);
            
        }
        response.setContentType(serviceListWriter.getContentType());
        serviceListWriter.writeServiceList(writer, soapEndpoints, restEndpoints);
    }
    

    private boolean isPrivate(EndpointInfo ei, List<String> privateEndpoints) {
        if (privateEndpoints != null) {
            for (String s : privateEndpoints) {
                if (ei.getAddress().endsWith(s)) {
                    return true;
                }
            }
        }
        return false;
    }

    private AbstractDestination[] getSOAPEndpoints(AbstractDestination[] destinations,
                                                   List<String> privateEndpoints) {
        List<AbstractDestination> soapEndpoints = new ArrayList<AbstractDestination>();
        for (AbstractDestination sd : destinations) {
            if (null != sd.getEndpointInfo().getName() && null != sd.getEndpointInfo().getInterface()
                && !isPrivate(sd.getEndpointInfo(), privateEndpoints)) {
                soapEndpoints.add(sd);
            }
        }
        return soapEndpoints.toArray(new AbstractDestination[]{});
    }
    
    private AbstractDestination[] getRestEndpoints(AbstractDestination[] destinations,
                                                   List<String> privateEndpoints) {
        List<AbstractDestination> restfulDests = new ArrayList<AbstractDestination>();
        for (AbstractDestination sd : destinations) {
            // use some more reasonable check - though this one seems to be the only option at the moment
            if (null == sd.getEndpointInfo().getInterface()
                && !isPrivate(sd.getEndpointInfo(), privateEndpoints)) {
                restfulDests.add(sd);
            }
        }
        return restfulDests.toArray(new AbstractDestination[]{});
    }
    

    private void renderStyleSheet(HttpServletRequest request, HttpServletResponse response)
        throws IOException {
        response.setContentType("text/css; charset=UTF-8");
        URL url = this.getClass().getResource("servicelist.css");
        if (url != null) {
            IOUtils.copy(url.openStream(), response.getOutputStream());
        }
    }

    public void init(ServletConfig servletConfig) {
        String configServiceListStyleSheet = servletConfig.getInitParameter("service-list-stylesheet");
        if (configServiceListStyleSheet != null) {
            this.serviceListStyleSheet = configServiceListStyleSheet;
        }
        String configTitle = servletConfig.getInitParameter("service-list-title");
        if (configTitle != null) {
            this.title = configTitle;
        }
    }

    public ServletConfig getServletConfig() {
        return null;
    }

    public String getServletInfo() {
        return null;
    }

    public void destroy() { 
    }
}
