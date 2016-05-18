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

/**
 * Keep track of the aliases needed to create the Cypher query.
 *
 * @author Davide D'Alto
 */
public class AliasResolver {

	private final Map<String, String> aliasByEntityName = new HashMap<String, String>();
	private final Map<String, EmbeddedAliasTree> embeddedAliases = new HashMap<String, EmbeddedAliasTree>();

	// Contains the aliases that will appear in the OPTIONAL MATCH clause of the query
	private final Set<String> optionalMatches = new HashSet<String>();

	// Contains the aliases that will appear in the MATCH clause of the query
	private final Set<String> requiredMatches = new HashSet<String>();

	private int embeddedCounter = 0;

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
	 * The alias will be saved and can be returned using the method {@link #findAliasForEmbedded(String, List)}.
	 * <p>
	 * For example, using n as entity alias and [embedded, anotherEmbedded, property] as path to the property will
	 * return "n_2" as alias for "n.embedded.anotherEmbedded".
	 *
	 * @param entityAlias the alias of the entity that contains the embedded
	 * @param propertyPathWithoutAlias the path to the property without the alias
	 * @param optionalMatch if true, the alias does not represent a required match in the query (It will appear in the OPTIONAL MATCH clause)
	 * @return the alias of the embedded containing the property
	 */
	public String createAliasForEmbedded(String entityAlias, List<String> propertyPathWithoutAlias, boolean optionalMatch) {
		EmbeddedAliasTree embeddedAlias = embeddedAliases.get( entityAlias );
		if ( embeddedAlias == null ) {
			embeddedAlias = new EmbeddedAliasTree( entityAlias, entityAlias );
			embeddedAliases.put( entityAlias, embeddedAlias );
		}
		for ( int i = 0; i < propertyPathWithoutAlias.size() - 1; i++ ) {
			String name = propertyPathWithoutAlias.get( i );
			EmbeddedAliasTree child = embeddedAlias.findChild( name );
			if ( child == null ) {
				embeddedCounter++;
				String childAlias = "_" + entityAlias + embeddedCounter;
				child = new EmbeddedAliasTree( childAlias, name );
				embeddedAlias.addChild( child );
			}
			embeddedAlias = child;
		}
		if ( optionalMatch && !requiredMatches.contains( embeddedAlias.getAlias() ) ) {
			optionalMatches.add( embeddedAlias.getAlias() );
		}
		else {
			requiredMatches.add( embeddedAlias.getAlias() );
			optionalMatches.remove( embeddedAlias.getAlias() );
		}
		return embeddedAlias.getAlias();
	}

	/**
	 * Given the alias of the entity and the path to the embedded properties it will return the alias
	 * of the embedded component containing the property.
	 *
	 * @see #createAliasForEmbedded(String, List, boolean)
	 * @param entityAlias the alias of the entity that contains the embedded
	 * @param propertyPathWithoutAlias the path to the property without the alias
	 * @return the alias of the embedded containing the property or null
	 */
	public String findAliasForEmbedded(String entityAlias, List<String> propertyPathWithoutAlias) {
		EmbeddedAliasTree aliasTree = embeddedAliases.get( entityAlias );
		if ( aliasTree == null ) {
			return null;
		}
		EmbeddedAliasTree embedded = aliasTree;
		for ( int i = 0; i < propertyPathWithoutAlias.size() - 1; i++ ) {
			embedded = embedded.findChild( propertyPathWithoutAlias.get( i ) );
			if ( embedded == null ) {
				return null;
			}
		}
		return embedded.getAlias();
	}

	/**
	 * Given the alias of the entity it will return a tree structure containing all the aliases for the embedded
	 * properties of the entity.
	 * <p>
	 * The tree has the entity alias as root and the embedded alias as children.
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
	 * @return the corresponding {@link EmbeddedAliasTree} or null
	 */
	public EmbeddedAliasTree getAliasTree(String entityAlias) {
		return embeddedAliases.get( entityAlias );
	}

	/**
	 * Tells if the alias has to be used in the OPTIONAL MATCH part of the query.
	 *
	 * @param alias the alias to check
	 * @return {@code true} if the alias should be used in OPTIONAL MATCH part of the query, {@code false} otherwise
	 */
	public boolean isOptionalMatch(String alias) {
		return optionalMatches.contains( alias );
	}
}
