/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.logging.impl;

import static org.hibernate.ogm.datastore.neo4j.query.parsing.cypherdsl.impl.CypherDSL.identifier;
import static org.hibernate.ogm.datastore.neo4j.query.parsing.cypherdsl.impl.CypherDSL.literal;

import java.util.Iterator;

import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.PropertyContainer;
import org.neo4j.graphdb.Relationship;

/**
 * Utility methods to lgo information about graph elements
 *
 * @author Davide D'Alto
 */
public final class GraphLogger {

	private static final Log log = LoggerFactory.getLogger();

	private GraphLogger() {
	}

	public static void log(String msg, Node node) {
		if ( log.isTraceEnabled() ) {
			StringBuilder builder = new StringBuilder();
			nodeAsCypher( builder, node );
			log.tracef( msg, builder.toString() );
		}
	}

	public static void log(String msg, Relationship relationship) {
		if ( log.isTraceEnabled() ) {
			StringBuilder builder = new StringBuilder();
			if ( relationship != null ) {
				nodeAsCypher( builder, relationship.getStartNode() );
				builder.append( " - [" );
				identifier( builder, relationship.getType().name() );
				appendProperties( builder, relationship );
				builder.append( "] - " );
				nodeAsCypher( builder, relationship.getEndNode() );
			}
			else {
				builder.append( "null" );
			}
			log.tracef( msg, builder.toString() );
		}
	}

	private static void nodeAsCypher(StringBuilder builder, Node node) {
		if ( node != null ) {
			builder.append( "(" );
			for ( Label label : node.getLabels() ) {
				builder.append( ":" );
				identifier( builder, label.name() );
			}
			appendProperties( builder, node );
			builder.append( ")" );
		}
		else {
			builder.append( "null" );
		}
	}

	private static void appendProperties(StringBuilder builder, PropertyContainer propertyContainer) {
		Iterator<String> propertyKeys = propertyContainer.getPropertyKeys().iterator();
		boolean hasProperties = false;
		if ( propertyKeys.hasNext() ) {
			hasProperties = true;
			builder.append( "{" );
			String property = propertyKeys.next();
			identifier( builder, property );
			builder.append( ":" );
			literal( builder, propertyContainer.getProperty( property ) );
		}
		while ( propertyKeys.hasNext() ) {
			builder.append( ", " );
			String property = propertyKeys.next();
			identifier( builder, property );
			builder.append( ":" );
			literal( builder, propertyContainer.getProperty( property ) );
		}
		if ( hasProperties ) {
			builder.append( "}" );
		}
	}

}
