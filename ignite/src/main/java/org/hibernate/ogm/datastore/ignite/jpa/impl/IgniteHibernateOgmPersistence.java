package org.hibernate.ogm.datastore.ignite.jpa.impl;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.Callable;

import javax.persistence.EntityManagerFactory;
import javax.persistence.spi.PersistenceUnitInfo;
import javax.persistence.spi.ProviderUtil;

import org.hibernate.ogm.jpa.HibernateOgmPersistence;

public class IgniteHibernateOgmPersistence extends HibernateOgmPersistence {
    private static final String IMPLEMENTATION_NAME_FIELD_NAME = "IMPLEMENTATION_NAME";

    @Override
    public EntityManagerFactory createContainerEntityManagerFactory(final PersistenceUnitInfo info, @SuppressWarnings("rawtypes") final Map map) {
        try {
            return wrapWithIgniteEntityManagerFactory(wrapWithIgniteClass(new Callable<EntityManagerFactory>() {

                @Override
                public EntityManagerFactory call() throws Exception {
                    return IgniteHibernateOgmPersistence.super.createContainerEntityManagerFactory(info, map);
                }
            }));
        } catch (final Exception e) {
            throw new RuntimeException("Cannot create EntityManagerFactory", e);
        }
    }

    @Override
    public EntityManagerFactory createEntityManagerFactory(final String emName, @SuppressWarnings("rawtypes") final Map map) {
        try {
            return wrapWithIgniteEntityManagerFactory(wrapWithIgniteClass(new Callable<EntityManagerFactory>() {

                @Override
                public EntityManagerFactory call() throws Exception {
                    return IgniteHibernateOgmPersistence.super.createEntityManagerFactory(emName, map);
                }
            }));
        } catch (final Exception e) {
            throw new RuntimeException("Cannot create EntityManagerFactory", e);
        }
    }

    @Override
    public void generateSchema(final PersistenceUnitInfo info, @SuppressWarnings("rawtypes") final Map map) {
        super.generateSchema(info, map);
    }

    @Override
    public boolean generateSchema(final String persistenceUnitName, @SuppressWarnings("rawtypes") final Map map) {
        return super.generateSchema(persistenceUnitName, map);
    }

    @Override
    public ProviderUtil getProviderUtil() {
        return super.getProviderUtil();
    }

    private EntityManagerFactory wrapWithIgniteClass(final Callable<EntityManagerFactory> callable) throws Exception {
        final Class<?> hibernateOgmPersistenceClass = getClass().getSuperclass();
        final Field implementationNameField = hibernateOgmPersistenceClass.getDeclaredField(IMPLEMENTATION_NAME_FIELD_NAME);
        implementationNameField.setAccessible(true);
        final Object currentValue = implementationNameField.get(hibernateOgmPersistenceClass);
        implementationNameField.set(hibernateOgmPersistenceClass, IgniteHibernateOgmPersistence.class.getName());
        final EntityManagerFactory entityManagerFactory = callable.call();
        implementationNameField.set(hibernateOgmPersistenceClass, currentValue);
        return entityManagerFactory;
    }

    private EntityManagerFactory wrapWithIgniteEntityManagerFactory(final EntityManagerFactory entityManagerFactory) throws Exception {
        return new IgniteEntityManagerFactory(entityManagerFactory);
    }
}
