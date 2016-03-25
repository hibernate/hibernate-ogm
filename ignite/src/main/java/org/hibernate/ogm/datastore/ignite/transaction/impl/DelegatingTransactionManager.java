/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.ignite.transaction.impl;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.InvalidTransactionException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

import org.hibernate.HibernateException;

public class DelegatingTransactionManager implements TransactionManager {
    
    private static final String WAS_TRANSACTION_MANAGER_CLASS_NAME = "com.ibm.tx.jta.impl.TranManagerSet";
    private static final String WAS_TRANSACTION_MANAGER_METHOD_NAME = "instance";
    private static final String JBOSS_TRANSACTION_MANAGER_JNDI_NAME = "java:jboss/TransactionManager";

    private static volatile TransactionManager INSTANCE;

    private final TransactionManager delegate;
    private final ApplicationServer selectedApplicationServer;

    public DelegatingTransactionManager(final TransactionManager delegate, final ApplicationServer applicationServer) {
        this.delegate = delegate;
        selectedApplicationServer = applicationServer;
    }

    public static TransactionManager transactionManager() {
        if (INSTANCE == null) {
            synchronized (DelegatingTransactionManager.class) {
                if (INSTANCE == null) {
                    try {
                        TransactionManager delegate = null;
                        final ApplicationServer applicationServer = ApplicationServer.currentApplicationServer();
                        switch (applicationServer) {
	                        case WEBSPHERE:
	                            delegate = initWebSphereTransactionManager();
	                            break;
	                        case JBOSS:
	                            delegate = initJBossTransactionManager();
	                            break;
	                        default:
	                        	throw new UnsupportedOperationException("Unsupported application server");
                        }
                        INSTANCE = new DelegatingTransactionManager(delegate, applicationServer);
                    } catch (ClassNotFoundException | NoSuchMethodException | SecurityException | IllegalAccessException
                            | IllegalArgumentException | InvocationTargetException | NamingException e) {
                        throw new HibernateException("Cannot instantiate TransactionManager", e);
                    }
                }
            }
        }
        return INSTANCE;
    }

    @Override
    public void begin() throws NotSupportedException, SystemException {
        delegate.begin();
    }

    @Override
    public void commit() throws RollbackException, HeuristicMixedException, HeuristicRollbackException, SecurityException,
            IllegalStateException, SystemException {
        delegate.commit();
    }

    @Override
    public int getStatus() throws SystemException {
        return delegate.getStatus();
    }

    @Override
    public Transaction getTransaction() throws SystemException {
        final Transaction transaction = delegate.getTransaction();
        if (selectedApplicationServer == ApplicationServer.WEBSPHERE) {
            return transaction != null ? new WebSphereTransaction(transaction) : null;
        }
        return transaction;
    }

    @Override
    public void resume(final Transaction tobj) throws InvalidTransactionException, IllegalStateException, SystemException {
        delegate.resume(tobj);
    }

    @Override
    public void rollback() throws IllegalStateException, SecurityException, SystemException {
        delegate.rollback();
    }

    @Override
    public void setRollbackOnly() throws IllegalStateException, SystemException {
        delegate.setRollbackOnly();
    }

    @Override
    public void setTransactionTimeout(final int seconds) throws SystemException {
        delegate.setTransactionTimeout(seconds);
    }

    @Override
    public Transaction suspend() throws SystemException {
        return delegate.suspend();
    }

    private static TransactionManager initWebSphereTransactionManager() throws ClassNotFoundException, NoSuchMethodException,
            SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        final Class<?> clazz = Class.forName(WAS_TRANSACTION_MANAGER_CLASS_NAME);
        final Method m = clazz.getMethod(WAS_TRANSACTION_MANAGER_METHOD_NAME, (Class[]) null);
        final TransactionManager transactionManager = (TransactionManager) m.invoke(null, (Object[]) null);

        return transactionManager;
    }

    private static TransactionManager initJBossTransactionManager() throws NamingException {
        final InitialContext initialContext = new InitialContext();
        final TransactionManager transactionManager = (TransactionManager) initialContext.lookup(JBOSS_TRANSACTION_MANAGER_JNDI_NAME);
        return transactionManager;
    }
}
