package org.hibernate.ogm.datastore.ignite.jpa.impl;

import java.lang.reflect.Field;
import java.util.Map;

import javax.persistence.Cache;
import javax.persistence.EntityGraph;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnitUtil;
import javax.persistence.Query;
import javax.persistence.SynchronizationType;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.metamodel.Metamodel;

import org.hibernate.jpa.HibernateEntityManagerFactory;
import org.hibernate.jpa.internal.EntityManagerImpl;
import org.hibernate.ogm.jpa.impl.OgmEntityManagerFactory;

public class IgniteEntityManagerFactory implements EntityManagerFactory {
    private final EntityManagerFactory entityManagerFactory;
    private final HibernateEntityManagerFactory hibernateEmf;
    private static final String HIBERNATE_ENTITY_MANAGER_FACTORY_FIELD_NAME = "hibernateEmf";

    public IgniteEntityManagerFactory(final EntityManagerFactory entityManagerFactory) {
        if (entityManagerFactory == null) {
            throw new IllegalArgumentException("EntityManagerFactory cannot be null");
        }
        this.entityManagerFactory = entityManagerFactory;
        this.hibernateEmf = getHibernateEntityManagerFactory(entityManagerFactory);
        if (this.hibernateEmf == null) {
            throw new IllegalArgumentException("HibernateEntityManagerFactory cannot be null");
        }
    }

    @Override
    public EntityManager createEntityManager() {
        return new IgniteEntityManager((OgmEntityManagerFactory) entityManagerFactory,
                (EntityManagerImpl) hibernateEmf.createEntityManager());
    }

    @Override
    public EntityManager createEntityManager(@SuppressWarnings("rawtypes") final Map map) {
        return new IgniteEntityManager((OgmEntityManagerFactory) entityManagerFactory,
                (EntityManagerImpl) hibernateEmf.createEntityManager(map));
    }

    @Override
    public EntityManager createEntityManager(final SynchronizationType synchronizationType) {
        return new IgniteEntityManager((OgmEntityManagerFactory) entityManagerFactory,
                (EntityManagerImpl) hibernateEmf.createEntityManager(synchronizationType));
    }

    @Override
    public EntityManager createEntityManager(final SynchronizationType synchronizationType, @SuppressWarnings("rawtypes") final Map map) {
        return new IgniteEntityManager((OgmEntityManagerFactory) entityManagerFactory,
                (EntityManagerImpl) hibernateEmf.createEntityManager(synchronizationType, map));
    }

    @Override
    public CriteriaBuilder getCriteriaBuilder() {
        return entityManagerFactory.getCriteriaBuilder();
    }

    @Override
    public Metamodel getMetamodel() {
        return entityManagerFactory.getMetamodel();
    }

    @Override
    public boolean isOpen() {
        return entityManagerFactory.isOpen();
    }

    @Override
    public void close() {
        entityManagerFactory.close();
    }

    @Override
    public Map<String, Object> getProperties() {
        return entityManagerFactory.getProperties();
    }

    @Override
    public Cache getCache() {
        return entityManagerFactory.getCache();
    }

    @Override
    public PersistenceUnitUtil getPersistenceUnitUtil() {
        return entityManagerFactory.getPersistenceUnitUtil();
    }

    @Override
    public void addNamedQuery(final String name, final Query query) {
        entityManagerFactory.addNamedQuery(name, query);
    }

    @Override
    public <T> T unwrap(final Class<T> cls) {
        return entityManagerFactory.unwrap(cls);
    }

    @Override
    public <T> void addNamedEntityGraph(final String graphName, final EntityGraph<T> entityGraph) {
        entityManagerFactory.addNamedEntityGraph(graphName, entityGraph);
    }

    private HibernateEntityManagerFactory getHibernateEntityManagerFactory(final EntityManagerFactory entityManagerFactory) {
        try {
            final Field hibernateRefField = this.entityManagerFactory.getClass().getDeclaredField(HIBERNATE_ENTITY_MANAGER_FACTORY_FIELD_NAME);
            hibernateRefField.setAccessible(true);
            final HibernateEntityManagerFactory hibernateEmf = (HibernateEntityManagerFactory) hibernateRefField
                    .get(this.entityManagerFactory);
            return hibernateEmf;
        } catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e) {
            throw new IllegalArgumentException("Error while initializing IgniteEntityManagerFactory", e);
        }
    }
}
