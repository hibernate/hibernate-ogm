/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.query.parsing.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.antlr.runtime.tree.Tree;
import org.hibernate.hql.ast.common.JoinType;
import org.hibernate.hql.ast.origin.hql.resolve.path.PathedPropertyReference;
import org.hibernate.hql.ast.origin.hql.resolve.path.PathedPropertyReferenceSource;
import org.hibernate.hql.ast.origin.hql.resolve.path.PropertyPath;
import org.hibernate.hql.ast.spi.QueryResolverDelegate;

/**
 * @author Davide D'Alto &lt;davide@hibernate.org&gt;
 */
public class Neo4jQueryResolverDelegate implements QueryResolverDelegate {

	/**
	 * Persister space: keep track of aliases and entity names.
	 */
	private final Map<String, String> aliasToEntityType = new HashMap<String, String>();
	private final Map<String, String> aliases = new HashMap<String, String>();
	private final Map<String, EmbeddedAliasTree> embeddedAliases = new HashMap<String, EmbeddedAliasTree>();
	private int embeddedCounter = 0;

	@Override
	public void registerPersisterSpace(Tree entityName, Tree alias) {
		aliases.put( entityName.getText(), alias.getText() );
		String put = aliasToEntityType.put( alias.getText(), entityName.getText() );
		if ( put != null && !put.equalsIgnoreCase( entityName.getText() ) ) {
			throw new UnsupportedOperationException(
					"Alias reuse currently not supported: alias " + alias.getText()
					+ " already assigned to type " + put );
		}
	}

	@Override
	public boolean isUnqualifiedPropertyReference() {
		return true;
	}

	@Override
	public PathedPropertyReferenceSource normalizeUnqualifiedPropertyReference(Tree property) {
		if ( aliasToEntityType.containsKey( property.getText() ) ) {
			return new PathedPropertyReference( property.getText(), null, true );
		}
		else {
			return new PathedPropertyReference( property.getText(), null, false );
		}
	}

	@Override
	public boolean isPersisterReferenceAlias() {
		return true;
	}

	@Override
	public PathedPropertyReferenceSource normalizeUnqualifiedRoot(Tree identifier382) {
		throw new UnsupportedOperationException( "Not implemented yet" );
	}

	@Override
	public PathedPropertyReferenceSource normalizeQualifiedRoot(Tree identifier381) {
		return new PathedPropertyReference( identifier381.getText(), null, true );
	}

	@Override
	public PathedPropertyReferenceSource normalizePropertyPathIntermediary(PropertyPath path, Tree propertyName) {
		return new PathedPropertyReference( propertyName.getText(), null, false );
	}

	@Override
	public PathedPropertyReferenceSource normalizeIntermediateIndexOperation(PathedPropertyReferenceSource propertyReferenceSource, Tree collectionProperty,
			Tree selector) {
		throw new UnsupportedOperationException( "Not implemented yet" );
	}

	@Override
	public void normalizeTerminalIndexOperation(PathedPropertyReferenceSource propertyReferenceSource, Tree collectionProperty, Tree selector) {
		throw new UnsupportedOperationException( "Not implemented yet" );
	}

	@Override
	public PathedPropertyReferenceSource normalizeUnqualifiedPropertyReferenceSource(Tree identifier394) {
		throw new UnsupportedOperationException( "Not implemented yet" );
	}

	@Override
	public PathedPropertyReferenceSource normalizePropertyPathTerminus(PropertyPath path, Tree propertyNameNode) {
		return new PathedPropertyReference( propertyNameNode.getText(), null, false );
	}

	@Override
	public void pushFromStrategy(JoinType joinType, Tree assosiationFetchTree, Tree propertyFetchTree, Tree alias) {
		throw new UnsupportedOperationException( "Not implemented yet" );
	}

	@Override
	public void pushSelectStrategy() {
		//nothing to do
	}

	@Override
	public void popStrategy() {
		//nothing to do
	}

	@Override
	public void propertyPathCompleted(PropertyPath path) {
		//nothing to do
	}

	public String findAliasForType(String entityType) {
		return aliases.get( entityType );
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
	 * @return the alias of the embedded containing the property
	 */
	public String createAliasForEmbedded(String entityAlias, List<String> propertyPathWithoutAlias) {
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
		return embeddedAlias.getAlias();
	}

	/**
	 * Given the alias of the entity and the path to the embedded properties it will return the alias
	 * of the embedded component containing the property.
	 *
	 * @see #createAliasForEmbedded(String, List)
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
}
