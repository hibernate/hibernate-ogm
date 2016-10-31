package org.hibernate.ogm.datastore.ignite.loader.criteria.impl;

import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.CriteriaImpl;
import org.hibernate.loader.criteria.CriteriaJoinWalker;
import org.hibernate.loader.criteria.CriteriaQueryTranslator;
import org.hibernate.persister.entity.Joinable;
import org.hibernate.persister.entity.OuterJoinLoadable;

public class OgmCriteriaJoinWalker extends CriteriaJoinWalker {

	public OgmCriteriaJoinWalker(OuterJoinLoadable persister,
			CriteriaQueryTranslator translator,
			SessionFactoryImplementor factory, CriteriaImpl criteria,
			String rootEntityName, LoadQueryInfluencers loadQueryInfluencers) {
		super(persister, translator, factory, criteria, rootEntityName,
				loadQueryInfluencers);
	}
	
	public OgmCriteriaJoinWalker(OuterJoinLoadable persister,
			CriteriaQueryTranslator translator,
			SessionFactoryImplementor factory, CriteriaImpl criteria,
			String rootEntityName, LoadQueryInfluencers loadQueryInfluencers,
			String alias) {
		super(persister, translator, factory, criteria, rootEntityName,
				loadQueryInfluencers, alias);
	}
	
	public String getFromString() {
		StringBuffer buf = new StringBuffer();
		buf.append(((OuterJoinLoadable)getPersister()).fromTableFragment(getAlias()));
		buf.append(((Joinable)getPersister()).fromJoinFragment( getAlias(), true, true ));
		buf.append(mergeOuterJoins(getAssociations()).toFromFragmentString());
		return buf.length() > 0 ? "FROM " + buf.toString() : "";
	}
	
}
