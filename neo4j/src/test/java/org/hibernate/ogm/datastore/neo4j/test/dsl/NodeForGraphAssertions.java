/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.test.dsl;

import static org.hibernate.ogm.datastore.neo4j.query.parsing.cypherdsl.impl.CypherDSL.escapeIdentifier;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * /**
 * A DSL to define a node like the following:
 * <p>
 * <pre>
 * (n:ENTITY:StoryGame {id: {n}.id})
 * </pre>
 *
 * @author Davide D'Alto
 */
public class NodeForGraphAssertions {

	private final String alias;
	private final String[] labels;
	private final Map<String, Object> properties = new HashMap<String, Object>();
	private final Map<String, Object> params = new HashMap<String, Object>();

	public NodeForGraphAssertions(String alias, String[] labels) {
		this.alias = alias;
		this.labels = labels;
		this.params.put( alias, properties );
	}

	public NodeForGraphAssertions property(String property, Object value) {
		properties.put( property, value );
		return this;
	}

	public RelationshipsChainForGraphAssertions relationshipTo(NodeForGraphAssertions endNode, String relationshipType) {
		Map<String, Object> emptyMap = Collections.emptyMap();
		return relationshipTo( endNode, relationshipType, emptyMap );
	}

	public RelationshipsChainForGraphAssertions relationshipTo(NodeForGraphAssertions endNode, String relationshipType, Map<String, Object> properties) {
		return new RelationshipsChainForGraphAssertions( this, endNode, relationshipType );
	}

	public String toCypher() {
		StringBuilder builder = new StringBuilder();
		builder.append( "(" );
		builder.append( alias );
		for ( String label : labels ) {
			builder.append( ":" );
			builder.append( label );
		}
		if ( !properties.isEmpty() ) {
			builder.append( " {" );
			int index = 0;
			for ( String property : properties.keySet() ) {
				escapeIdentifier( builder, property );
				builder.append( ": " );
				builder.append( "{" );
				builder.append( alias );
				builder.append( "}" );
				builder.append( "." );
				escapeIdentifier( builder, property );
				index++;
				if ( index < properties.size() ) {
					builder.append( ", " );
				}
			}
			builder.append( "}" );
		}
		builder.append( ")" );
		return builder.toString();
	}

	/**
	 * The node alias.
	 */
	public String getAlias() {
		return alias;
	}

	/**
	 * The map with the values for the parameters in the cypher representation of the node.
	 */
	public Map<String, Object> getParams() {
		return Collections.unmodifiableMap( params );
	}

	/**
	 * Returns the properties for the node.
	 */
	public Map<String, Object> getProperties() {
		return Collections.unmodifiableMap( properties );
	}
}
