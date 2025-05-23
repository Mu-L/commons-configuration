/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.commons.configuration2.tree;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>
 * An abstract base class for a {@link NodeHandler} implementation for {@link ImmutableNode} objects.
 * </p>
 * <p>
 * This class already implements all methods which need no other information than the passed in node object.
 * Functionality which requires additional state (for example querying the root node or a parent node) has to be added by
 * concrete sub classes.
 * </p>
 *
 * @since 2.0
 */
abstract class AbstractImmutableNodeHandler implements NodeHandler<ImmutableNode> {
    /**
     * Checks if the passed in node is defined. Result is <strong>true</strong> if the node contains any data.
     *
     * @param node the node in question
     * @return <strong>true</strong> if the node is defined, <strong>false</strong> otherwise
     */
    static boolean checkIfNodeDefined(final ImmutableNode node) {
        return node.getValue() != null || !node.getChildren().isEmpty() || !node.getAttributes().isEmpty();
    }

    @Override
    public Set<String> getAttributes(final ImmutableNode node) {
        return node.getAttributes().keySet();
    }

    @Override
    public Object getAttributeValue(final ImmutableNode node, final String name) {
        return node.getAttributes().get(name);
    }

    @Override
    public ImmutableNode getChild(final ImmutableNode node, final int index) {
        return node.getChildren().get(index);
    }

    @Override
    public List<ImmutableNode> getChildren(final ImmutableNode node) {
        return node.getChildren();
    }

    /**
     * {@inheritDoc} This implementation returns an immutable list with all child nodes that have the specified name.
     */
    @Override
    public List<ImmutableNode> getChildren(final ImmutableNode node, final String name) {
        return getMatchingChildren(node, NodeNameMatchers.EQUALS, name);
    }

    @Override
    public int getChildrenCount(final ImmutableNode node, final String name) {
        if (name == null) {
            return node.getChildren().size();
        }
        return getMatchingChildrenCount(node, NodeNameMatchers.EQUALS, name);
    }

    /**
     * {@inheritDoc} This implementation returns an immutable list with all child nodes accepted by the specified matcher.
     */
    @Override
    public <C> List<ImmutableNode> getMatchingChildren(final ImmutableNode node, final NodeMatcher<C> matcher, final C criterion) {
        return Collections.unmodifiableList(node.stream().filter(c -> matcher.matches(c, this, criterion)).collect(Collectors.toList()));
    }

    @Override
    public <C> int getMatchingChildrenCount(final ImmutableNode node, final NodeMatcher<C> matcher, final C criterion) {
        return getMatchingChildren(node, matcher, criterion).size();
    }

    @Override
    public Object getValue(final ImmutableNode node) {
        return node.getValue();
    }

    @Override
    public boolean hasAttributes(final ImmutableNode node) {
        return !node.getAttributes().isEmpty();
    }

    @Override
    public int indexOfChild(final ImmutableNode parent, final ImmutableNode child) {
        return parent.getChildren().indexOf(child);
    }

    /**
     * {@inheritDoc} This implementation assumes that a node is defined if it has a value or has children or has attributes.
     */
    @Override
    public boolean isDefined(final ImmutableNode node) {
        return checkIfNodeDefined(node);
    }

    @Override
    public String nodeName(final ImmutableNode node) {
        return node.getNodeName();
    }
}
