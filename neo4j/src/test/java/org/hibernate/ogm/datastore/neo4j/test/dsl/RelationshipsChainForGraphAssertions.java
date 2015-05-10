/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.test.dsl;

import static org.hibernate.ogm.datastore.neo4j.query.parsing.cypherdsl.impl.CypherDSL.escapeIdentifier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A DSL to define a chain of relationships like the following:
 * <p>
 * <pre>
 * () -> [r1:type1 {property: {r1}.property}] -> () -[r2:type2]-> ()
 * </pre>
 *
 * @author Davide D'Alto
 */
public class RelationshipsChainForGraphAssertions {

	private final NodeForGraphAssertions start;
	private final List<NextRelationship> stream = new ArrayList<NextRelationship>();
	private final Map<String, Object> params = new HashMap<String, Object>();

	public RelationshipsChainForGraphAssertions(NodeForGraphAssertions start, NodeForGraphAssertions end, String relationshipType) {
		this.start = start;
		this.params.put( start.getAlias(), start.getProperties() );
		addToStream( end, relationshipType );
	}

	private void addToStream(NodeForGraphAssertions end, String relationshipType) {
		stream.add( new NextRelationship( end, relationshipType, "r" + stream.size() ) );
		params.put( end.getAlias(), end.getProperties() );
	}

	public RelationshipsChainForGraphAssertions relationshipTo(NodeForGraphAssertions next, String relationshipType) {
		addToStream( next, relationshipType );
		return this;
	}

	/**
	 * Get the starting node of the current chain of relationships.
	 */
	public NodeForGraphAssertions getStart() {
		return start;
	}

	/**
	 * The map with the values for the parameters in the cypher representation of the current chain.
	 */
	public Map<String, Object> getParams() {
		return params;
	}

	/**
	 * The number of relationships in the chain.
	 */
	public int getSize() {
		return stream.size();
	}

	/**
	 * Define a property on the last relationship of the chain.
	 */
	public RelationshipsChainForGraphAssertions property(String name, Object value) {
		NextRelationship relationship = stream.get( stream.size() - 1 );
		@SuppressWarnings("unchecked")
		Map<String, Object> properties = (Map<String, Object>) params.get( relationship.getAlias() );
		if ( properties == null ) {
			params.put( relationship.getAlias(), relationship.getProperties() );
		}
		relationship.addProperty( name, value );
		return this;
	}

	/**
	 * Returns the cypher representation of the chain of relationships,
	 * <p>
	 * Example:
	 * <pre>
	 * (n1:ENTITY {id: {n1}.id}) -[r0:type0 {property: {r0}.property}]-> (n2:EMBEDDED {property: {n2}.property}) -[r1:type1]-> (n3:EMBEDDED)
	 * <pre>
	 */
	public String toCypher() {
		StringBuilder builder = new StringBuilder();
		builder.append( start.toCypher() );
		for ( NextRelationship relationship : stream ) {
			builder.append( " -[" );
			if ( !relationship.getProperties().isEmpty() ) {
				builder.append( relationship.getAlias() );
			}
			builder.append( ":" );
			builder.append( relationship.getRelationshipType() );
			if ( !relationship.getProperties().isEmpty() ) {
				builder.append( " {" );
				boolean first = true;
				for ( String property : relationship.getProperties().keySet() ) {
					if ( first ) {
						first = false;
					}
					else {
						builder.append( ", " );
					}
					escapeIdentifier( builder, property );
					builder.append( ": {" );
					builder.append( relationship.getAlias() );
					builder.append( "}." );
					escapeIdentifier( builder, property );
				}
				builder.append( " }" );
			}
			builder.append( "]-> " );
			builder.append( relationship.getEnd().toCypher() );
		}
		return builder.toString();
	}

	private static class NextRelationship {

		private final NodeForGraphAssertions end;
		private final String relationshipType;
		private final Map<String, Object> properties = new HashMap<String, Object>();
		private final String alias;

		public NextRelationship(NodeForGraphAssertions to, String relationshipType, String alias) {
			this.end = to;
			this.relationshipType = relationshipType;
			this.alias = alias;
		}

		public String getAlias() {
			return alias;
		}

		public Map<String, Object> getProperties() {
			return properties;
		}

		public NodeForGraphAssertions getEnd() {
			return end;
		}

		public String getRelationshipType() {
			return relationshipType;
		}

		public void addProperty( String name, Object value ) {
			properties.put( name, value );
		}
	}

}
