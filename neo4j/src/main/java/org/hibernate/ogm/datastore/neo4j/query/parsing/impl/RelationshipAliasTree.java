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
 * Tree structure that can be used to store alias of the relationships in Neo4j.
 * <p>
 * For example, a path to two properties like:
 * <ol>
 * <li>n.first.anotherEmbedded</li>
 * <li>n.association.anotherEmbedded</li>
 * </ol>
 * Might be represented with the following structure:
 * <pre>
 * n (alias = n)|- first (alias = _n0)  -- anotherEmbedded (alias = _n1)
 *              |
 *              -- association (alias = _n2) -- anotherEmbedded (alias = _n3)
 * </pre>
 *
 * @author Davide D'Alto
 * @author Guillaume Smet
 */
public class RelationshipAliasTree {

	private final String relationshipName;
	private final String targetEntityName;
	private final String alias;
	private final List<RelationshipAliasTree> children;

	/**
	 * Creates a tree node.
	 *
	 * @param alias the alias used for the association
	 * @param relationshipName the name of the property porting the association: it is the name of the Neo4j relationship
	 * @param entityName the name of the target entity of the relationship
	 */
	private RelationshipAliasTree(String alias, String relationshipName, String targetEntityName) {
		this.alias = alias;
		this.relationshipName = relationshipName;
		this.targetEntityName = targetEntityName;
		this.children = new ArrayList<RelationshipAliasTree>();
	}

	public static RelationshipAliasTree root(String alias) {
		return new RelationshipAliasTree( alias, alias, alias );
	}

	public static RelationshipAliasTree relationship(String alias, String relationshipName, String targetEntityName) {
		return new RelationshipAliasTree( alias, relationshipName, targetEntityName );
	}

	public RelationshipAliasTree findChild(String relationshipName) {
		for ( RelationshipAliasTree child : children ) {
			if ( child.getRelationshipName().equals( relationshipName ) ) {
				return child;
			}
		}
		return null;
	}

	public String getRelationshipName() {
		return relationshipName;
	}

	public String getTargetEntityName() {
		return targetEntityName;
	}

	public void addChild(RelationshipAliasTree embeddedNode) {
		children.add( embeddedNode );
	}

	public String getAlias() {
		return alias;
	}

	public List<RelationshipAliasTree> getChildren() {
		return Collections.unmodifiableList( children );
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append( "[relationshipName=" );
		builder.append( relationshipName );
		builder.append( ", targetEntityName=" );
		builder.append( targetEntityName );
		builder.append( ", alias=" );
		builder.append( alias );
		builder.append( "]" );
		return builder.toString();
	}

}
