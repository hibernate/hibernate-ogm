/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.query.parsing.impl;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.AssertionFailure;
import org.hibernate.ogm.datastore.neo4j.dialect.impl.NodeLabel;
import org.hibernate.ogm.util.impl.StringHelper;

/**
 * Keep track of the aliases needed to create the Cypher query.
 *
 * @author Davide D'Alto
 * @author Guillaume Smet
 */
public class Neo4jAliasResolver {

	private final Map<String, String> aliasByEntityName = new HashMap<String, String>();
	private final Map<String, RelationshipAliasTree> relationshipAliases = new HashMap<String, RelationshipAliasTree>();

	// Contains the aliases that will appear in the OPTIONAL MATCH clause of the query
	private final Set<String> optionalMatches = new HashSet<String>();

	// Contains the aliases that will appear in the MATCH clause of the query
	private final Set<String> requiredMatches = new HashSet<String>();

	private int relationshipCounter = 0;

	public void registerEntityAlias(String entityName, String alias) {
		aliasByEntityName.put( entityName, alias );
	}

	public String findAliasForType(String entityType) {
		return aliasByEntityName.get( entityType );
	}

	/**
	 * Given the path to an embedded property, it will create an alias to use in the query for the embedded containing
	 * the property.
	 * <p>
	 * The alias will be saved and can be returned using the method {@link #findAlias(String, List)}.
	 * <p>
	 * For example, using n as entity alias and [embedded, anotherEmbedded] as path to the embedded will
	 * return "_n2" as alias for "n.embedded.anotherEmbedded".
	 * <p>
	 * Note that you need to create an alias for every embedded/association in the path before this one.
	 *
	 * @param entityAlias the alias of the entity that contains the embedded
	 * @param propertyPathWithoutAlias the path to the property without the alias
	 * @param optionalMatch if true, the alias does not represent a required match in the query (It will appear in the OPTIONAL MATCH clause)
	 * @return the alias of the embedded containing the property
	 */
	public String createAliasForEmbedded(String entityAlias, List<String> propertyPathWithoutAlias, boolean optionalMatch) {
		return createAliasForRelationship( entityAlias, propertyPathWithoutAlias, NodeLabel.EMBEDDED.name(), optionalMatch );
	}

	/**
	 * Given the path to an association, it will create an alias to use in the query for the association.
	 * <p>
	 * The alias will be saved and can be returned using the method {@link #findAlias(String, List)}.
	 * <p>
	 * For example, using n as entity alias and [association, anotherAssociation] as path to the association will
	 * return "_n2" as alias for "n.association.anotherAssociation".
	 * <p>
	 * Note that you need to create an alias for every embedded/association in the path before this one.
	 *
	 * @param entityAlias the alias of the entity that contains the embedded
	 * @param propertyPathWithoutAlias the path to the property without the alias
	 * @param targetNodeType the name of the target node type
	 * @param optionalMatch if true, the alias does not represent a required match in the query (It will appear in the OPTIONAL MATCH clause)
	 * @return the alias of the embedded containing the property
	 */
	public String createAliasForAssociation(String entityAlias, List<String> propertyPathWithoutAlias, String targetNodeType,
			boolean optionalMatch) {
		return createAliasForRelationship( entityAlias, propertyPathWithoutAlias, targetNodeType, optionalMatch );
	}

	private String createAliasForRelationship(String entityAlias, List<String> propertyPathWithoutAlias, String targetEntityName,
			boolean optionalMatch) {
		RelationshipAliasTree relationshipAlias = relationshipAliases.get( entityAlias );
		if ( relationshipAlias == null ) {
			relationshipAlias = RelationshipAliasTree.root( entityAlias );
			relationshipAliases.put( entityAlias, relationshipAlias );
		}
		for ( int i = 0; i < propertyPathWithoutAlias.size(); i++ ) {
			String name = propertyPathWithoutAlias.get( i );
			RelationshipAliasTree child = relationshipAlias.findChild( name );
			if ( child == null ) {
				if ( i != propertyPathWithoutAlias.size() - 1 ) {
					throw new AssertionFailure( "The path to " + StringHelper.join( propertyPathWithoutAlias, "." )
							+ " has not been completely constructed" );
				}

				relationshipCounter++;
				String childAlias = "_" + entityAlias + relationshipCounter;
				child = RelationshipAliasTree.relationship( childAlias, name, targetEntityName );

				relationshipAlias.addChild( child );
			}
			relationshipAlias = child;
			String alias = relationshipAlias.getAlias();
			if ( optionalMatch && !requiredMatches.contains( alias ) ) {
				optionalMatches.add( alias );
			}
			else {
				requiredMatches.add( alias );
				optionalMatches.remove( alias );
			}
		}
		return relationshipAlias.getAlias();
	}

	/**
	 * Given the alias of the entity and the path to the relationship it will return the alias
	 * of the component.
	 *
	 * @param entityAlias the alias of the entity
	 * @param propertyPathWithoutAlias the path to the property without the alias
	 * @return the alias the relationship or null
	 */
	public String findAlias(String entityAlias, List<String> propertyPathWithoutAlias) {
		RelationshipAliasTree aliasTree = relationshipAliases.get( entityAlias );
		if ( aliasTree == null ) {
			return null;
		}
		RelationshipAliasTree associationAlias = aliasTree;
		for ( int i = 0; i < propertyPathWithoutAlias.size(); i++ ) {
			associationAlias = associationAlias.findChild( propertyPathWithoutAlias.get( i ) );
			if ( associationAlias == null ) {
				return null;
			}
		}
		return associationAlias.getAlias();
	}

	/**
	 * Given the alias of the entity it will return a tree structure containing all the aliases for the embedded
	 * properties and associations of the entity.
	 * <p>
	 * The tree has the entity alias as root and the embedded/association alias as children.
	 * For example, a path to two properties like:
	 * <ol>
	 * <li>n.first.anotherEmbedded</li>
	 * <li>n.second.anotherEmbedded</li>
	 * </ol>
	 *
	 * Might be represented with the following structure:
	 * <pre>
	 * n (alias = n)|- first (alias = _n1)  -- anotherEmbedded (alias = _n2)
	 *              |
	 *              -- second (alias = _n3) -- anotherEmbedded (alias = _n4)
	 * </pre>
	 * @param entityAlias the alias of the entity that contains the embedded
	 * @return the corresponding {@link RelationshipAliasTree} or null
	 */
	public RelationshipAliasTree getRelationshipAliasTree(String entityAlias) {
		return relationshipAliases.get( entityAlias );
	}

	/**
	 * Tells if the alias has to be used in the OPTIONAL MATCH part of the query.
	 *
	 * @param alias the alis to check
	 * @return {@code true} if the alias should be used in OPTIONAL MATCH part of the query, {@code false} otherwise
	 */
	public boolean isOptionalMatch(String alias) {
		return optionalMatches.contains( alias );
	}

}
