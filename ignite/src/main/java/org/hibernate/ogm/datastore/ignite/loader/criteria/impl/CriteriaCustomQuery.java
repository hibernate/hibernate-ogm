package org.hibernate.ogm.datastore.ignite.loader.criteria.impl;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.MappingException;
import org.hibernate.engine.internal.ForeignKeys;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.QueryParameters;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.internal.CriteriaImpl;
import org.hibernate.loader.DefaultEntityAliases;
import org.hibernate.loader.criteria.CriteriaQueryTranslator;
import org.hibernate.loader.custom.CustomQuery;
import org.hibernate.loader.custom.Return;
import org.hibernate.loader.custom.RootReturn;
import org.hibernate.loader.custom.ScalarReturn;
import org.hibernate.ogm.model.key.spi.EntityKeyMetadata;
import org.hibernate.ogm.persister.impl.OgmEntityPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.OuterJoinLoadable;
import org.hibernate.type.ComponentType;
import org.hibernate.type.EntityType;
import org.hibernate.type.Type;

/**
 * Extension point allowing criteria query returning managed entities
 * 
 * @author Dmitriy Kozlov
 *
 */
public class CriteriaCustomQuery implements CustomQuery, Serializable {

	private static final long serialVersionUID = 2717988631895777153L;
	
	private final CriteriaImpl criteria;
	private final CriteriaQueryTranslator translator;
	
	private final String queryString;
	private final Set querySpaces;
	private final Type[] resultTypes;
	private final String whereCondition;
	private final String orderBy;
	private final List<Return> customQueryReturns;
	private final String fromString;
	private final Object[] positionedQueryParameters;

	private final EntityKeyMetadata singleEntityKeyMetadata;

	public CriteriaCustomQuery(CriteriaImpl criteria, SessionFactoryImplementor factory, LoadQueryInfluencers loadQueryInfluencers, SessionImplementor session) {
		this.criteria = criteria;
		translator = new CriteriaQueryTranslator(
				factory, 
				criteria, 
				criteria.getEntityOrClassName(), 
				criteria.getAlias()
			);
		
		this.querySpaces = Collections.unmodifiableSet(translator.getQuerySpaces());
		this.whereCondition = translator.getWhereCondition();
		this.orderBy = translator.getOrderBy();
		List<Object> args = new ArrayList<Object>();
		for (int i = 0; i < translator.getQueryParameters().getPositionalParameterValues().length; i++){
			Type type = translator.getQueryParameters().getPositionalParameterTypes()[i]; 
			if (type.isEntityType()){
				Serializable id = ForeignKeys.getEntityIdentifierIfNotUnsaved( ((EntityType)type).getAssociatedEntityName(), translator.getQueryParameters().getPositionalParameterValues()[i], session );
				args.add(id);
			} 
			else if (type.isComponentType()) {
				int size = ((ComponentType)type).getPropertyNames().length;
				for (int k = 0; k < size; k++) {
					args.add(((ComponentType)type).getPropertyValue(translator.getQueryParameters().getPositionalParameterValues()[i], k));
				}
			}
			else {
				args.add(translator.getQueryParameters().getPositionalParameterValues()[i]);
			}
		}
		positionedQueryParameters = args.toArray();
		
		OgmCriteriaJoinWalker walker = new OgmCriteriaJoinWalker(
				getOuterJoinLoadable(factory.getEntityPersister(criteria.getEntityOrClassName())),
				translator,
				factory,
				criteria,
				criteria.getEntityOrClassName(),
				loadQueryInfluencers,
				criteria.getAlias());
		this.queryString = walker.getSQLString();
		this.fromString = walker.getFromString();
		this.resultTypes = walker.getResultTypes();
		
		OgmEntityPersister persister = (OgmEntityPersister) factory.getEntityPersister( criteria.getEntityOrClassName() );
		this.singleEntityKeyMetadata = persister.getEntityKeyMetadata();
		
		List<Return> returnList = new ArrayList<>();
		if (criteria.getProjection() == null){
			DefaultEntityAliases entityAliases = new DefaultEntityAliases(walker.getPersister(), walker.getSuffixes()[0]);
			returnList.add(new RootReturn(walker.getAlias(), criteria.getEntityOrClassName(), entityAliases, criteria.getLockModes().get(criteria.getAlias())));
		}
		else {
			Type[] types = criteria.getProjection().getTypes(criteria, translator);
			String[] aliases = criteria.getProjection().getColumnAliases(0);
			for (int i = 0; i < types.length; i++){
				returnList.add(new ScalarReturn(types[i], aliases[i]));
			}
		}
		
		this.customQueryReturns = Collections.unmodifiableList(returnList);
	}
	
	private OuterJoinLoadable getOuterJoinLoadable(EntityPersister persister) throws MappingException {
		if ( !(persister instanceof OuterJoinLoadable) ) {
			throw new MappingException( "class persister is not OuterJoinLoadable: " + persister.getEntityName() );
		}
		return ( OuterJoinLoadable ) persister;
	}
	
	public QueryParameters getQueryParameters() {
		return translator.getQueryParameters();
	}
	
//	private Set getStringQuerySpaces() {
//		Set<String> spaces = new HashSet<>();
//		for (Serializable ser : querySpaces){
//			spaces.add(ser.getClass().getName());
//		}
//		return Collections.unmodifiableSet(spaces);
//	}
	
	@Override
	public String getSQL() {
		return queryString;
	}

	@Override
	public Set getQuerySpaces() {
		return querySpaces;
	}
	
	@Override
	public Map getNamedParameterBindPoints() {
		return Collections.emptyMap();
	}

	@Override
	public List<Return> getCustomQueryReturns() {
		return customQueryReturns;
	}

	public CriteriaImpl getCriteria() {
		return criteria;
	}

	public String getWhereCondition() {
		return whereCondition;
	}

	public String getOrderBy() {
		return orderBy;
	}

	public EntityKeyMetadata getSingleEntityKeyMetadata() {
		return singleEntityKeyMetadata;
	}

	public String getFromString() {
		return fromString;
	}

	public Object[] getPositionedQueryParameters() {
		return positionedQueryParameters;
	}

}
