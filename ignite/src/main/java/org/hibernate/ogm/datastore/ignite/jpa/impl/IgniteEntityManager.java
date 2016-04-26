package org.hibernate.ogm.datastore.ignite.jpa.impl;

import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaDelete;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.CriteriaUpdate;

import org.hibernate.jpa.internal.EntityManagerImpl;
import org.hibernate.ogm.jpa.impl.OgmEntityManager;
import org.hibernate.ogm.jpa.impl.OgmEntityManagerFactory;

public class IgniteEntityManager extends OgmEntityManager {
    private final EntityManagerImpl hibernateEm;

    public IgniteEntityManager(final OgmEntityManagerFactory factory, final EntityManagerImpl hibernateEm) {
        super(factory, hibernateEm);
        this.hibernateEm = hibernateEm;
    }

    @Override
    public <T> TypedQuery<T> createQuery(final CriteriaQuery<T> criteriaQuery) {
        return hibernateEm.createQuery(criteriaQuery);
    }

    @Override
    public Query createQuery(@SuppressWarnings("rawtypes") final CriteriaUpdate updateQuery) {
        return hibernateEm.createQuery(updateQuery);
    }

    @Override
    public Query createQuery(@SuppressWarnings("rawtypes") final CriteriaDelete deleteQuery) {
        return hibernateEm.createQuery(deleteQuery);
    }
}