/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.query.parsing.impl;

import static org.hibernate.ogm.datastore.neo4j.query.parsing.cypherdsl.impl.CypherDSL.identifier;
import static org.hibernate.ogm.datastore.neo4j.query.parsing.cypherdsl.impl.CypherDSL.node;
import static org.hibernate.ogm.datastore.neo4j.query.parsing.cypherdsl.impl.CypherDSL.relationship;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.antlr.runtime.tree.Tree;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.hql.ast.common.JoinType;
import org.hibernate.hql.ast.origin.hql.resolve.path.PropertyPath;
import org.hibernate.hql.ast.spi.EntityNamesResolver;
import org.hibernate.hql.ast.spi.SingleEntityQueryBuilder;
import org.hibernate.hql.ast.spi.SingleEntityQueryRendererDelegate;
import org.hibernate.hql.ast.spi.predicate.ComparisonPredicate.Type;
import org.hibernate.ogm.datastore.neo4j.logging.impl.Log;
import org.hibernate.ogm.datastore.neo4j.logging.impl.LoggerFactory;
import java.lang.invoke.MethodHandles;
import org.hibernate.ogm.datastore.neo4j.query.parsing.cypherdsl.impl.CypherDSL;
import org.hibernate.ogm.datastore.neo4j.query.parsing.impl.predicate.impl.Neo4jPredicateFactory;
import org.hibernate.ogm.model.key.spi.EntityKeyMetadata;
import org.hibernate.ogm.persister.impl.OgmEntityPersister;
import org.hibernate.ogm.type.spi.GridType;
import org.hibernate.ogm.type.spi.TypeTranslator;

/**
 * Parser delegate which creates Neo4j queries in form of {@link StringBuilder}s.
 *
 * @author Davide D'Alto
 * @author Guillaume Smet
 */
public class Neo4jQueryRendererDelegate extends SingleEntityQueryRendererDelegate<StringBuilder, Neo4jQueryParsingResult> {

	private static final Log LOG = LoggerFactory.make( MethodHandles.lookup() );

	private final Neo4jPropertyHelper propertyHelper;
	private final SessionFactoryImplementor sessionFactory;
	private final Neo4jAliasResolver aliasResolver;
	private List<OrderByClause> orderByExpressions;

	/**
	 * Temporary state used while parsing the query.
	 */
	private JoinType joinType;

	public Neo4jQueryRendererDelegate(SessionFactoryImplementor sessionFactory, Neo4jAliasResolver aliasResolver, EntityNamesResolver entityNames, Neo4jPropertyHelper propertyHelper, Map<String, Object> namedParameters) {
		super( propertyHelper, entityNames, singleEntityQueryBuilder( propertyHelper ), namedParameters );
		this.sessionFactory = sessionFactory;
		this.aliasResolver = aliasResolver;
		this.propertyHelper = propertyHelper;
	}

	private static SingleEntityQueryBuilder<StringBuilder> singleEntityQueryBuilder(Neo4jPropertyHelper propertyHelper) {
		return SingleEntityQueryBuilder.getInstance(
				new Neo4jPredicateFactory( propertyHelper ),
				propertyHelper
		);
	}

	private EntityKeyMetadata getKeyMetaData(Class<?> entityType) {
		return getEntityPersister( entityType ).getEntityKeyMetadata();
	}

	private OgmEntityPersister getEntityPersister(Class<?> entityType) {
		OgmEntityPersister persister = (OgmEntityPersister) ( sessionFactory ).getMetamodel().entityPersister( entityType );
		return persister;
	}

	@Override
	public Neo4jQueryParsingResult getResult() {
		String targetAlias = aliasResolver.findAliasForType( targetTypeName );
		String label = getKeyMetaData( targetType ).getTable();
		StringBuilder queryBuilder = new StringBuilder();
		match( queryBuilder, targetAlias, label );
		where( queryBuilder, targetAlias );
		optionalMatch( queryBuilder, targetAlias );
		returns( queryBuilder, targetAlias );
		orderBy( queryBuilder );
		return new Neo4jQueryParsingResult( targetType, projections, queryBuilder.toString() );
	}

	private void match(StringBuilder queryBuilder, String targetAlias, String label) {
		queryBuilder.append( "MATCH " );
		node( queryBuilder, targetAlias, label );
		RelationshipAliasTree node = aliasResolver.getRelationshipAliasTree( targetAlias );
		boolean first = true;
		if ( node != null ) {
			for ( RelationshipAliasTree child : node.getChildren() ) {
				if ( !aliasResolver.isOptionalMatch( child.getAlias() ) ) {
					StringBuilder relationshipMatch = new StringBuilder();
					if ( first ) {
						first = false;
					}
					else {
						relationshipMatch.append( ", " );
						node( relationshipMatch, targetAlias, label );
					}
					relationship( relationshipMatch, child.getRelationshipName() );
					node( relationshipMatch, child.getAlias(), child.getTargetEntityName() );
					appendMatchRelationship( queryBuilder, relationshipMatch.toString(), child );
				}
			}
		}
	}

	private void appendMatchRelationship(StringBuilder queryBuilder, String currentMatch, RelationshipAliasTree node) {
		if ( node.getChildren().isEmpty() ) {
			queryBuilder.append( currentMatch );
		}
		else {
			for ( RelationshipAliasTree child : node.getChildren() ) {
				boolean optional = aliasResolver.isOptionalMatch( child.getAlias() );
				if ( !optional ) {
					StringBuilder builder = new StringBuilder( currentMatch );
					relationship( builder, child.getRelationshipName() );
					node( builder, child.getAlias(), child.getTargetEntityName() );
					appendMatchRelationship( queryBuilder, builder.toString(), child );
				}
				else {
					queryBuilder.append( currentMatch );
				}
			}
		}
	}

	private void optionalMatch(StringBuilder queryBuilder, String targetAlias) {
		RelationshipAliasTree node = aliasResolver.getRelationshipAliasTree( targetAlias );
		if ( node != null ) {
			appendOptionalMatch( queryBuilder, targetAlias, node.getChildren() );
		}
	}

	private void appendOptionalMatch(StringBuilder queryBuilder, String targetAlias, List<RelationshipAliasTree> children) {
		for ( RelationshipAliasTree child : children ) {
			if ( aliasResolver.isOptionalMatch( child.getAlias() ) ) {
				queryBuilder.append( " OPTIONAL MATCH " );
				node( queryBuilder, targetAlias );
				relationship( queryBuilder, child.getRelationshipName() );
				node( queryBuilder, child.getAlias(), child.getTargetEntityName() );
			}
			appendOptionalMatch( queryBuilder, child.getAlias(), child.getChildren() );
		}
	}

	private void where(StringBuilder queryBuilder, String targetAlias) {
		StringBuilder whereCondition = builder.build();

		if ( whereCondition != null ) {
			queryBuilder.append( " WHERE " );
			queryBuilder.append( whereCondition );
		}

		appendDiscriminatorClause( queryBuilder, targetAlias, whereCondition );
	}

	private void appendDiscriminatorClause(StringBuilder queryBuilder, String targetAlias, StringBuilder whereCondition) {
		OgmEntityPersister entityPersister = getEntityPersister( targetType );
		String discriminatorColumnName = entityPersister.getDiscriminatorColumnName();
		if ( discriminatorColumnName != null ) {
			// InheritanceType.SINGLE_TABLE
			addConditionOnDiscriminatorValue( queryBuilder, targetAlias, whereCondition, entityPersister, discriminatorColumnName );
		}
		else if ( entityPersister.hasSubclasses() ) {
			// InheritanceType.TABLE_PER_CLASS
			@SuppressWarnings("unchecked")
			Set<String> subclassEntityNames = entityPersister.getEntityMetamodel().getSubclassEntityNames();
			throw LOG.queriesOnPolymorphicEntitiesAreNotSupportedWithTablePerClass( "Neo4j", subclassEntityNames );
		}
	}

	private void addConditionOnDiscriminatorValue(StringBuilder queryBuilder, String targetAlias, StringBuilder whereCondition, OgmEntityPersister entityPersister,
			String discriminatorColumnName) {
		if ( whereCondition != null ) {
			queryBuilder.append( " AND " );
		}
		else {
			queryBuilder.append( " WHERE " );
		}
		@SuppressWarnings("unchecked")
		Set<String> subclassEntityNames = entityPersister.getEntityMetamodel().getSubclassEntityNames();
		identifier( queryBuilder, targetAlias );
		queryBuilder.append( "." );
		identifier( queryBuilder, discriminatorColumnName );

		org.hibernate.type.Type discriminatorType = entityPersister.getDiscriminatorType();
		if ( subclassEntityNames.size() == 1 ) {
			queryBuilder.append( " = " );
			appendDiscriminatorValue( queryBuilder, discriminatorType, entityPersister.getDiscriminatorValue() );
		}
		else {
			queryBuilder.append( " IN [" );
			Set<Object> discrimiantorValues = new HashSet<>();
			discrimiantorValues.add( entityPersister.getDiscriminatorValue() );

			String separator = "";
			for ( String subclass : subclassEntityNames ) {
				OgmEntityPersister subclassPersister = (OgmEntityPersister) sessionFactory.getMetamodel().entityPersister( subclass );
				Object discriminatorValue = subclassPersister.getDiscriminatorValue();
				queryBuilder.append( separator );
				appendDiscriminatorValue( queryBuilder, discriminatorType, discriminatorValue );
				separator = ", ";
			}
			queryBuilder.append( "]" );
		}
	}

	private void appendDiscriminatorValue(StringBuilder queryBuilder, org.hibernate.type.Type discriminatorType, Object discriminatorValue) {
		Object value = convertToBackendType( discriminatorType, discriminatorValue );
		CypherDSL.literal( queryBuilder, value );
	}

	private Object convertToBackendType(org.hibernate.type.Type discriminatorType, Object discriminatorValue) {
		GridType ogmType = sessionFactory.getServiceRegistry().getService( TypeTranslator.class ).getType( discriminatorType );
		return ogmType.convertToBackendType( discriminatorValue, sessionFactory );
	}

	private void orderBy(StringBuilder queryBuilder) {
		if ( orderByExpressions != null && !orderByExpressions.isEmpty() ) {
			queryBuilder.append( " ORDER BY " );
			int counter = 1;
			for ( OrderByClause orderBy : orderByExpressions ) {
				orderBy.asString( queryBuilder );
				if ( counter++ < orderByExpressions.size() ) {
					queryBuilder.append( ", " );
				}
			}
		}
	}

	private void returns(StringBuilder builder, String targetAlias) {
		builder.append( " RETURN " );
		if ( projections.isEmpty() ) {
			identifier( builder, targetAlias );
		}
		else {
			int counter = 1;
			for ( String projection : projections ) {
				builder.append( projection );
				if ( counter++ < projections.size() ) {
					builder.append( ", " );
				}
			}
		}
	}

	@Override
	public void setPropertyPath(PropertyPath path) {
		if ( status == Status.DEFINING_SELECT ) {
			defineSelect( path );
		}
		else {
			this.propertyPath = path;
		}
	}

	private void defineSelect(PropertyPath path) {
		List<String> pathWithoutAlias = resolveAlias( path );
		if ( !pathWithoutAlias.isEmpty() ) { // It might be empty if we have selected the target entity
			// for the explicit joins, the relationships are already declared as required if needed so
			// we will only declare new relationships for the implicit joins which are optional thus the requiredDepth
			// set to 0
			PropertyIdentifier identifier = propertyHelper.getPropertyIdentifier( targetTypeName, pathWithoutAlias, 0 );
			String projection = identifier( identifier.getAlias(), identifier.getPropertyName() );
			projections.add( projection );
		}
	}

	@Override
	protected void addSortField(PropertyPath propertyPath, String collateName, boolean isAscending) {
		if ( orderByExpressions == null ) {
			orderByExpressions = new ArrayList<OrderByClause>();
		}

		List<String> propertyPathWithoutAlias = resolveAlias( propertyPath );
		PropertyIdentifier identifier = propertyHelper.getPropertyIdentifier( targetTypeName, propertyPathWithoutAlias, 0 );

		OrderByClause order = new OrderByClause( identifier.getAlias(), identifier.getPropertyName(), isAscending );
		orderByExpressions.add( order );
	}

	@Override
	public void pushFromStrategy(JoinType joinType, Tree associationFetchTree, Tree propertyFetchTree, Tree alias) {
		super.pushFromStrategy( joinType, associationFetchTree, propertyFetchTree, alias );
		this.joinType = joinType;
	}

	@Override
	public void popStrategy() {
		super.popStrategy();
		joinType = null;
	}

	@Override
	public void registerJoinAlias(Tree alias, PropertyPath path) {
		super.registerJoinAlias( alias, path );
		List<String> propertyPath = resolveAlias( path );

		int requiredDepth;
		// For now, we deal with INNER JOIN and LEFT OUTER JOIN, it's not really perfect as you might have issues
		// with join precedence but it's probably the best we can do for now.
		if ( JoinType.INNER.equals( joinType ) ) {
			requiredDepth = propertyPath.size();
		}
		else if ( JoinType.LEFT.equals( joinType ) ) {
			requiredDepth = 0;
		}
		else {
			LOG.joinTypeNotFullySupported( joinType );
			// defaults to mark the alias as required for now
			requiredDepth = propertyPath.size();
		}

		// Even if we don't need the property identifier, it's important to create the aliases for the corresponding
		// associations/embedded with the correct requiredDepth.
		propertyHelper.getPropertyIdentifier( targetTypeName, propertyPath, requiredDepth );
	}

	// TODO Methods below were not required here if fromNamedQuery() could be overridden from super

	@Override
	public void predicateLess(String comparativePredicate) {
		addComparisonPredicate( comparativePredicate, Type.LESS );
	}

	@Override
	public void predicateLessOrEqual(String comparativePredicate) {
		addComparisonPredicate( comparativePredicate, Type.LESS_OR_EQUAL );
	}

	/**
	 * This implements the equality predicate; the comparison
	 * predicate could be a constant, a subfunction or
	 * some random type parameter.
	 * The tree node has all details but with current tree rendering
	 * it just passes it's text so we have to figure out the options again.
	 */
	@Override
	public void predicateEquals(final String comparativePredicate) {
		addComparisonPredicate( comparativePredicate, Type.EQUALS );
	}

	@Override
	public void predicateNotEquals(String comparativePredicate) {
		builder.pushNotPredicate();
		addComparisonPredicate( comparativePredicate, Type.EQUALS );
		builder.popBooleanPredicate();
	}

	@Override
	public void predicateGreaterOrEqual(String comparativePredicate) {
		addComparisonPredicate( comparativePredicate, Type.GREATER_OR_EQUAL );
	}

	@Override
	public void predicateGreater(String comparativePredicate) {
		addComparisonPredicate( comparativePredicate, Type.GREATER );
	}

	private void addComparisonPredicate(String comparativePredicate, Type comparisonType) {
		Object comparisonValue = fromNamedQuery( comparativePredicate );
		List<String> property = resolveAlias( propertyPath );
		builder.addComparisonPredicate( property, comparisonType, comparisonValue );
	}

	@Override
	public void predicateIn(List<String> list) {
		List<Object> values = fromNamedQuery( list );
		List<String> property = resolveAlias( propertyPath );
		builder.addInPredicate( property, values );
	}

	@Override
	public void predicateBetween(String lower, String upper) {
		Object lowerComparisonValue = fromNamedQuery( lower );
		Object upperComparisonValue = fromNamedQuery( upper );

		List<String> property = resolveAlias( propertyPath );
		builder.addRangePredicate( property, lowerComparisonValue, upperComparisonValue );
	}

	@Override
	public void predicateLike(String patternValue, Character escapeCharacter) {
		Object pattern = fromNamedQuery( patternValue );
		List<String> property = resolveAlias( propertyPath );
		builder.addLikePredicate( property, (String) pattern, escapeCharacter );
	}

	@Override
	public void predicateIsNull() {
		List<String> property = resolveAlias( propertyPath );
		builder.addIsNullPredicate( property );
	}

	private Object fromNamedQuery(String comparativePredicate) {
		// It's a named parameter; Value given via setParameter(), taking that as is
		if ( comparativePredicate.startsWith( ":" ) ) {
			return new Neo4jQueryParameter( comparativePredicate.substring( 1 ) );
		}
		// It's a value given in JP-QL; Convert the literal value
		else {
			List<String> path = new ArrayList<String>();
			path.addAll( propertyPath.getNodeNamesWithoutAlias() );

			PropertyPath fullPath = propertyPath;

			// create the complete path in case it's a join
			while ( fullPath.getFirstNode().isAlias() && aliasToPropertyPath.containsKey( fullPath.getFirstNode().getName() ) ) {
				fullPath = aliasToPropertyPath.get( fullPath.getFirstNode().getName() );
				path.addAll( 0, fullPath.getNodeNamesWithoutAlias() );
			}

			return propertyHelper.convertToPropertyType( targetTypeName, path, comparativePredicate );
		}
	}

	private List<Object> fromNamedQuery(List<String> list) {
		List<Object> elements = new ArrayList<Object>( list.size() );

		for ( String string : list ) {
			elements.add( fromNamedQuery( string ) );
		}

		return elements;
	}
}
