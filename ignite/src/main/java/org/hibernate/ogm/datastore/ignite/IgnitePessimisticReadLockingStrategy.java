package org.hibernate.ogm.datastore.ignite;

import java.io.Serializable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

import org.apache.ignite.IgniteCache;
import org.apache.ignite.binary.BinaryObject;
import org.hibernate.LockMode;
import org.hibernate.StaleObjectStateException;
import org.hibernate.dialect.lock.LockingStrategy;
import org.hibernate.dialect.lock.LockingStrategyException;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.ogm.datastore.ignite.exception.IgniteHibernateException;
import org.hibernate.ogm.datastore.ignite.exception.IgniteLockingStrategyException;
import org.hibernate.ogm.datastore.ignite.impl.IgniteDatastoreProvider;
import org.hibernate.ogm.datastore.ignite.logging.impl.Log;
import org.hibernate.ogm.datastore.ignite.logging.impl.LoggerFactory;
import org.hibernate.ogm.model.impl.EntityKeyBuilder;
import org.hibernate.ogm.model.key.spi.EntityKey;
import org.hibernate.ogm.persister.impl.OgmEntityPersister;
import org.hibernate.ogm.type.spi.GridType;
import org.hibernate.ogm.type.spi.TypeTranslator;
import org.hibernate.persister.entity.Lockable;

public class IgnitePessimisticReadLockingStrategy implements LockingStrategy {

	private static final Log log = LoggerFactory.getLogger(); 
	
	private final Lockable lockable;
	private final LockMode lockMode;
	
	private IgniteDatastoreProvider provider;
	
	public IgnitePessimisticReadLockingStrategy(Lockable lockable, LockMode lockMode, IgniteDatastoreProvider provider){
		this.lockable = lockable;
		this.lockMode = lockMode;
		this.provider = provider;
		//this.provider = getProvider(lockable.getFactory());
	}
	
	@Override
	public void lock(Serializable id, Object version, Object object, int timeout, SessionImplementor session)
			throws StaleObjectStateException, LockingStrategyException {
		
		TypeTranslator typeTranslator = lockable.getFactory().getServiceRegistry().getService( TypeTranslator.class );
		GridType idGridType = typeTranslator.getType(lockable.getIdentifierType());
		EntityKey key = EntityKeyBuilder.fromData(((OgmEntityPersister)lockable).getRootEntityKeyMetadata(), 
													idGridType, 
													id, 
													session);
		
		IgniteCache<String, BinaryObject> cache = provider.getEntityCache(key.getMetadata());
		if (cache == null)
			throw new IgniteHibernateException("Не найден кэш " + key.getMetadata().getTable());
		Lock lock = cache.lock(provider.getKeyProvider().getEntityKeyString(key));
		try {
			lock.tryLock(timeout, TimeUnit.MILLISECONDS);
		}
		catch (InterruptedException e){
			throw new IgniteLockingStrategyException(object, e.getMessage(), e);
		}
	}

//	private static IgniteDatastoreProvider getProvider(SessionFactoryImplementor factory) {
//		DatastoreProvider service = factory.getServiceRegistry().getService( DatastoreProvider.class );
//
//		if ( service instanceof IgniteDatastoreProvider ) {
//			return IgniteDatastoreProvider.class.cast( service );
//		}
//		else {
//			throw log.unexpectedDatastoreProvider( service.getClass(), IgniteDatastoreProvider.class );
//		}
//	}
	
}
