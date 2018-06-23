/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.ogm.datastore.neo4j.index.impl;

import java.util.List;
import java.util.Objects;

import org.hibernate.ogm.datastore.neo4j.query.parsing.cypherdsl.impl.CypherDSL;

import org.neo4j.graphdb.Label;

/**
 * @author The Viet Nguyen
 */
public class Neo4jIndexSpec {

	private Label label;
	private List<String> properties;
	private boolean unique;

	public Neo4jIndexSpec(Label label, List<String> properties) {
		this( label, properties, false );
	}

	public Neo4jIndexSpec(Label label, List<String> properties, boolean unique) {
		this.label = label;
		this.properties = properties;
		this.unique = unique;
	}

	public Label getLabel() {
		return label;
	}

	public List<String> getProperties() {
		return properties;
	}

	public boolean isUnique() {
		return unique;
	}

	/**
	 * @return the cypher query for the creation of the constraint
	 */
	public String asCypherQuery() {
		StringBuilder queryBuilder = new StringBuilder( "CREATE INDEX ON :" );
		CypherDSL.escapeIdentifier( queryBuilder, label.name() );
		queryBuilder.append( "(" );
		for ( int i = 0; i < properties.size(); ++i ) {
			if ( i != 0 ) {
				queryBuilder.append( ", " );
			}
			CypherDSL.escapeIdentifier( queryBuilder, properties.get( i ) );
		}
		queryBuilder.append( ")" );
		return queryBuilder.toString();
	}

	@Override
	public boolean equals(Object object) {
		if ( this == object ) {
			return true;
		}
		if ( object == null || getClass() != object.getClass() ) {
			return false;
		}
		Neo4jIndexSpec that = (Neo4jIndexSpec) object;
		return Objects.equals( label, that.label ) &&
				Objects.equals( properties, that.properties ) &&
				Objects.equals( unique, that.unique );
	}

	@Override
	public int hashCode() {
		return Objects.hash( label, properties, unique );
	}

	@Override
	public String toString() {
		return "Neo4jIndexSpec[" + "label=" + label + ", properties=" + properties + ", unique=" + unique + ']';
	}
}
