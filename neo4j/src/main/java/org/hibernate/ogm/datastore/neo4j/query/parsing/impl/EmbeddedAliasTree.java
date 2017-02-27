/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.query.parsing.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Tree structure that can be used to store alias of embedded components in Neo4j.
 * <p>
 * For example, a path to two properties like:
 * <ol>
 * <li>n.first.anotherEmbedded</li>
 * <li>n.second.anotherEmbedded</li>
 * </ol>
 * Might be represented with the following structure:
 * <pre>
 * n (alias = n)|- first (alias = n_0)  -- anotherEmbedded (alias = n_1)
 *              |
 *              -- second (alias = n_2) -- anotherEmbedded (alias = n_3)
 * </pre>
 *
 * @author Davide D'Alto
 */
public class EmbeddedAliasTree {

	private final String name;
	private final String alias;
	private final List<EmbeddedAliasTree> children;

	/**
	 * Creates a tree node.
	 *
	 * @param alias the alias used for the embedded
	 * @param name the name of the property representing the embedded
	 */
	public EmbeddedAliasTree(String alias, String name) {
		this.name = name;
		this.alias = alias;
		this.children = new ArrayList<EmbeddedAliasTree>();
	}

	public EmbeddedAliasTree findChild(String name) {
		for ( EmbeddedAliasTree child : children ) {
			if ( child.getName().equals( name ) ) {
				return child;
			}
		}
		return null;
	}

	public String getName() {
		return name;
	}

	public void addChild(EmbeddedAliasTree embeddedNode) {
		children.add( embeddedNode );
	}

	public String getAlias() {
		return alias;
	}

	public List<EmbeddedAliasTree> getChildren() {
		return Collections.unmodifiableList( children );
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append( "[name=" );
		builder.append( name );
		builder.append( ", alias=" );
		builder.append( alias );
		builder.append( "]" );
		return builder.toString();
	}

}
