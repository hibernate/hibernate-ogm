/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.remote.http.dialect.impl;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import org.hibernate.ogm.datastore.neo4j.remote.http.json.impl.Graph.Node;

/**
 * An entity node and all the embedded values associated to it.
 *
 * @author Davide D'Alto
 */
public class NodeWithEmbeddedNodes {

	private static final Map<String, Collection<Node>> EMPTY_MAP = Collections.<String, Collection<Node>>emptyMap();

	private final Node owner;
	private final Map<String, Collection<Node>> embeddedNodes;

	public NodeWithEmbeddedNodes(Node owner) {
		this( owner, EMPTY_MAP );
	}

	public NodeWithEmbeddedNodes(Node owner, Map<String, Collection<Node>> embeddedNodes) {
		this.owner = owner;
		this.embeddedNodes = embeddedNodes == null ? EMPTY_MAP : Collections.unmodifiableMap( embeddedNodes );
	}

	public Node getOwner() {
		return owner;
	}

	/**
	 * A map where the key is the path to the node (for example 'main.address.postcode') and the value is a collection
	 * of nodes.
	 *
	 * @return The set of nodes associated per path. It's never {@code null}
	 */
	public Map<String, Collection<Node>> getEmbeddedNodes() {
		return embeddedNodes;
	}
}
