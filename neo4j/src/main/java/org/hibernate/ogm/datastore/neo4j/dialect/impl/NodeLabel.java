/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.dialect.impl;

import org.neo4j.graphdb.Label;

/**
 * Identifies the role of the node created by the neo4j dialect.
 *
 * @author Davide D'Alto &lt;davide@hibernate.org&gt;
 */
public enum NodeLabel implements Label {
	/**
	 * A node mapping an entity
	 */
	ENTITY,

	/**
	 * Represents an embedded (dependent) entity.
	 */
	EMBEDDED,

	/**
	 * A node representing a sequence.
	 */
	SEQUENCE,

	/**
	 * A node representing a table-based sequence.
	 */
	TABLE_BASED_SEQUENCE;
}
