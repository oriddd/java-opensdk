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

import io.testproject.sdk.internal.rest.AgentClient;
import io.testproject.sdk.internal.rest.messages.ActionExecutionResponse;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriverException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Optional;

/**
 * Helper class allowing to execute Addons.
 */
public class AddonsHelper extends GenericAddonsHelper {
    /**
     * Logger instance.
     */
    private static final Logger LOG = LoggerFactory.getLogger(AddonsHelper.class);

    /**
     * Agent client cached instance.
     */
    private final AgentClient agentClient;

    /**
     * Initializes a new instance of the helper.
     *
     * @param agentClient Agent client to use for communicating with the Agent.
     */
    public AddonsHelper(final AgentClient agentClient) {
        super(null, agentClient);
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
     * @param action Specific Action proxy.
     * @param by     Element locator in case the Action needs one.
     * @return Presumably modified class with updated output fields.
     */
    public ActionProxy execute(final ActionProxy action, final By by) {
        return execute(action, by, -1);
    }

    /**
     * Executes an Action using it's proxy.
     * <p>
     * Addons are tiny automation building blocks that have one or more actions.
     * Addon Proxy can be obtained from the Addons page at:
     * <a href="https://app.testproject.io/#/addons">TestProject</a> App.
     *
     * @param action  Specific Action proxy.
     * @param by      Element locator in case the Action needs one.
     * @param timeout maximum amount of time allowed to wait for action execution to complete.
     * @return Potentially modified class with updated output fields (if any).
     */
    public ActionProxy execute(final ActionProxy action, final By by, final int timeout) {
        // Set element locator
        action.getDescriptor().setBy(by);
        return execute(action, timeout);
    }

}
