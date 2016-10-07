/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.remote.common.dialect.impl;

import java.util.Collections;
import java.util.Map;

/**
 * Contains the properties of two nodes and the relationship that joins them.
 *
 * @author Davide D'Alto
 */
public class RemoteNeo4jAssociationPropertiesRow {

	private final Map<String, Object> relationship;
	private final Map<String, Object> ownerNode;
	private final Map<String, Object> targetNode;

	public RemoteNeo4jAssociationPropertiesRow(Map<String, Object> rel, Map<String, Object> ownerNode, Map<String, Object> targetNode) {
		this.relationship = Collections.unmodifiableMap( rel );
		this.ownerNode = Collections.unmodifiableMap( ownerNode );
		this.targetNode = Collections.unmodifiableMap( targetNode );
	}

	public Map<String, Object> getRelationship() {
		return relationship;
	}

	public Map<String, Object> getOwnerNode() {
		return ownerNode;
	}

	public Map<String, Object> getTargetNode() {
		return targetNode;
	}
}
