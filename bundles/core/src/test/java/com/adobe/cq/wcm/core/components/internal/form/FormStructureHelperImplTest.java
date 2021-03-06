/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2017 Adobe Systems Incorporated
 ~
 ~ Licensed under the Apache License, Version 2.0 (the "License");
 ~ you may not use this file except in compliance with the License.
 ~ You may obtain a copy of the License at
 ~
 ~     http://www.apache.org/licenses/LICENSE-2.0
 ~
 ~ Unless required by applicable law or agreed to in writing, software
 ~ distributed under the License is distributed on an "AS IS" BASIS,
 ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 ~ See the License for the specific language governing permissions and
 ~ limitations under the License.
 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~*/
package com.adobe.cq.wcm.core.components.internal.form;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.ValueMap;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import com.adobe.cq.wcm.core.components.context.CoreComponentTestContext;

import io.wcm.testing.mock.aem.junit.AemContext;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@RunWith(MockitoJUnitRunner.class)
public class FormStructureHelperImplTest {

    private static final String CONTAINING_PAGE = "/content/we-retail/demo-page";

    @Rule
    public AemContext context = CoreComponentTestContext.createContext("/form/form-structure-helper", "/content");

    @Mock
    ResourceResolverFactory resourceResolverFactory;

    @InjectMocks
    private FormStructureHelperImpl formStructureHelper;

    private ResourceResolver resourceResolver;

    private static final String SLING_SCRIPTING_USER = "sling-scripting";

    @Before
    public void setUp() throws Exception {
        context.load().json("/form/form-structure-helper/test-apps.json", "/apps");
        resourceResolver = context.resourceResolver();
        final Map<String, Object> authenticationInfo = new HashMap<>();
        authenticationInfo.put(ResourceResolverFactory.SUBSERVICE, SLING_SCRIPTING_USER);

        Mockito.doAnswer(new Answer<ResourceResolver>() {
            @Override
            public ResourceResolver answer(InvocationOnMock invocation) throws Throwable {
                return resourceResolver.clone(null);
            }
        }).when(resourceResolverFactory).getServiceResourceResolver(authenticationInfo);
    }

    @Test
    public void testcanManage() {
        Resource resource = resourceResolver.getResource(CONTAINING_PAGE + "/jcr:content/root/responsivegrid/start");
        assertFalse(formStructureHelper.canManage(resource));

        resource = resourceResolver.getResource(CONTAINING_PAGE + "/jcr:content/root/responsivegrid/container/text");
        assertTrue(formStructureHelper.canManage(resource));

        resource = resourceResolver.getResource(CONTAINING_PAGE + "/jcr:content/root/responsivegrid/container");
        assertTrue(formStructureHelper.canManage(resource));
    }

    @Test
    public void testGetFormResource() {
        Resource resource = null;
        Resource formResource = formStructureHelper.getFormResource(resource);
        assertNull(formResource);

        resource = resourceResolver.getResource(CONTAINING_PAGE + "/jcr:content/root/responsivegrid/title");
        formResource = formStructureHelper.getFormResource(resource);
        assertNull(formResource);

        resource = resourceResolver.getResource(CONTAINING_PAGE + "/jcr:content/root/responsivegrid/container");
        formResource = formStructureHelper.getFormResource(resource);
        assertEquals(CONTAINING_PAGE + "/jcr:content/root/responsivegrid/container", formResource.getPath());

        resource = resourceResolver.getResource(CONTAINING_PAGE + "/jcr:content/root/responsivegrid/container/text");
        formResource = formStructureHelper.getFormResource(resource);
        assertEquals(CONTAINING_PAGE + "/jcr:content/root/responsivegrid/container", formResource.getPath());
    }


    @Test
    public void testGetFormElements() {
        Resource resource = resourceResolver.getResource(CONTAINING_PAGE + "/jcr:content/root/responsivegrid");
        Iterator<Resource> formFields = formStructureHelper.getFormElements(resource).iterator();
        assertFalse(formFields.hasNext());

        Set<String> allowedFields = new HashSet<>(6);
        allowedFields.add("text");
        allowedFields.add("hidden");
        allowedFields.add("button_button");
        allowedFields.add("text_inside_non_form_node");
        allowedFields.add("button_button_inherited");
        allowedFields.add("button_submit");
        allowedFields.add("button_submit_inherited");

        Set<String> returnedFormFields = new HashSet<>();

        resource = resourceResolver.getResource(CONTAINING_PAGE + "/jcr:content/root/responsivegrid/container");
        formFields = formStructureHelper.getFormElements(resource).iterator();
        //test only the allowed fields should be returned
        while (formFields.hasNext()) {
            Resource field = formFields.next();
            assertTrue(allowedFields.contains(field.getName()));
            returnedFormFields.add(field.getName());
        }

        //test all the fields preset in allowedFields should be returned
        for (String field : allowedFields) {
            assertTrue(returnedFormFields.contains(field));
        }
    }

    @Test
    public void testUpdateformStructure() {
        Resource resource = resourceResolver.getResource(CONTAINING_PAGE + "/jcr:content/root/responsivegrid/container");
        formStructureHelper.updateFormStructure(resource);

        ValueMap properties = resource.adaptTo(ValueMap.class);
        assertEquals("foundation/components/form/actions/store", properties.get("actionType", String.class));
        String action = properties.get("action", String.class);
        assertNotNull(action);
        assertTrue(action.length() > 0);
    }

}
