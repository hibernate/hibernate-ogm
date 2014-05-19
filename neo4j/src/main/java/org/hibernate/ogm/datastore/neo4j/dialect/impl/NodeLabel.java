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
 * @author Davide D'Alto <davide@hibernate.org>
 */
public enum NodeLabel implements Label {
	ENTITY,
	TEMP_NODE;
}
