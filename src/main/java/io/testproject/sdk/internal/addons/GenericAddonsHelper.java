/*
 * Copyright 2020 TestProject LTD. and/or its affiliates
 * and other contributors as indicated by the @author tags.
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

package io.testproject.sdk.internal.addons;

import io.testproject.sdk.drivers.ReportingDriver;
import io.testproject.sdk.internal.addons.interfaces.GenericAction;
import io.testproject.sdk.internal.rest.AgentClient;
import io.testproject.sdk.internal.rest.messages.ActionExecutionResponse;
import org.openqa.selenium.WebDriverException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Optional;

public class GenericAddonsHelper {
    /**
     * Logger instance.
     */
    private static final Logger LOG = LoggerFactory.getLogger(AddonsHelper.class);

    /**
     * Agent client cached instance.
     */
    private final AgentClient agentClient;
    /**
     * Reporting driver used for action execution.
     */
    private final ReportingDriver driver;

    /**
     * Initializes a new instance of the helper.
     *
     * @param agentClient Agent client to use for communicating with the Agent.
     * @param driver The reporting driver.
     */
    public GenericAddonsHelper(final ReportingDriver driver, final AgentClient agentClient) {
        this.driver = driver;
        this.agentClient = agentClient;
    }

    /**
     * Runs the action.
     * @param action Action to run.
     */
    public void run(final GenericAction action) {
        action.run(driver);
    }

    /**
     * Executes an Action using it's proxy.
     * <p>
     * Addons are tiny automation building blocks that have one or more actions.
     * Addon Proxy can be obtained from the Addons page at:
     * <a href="https://app.testproject.io/#/addons">TestProject</a> App.
     *
     * @param action Specific Action proxy.
     * @return Presumably modified class with updated output fields.
     */
    public ActionProxy execute(final ActionProxy action) {
        return execute(action, -1);
    }

    /**
     * Executes an Action using it's proxy.
     * <p>
     * Addons are tiny automation building blocks that have one or more actions.
     * Addon Proxy can be obtained from the Addons page at:
     * <a href="https://app.testproject.io/#/addons">TestProject</a> App.
     *
     * @param action  Specific Action proxy.
     * @param timeout maximum amount of time allowed to wait for action execution to complete.
     * @return Presumably modified class with updated output fields.
     */
    public ActionProxy execute(final ActionProxy action, final int timeout) {
        // Send execution request to the Agent
        ActionExecutionResponse response = agentClient.executeProxy(action, timeout);
        if (response.getResultType() != ActionExecutionResponse.ExecutionResultType.Passed) {
            throw new WebDriverException(response.getMessage());
        }

        // Copy response fields to proxy fields
        for (ActionExecutionResponse.ResultField field : response.getFields()) {

            // Ignore input fields (even if they are updated - it's wrong!)
            // Actions should consider input fields readonly,
            // and set values only into the output fields.
            if (!field.isOutput()) {
                continue;
            }

            // Get the field of the proxy class to update.
            // This should never fail, but still making sure.
            Optional<Field> proxyField = Arrays.stream(action.getClass().getDeclaredFields())
                    .filter(m -> m.getName().equals(field.getName())).findFirst();

            if (proxyField.isEmpty()) {
                continue;
            }

            // Use reflection to set the output value
            proxyField.get().setAccessible(true);
            try {
                proxyField.get().set(action, convertToType(proxyField.get().getType(), (String) field.getValue()));
            } catch (IllegalAccessException e) {
                LOG.error("Failed to set field [{}] value to [{}]", field.getName(), field.getValue());
            }
        }

        // Return potentially updated proxy.
        return action;
    }

    /**
     * Convert string to specified type.
     * @param clazz target type for conversion..
     * @param value the value that will be converted.
     * @return value converted to correct type.
     */
    private Object convertToType(final Class<?> clazz, final String value) {
        if (Boolean.class == clazz || boolean.class == clazz) {
            return Boolean.parseBoolean(value);
        }
        try {
            if (Byte.class == clazz || byte.class == clazz) {
                return Byte.parseByte(value);
            }
            if (Short.class == clazz || short.class == clazz) {
                return Short.parseShort(value);
            }
            if (Integer.class == clazz || int.class == clazz) {
                return Integer.parseInt(value);
            }
            if (Long.class == clazz || long.class == clazz) {
                return Long.parseLong(value);
            }
            if (Float.class == clazz || float.class == clazz) {
                return Float.parseFloat(value);
            }
            if (Double.class == clazz || double.class == clazz) {
                return Double.parseDouble(value);
            }
            return value;
        } catch (NumberFormatException e) {
            LOG.error("Error parsing {} to {}", value, clazz, e);
            throw new IllegalArgumentException(String.format("Could not parse %s to %s", value, clazz.toString()));
        }
    }
}
