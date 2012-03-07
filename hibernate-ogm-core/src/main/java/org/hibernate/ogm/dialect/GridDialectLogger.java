package org.hibernate.ogm.dialect;

import org.hibernate.LockMode;
import org.hibernate.dialect.lock.LockingStrategy;
import org.hibernate.id.IntegralDataTypeHolder;
import org.hibernate.ogm.datastore.spi.Association;
import org.hibernate.ogm.datastore.spi.Tuple;
import org.hibernate.ogm.grid.AssociationKey;
import org.hibernate.ogm.grid.EntityKey;
import org.hibernate.ogm.grid.RowKey;
import org.hibernate.ogm.util.impl.Log;
import org.hibernate.ogm.util.impl.LoggerFactory;
import org.hibernate.persister.entity.Lockable;

/**
 * A wrapper dialect that logs the calls performed on the real dialect.
 * It is only used when this class's logger level is set to Trace
 *
 * @see org.hibernate.ogm.dialect.impl.GridDialectFactoryImpl#buildGridDialect(java.util.Map, org.hibernate.service.ServiceRegistry)
 *
 * @author Sebastien Lorber (<i>lorber.sebastien@gmail.com</i>)
 */
public class GridDialectLogger implements GridDialect {

    private static final Log log = LoggerFactory.make();

    private final GridDialect gridDialect; // the real wrapped grid dialect

    public GridDialectLogger(GridDialect gridDialect) {
        if ( gridDialect == null ) {
            throw new IllegalArgumentException();
        }
        this.gridDialect = gridDialect;
    }

    /**
     * Returns true if this grid dialect logger should wrap the real grid dialect
     * @return boolean
     */
    public static boolean isActive() {
        return log.isTraceEnabled();
    }

    @Override
    public LockingStrategy getLockingStrategy(Lockable lockable, LockMode lockMode) {
        return gridDialect.getLockingStrategy(lockable, lockMode);
    }

    @Override
    public Tuple getTuple(EntityKey key) {
        log.trace("Reading Tuple from store with key: " + key);
        return gridDialect.getTuple(key);
    }

    @Override
    public Tuple createTuple(EntityKey key) {
        log.trace("Creating Tuple in store (or not!) with key: " + key);
        return gridDialect.createTuple(key);
    }

    @Override
    public void updateTuple(Tuple tuple, EntityKey key) {
        log.trace("Updating Tuple from store with key: " + key);
        gridDialect.updateTuple(tuple,key);
    }

    @Override
    public void removeTuple(EntityKey key) {
        log.trace("Removing Tuple from store with key: " + key);
        gridDialect.removeTuple(key);
    }

    @Override
    public Association getAssociation(AssociationKey key) {
        log.trace("Reading association from store with key: " + key);
        return gridDialect.getAssociation(key);
    }

    @Override
    public Association createAssociation(AssociationKey key) {
        log.trace("Creating association in store (or not!) with key: " + key);
        return gridDialect.createAssociation(key);
    }

    @Override
    public void updateAssociation(Association association, AssociationKey key) {
        log.trace("Updating association from store with key: " + key);
        gridDialect.updateAssociation(association,key);
    }

    @Override
    public void removeAssociation(AssociationKey key) {
        log.trace("Removing association from store with key: " + key);
        gridDialect.removeAssociation(key);
    }

    @Override
    public Tuple createTupleAssociation(AssociationKey associationKey, RowKey rowKey) {
        log.trace("Creating Tuple association from store with associationKey: " +associationKey + " and rowkey" + rowKey);
        return gridDialect.createTupleAssociation(associationKey,rowKey);
    }

    @Override
    public void nextValue(RowKey key, IntegralDataTypeHolder value, int increment, int initialValue) {
        log.trace("Reading next value from key: " + key);
        gridDialect.nextValue(key,value,increment,initialValue);
    }
    
}
