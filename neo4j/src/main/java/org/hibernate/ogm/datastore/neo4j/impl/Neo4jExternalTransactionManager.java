/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.impl;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

import org.hibernate.ogm.datastore.neo4j.logging.impl.Log;
import org.hibernate.ogm.datastore.neo4j.logging.impl.LoggerFactory;
import org.hibernate.ogm.util.impl.Contracts;
import org.neo4j.graphdb.TransactionFailureException;
import org.neo4j.helpers.UTF8;
import org.neo4j.kernel.impl.core.KernelPanicEventGenerator;
import org.neo4j.kernel.impl.core.TransactionState;
import org.neo4j.kernel.impl.nioneo.store.FileSystemAbstraction;
import org.neo4j.kernel.impl.nioneo.store.StoreChannel;
import org.neo4j.kernel.impl.transaction.AbstractTransactionManager;
import org.neo4j.kernel.impl.transaction.DataSourceRegistrationListener;
import org.neo4j.kernel.impl.transaction.RemoteTxHook;
import org.neo4j.kernel.impl.transaction.TransactionManagerProvider;
import org.neo4j.kernel.impl.transaction.TransactionStateFactory;
import org.neo4j.kernel.impl.transaction.TxLog;
import org.neo4j.kernel.impl.transaction.XaDataSourceManager;
import org.neo4j.kernel.impl.transaction.xaframework.XaDataSource;
import org.neo4j.kernel.impl.util.StringLogger;
import org.neo4j.kernel.monitoring.Monitors;

/**
 * Adaptor for an external transaction manager to be used by Neo4j. Note that it's *this*
 * {@link javax.transaction.TransactionManager} implementation which must be used by calling code, and not the
 * underlying delegate to be called directly, else Neo4j will not respect the association. In essence this class must
 * wrap the desired delegate {@link javax.transaction.TransactionManager}.
 *
 * @author <a href="mailto:alr@jboss.org">Andrew Lee Rubinger</a>
 */
public class Neo4jExternalTransactionManager extends AbstractTransactionManager {

	private static final Log log = LoggerFactory.getLogger();

	private static final Collection<String> TX_MANAGER_JNDI_NAMES = Arrays.asList(
			"java:/TransactionManager",
			"java:appserver/TransactionManager",
			"java:pm/TransactionManager",
			"java:comp/TransactionManager",
			"java:jboss/TransactionManager"
	);

	/**
	 * Underlying implementation to which all {@link javax.transaction.TransactionManager} methods delegate
	 */
	private final TransactionManager delegate;

	/**
	 * Map of {@link javax.transaction.Transaction} to the states they're in
	 */
	private final Map<Transaction, TransactionState> txStates = new HashMap<Transaction, TransactionState>();

	private final TransactionStateFactory transactionStateFactory;

	private XaDataSourceManager xaDataSourceManager;

	private String txLogDir;

	private final Map<RecoveredBranchInfo, Boolean> branches = new HashMap<RecoveredBranchInfo, Boolean>();

	private final FileSystemAbstraction fileSystem;

	private File logSwitcherFileName = null;
	private String txLog1FileName = "tm_tx_log.1";
	private String txLog2FileName = "tm_tx_log.2";
	private final int maxTxLogRecordCount = 1000;
	private volatile TxLog txLog = null;
	private volatile boolean recovered = false;
	private final Monitors monitors;

	private TxManagerDataSourceRegistrationListener dataSourceRegistrationListener;

	/**
	 * Neo4j hook to use an external {@link javax.transaction.TransactionManager) implementation; designated by a file
	 * named {@code META-INF/services/org.neo4j.kernel.impl.transaction.TransactionManagerProvider} with contents equal
	 * to this FQN
	 */
	public static class Provider extends TransactionManagerProvider {

		public static final String NAME = Neo4jExternalTransactionManager.Provider.class.getName();

		public Provider() {
			super( NAME );
		}

		@Override
		public AbstractTransactionManager loadTransactionManager(final String txLogDir, final XaDataSourceManager xaDataSourceManager,
				final KernelPanicEventGenerator kpe, final RemoteTxHook rollbackHook, final StringLogger msgLog, final FileSystemAbstraction fileSystem,
				final TransactionStateFactory stateFactory) {

			// Pick up the underlying transaction manager from some known context
			final TransactionManager delegate = locateTransactionManager();
			return new Neo4jExternalTransactionManager( delegate, stateFactory, xaDataSourceManager, txLogDir, fileSystem, new Monitors() );
		}

		private TransactionManager locateTransactionManager() {
			TransactionManager txManager = null;
			//TODO: Locate via the name provided in the configuration
			if ( txManager == null ) {
				txManager = locateViaJndi();
			}
			if (txManager == null) {
				txManager = com.arjuna.ats.jta.TransactionManager.transactionManager();
			}
			if ( txManager == null ) {
				throw log.transactionManagerNotFound();
			}
			return txManager;
		}

		private TransactionManager locateViaJndi() {
			for ( String name : TX_MANAGER_JNDI_NAMES ) {
				try {
					TransactionManager delegate = (TransactionManager) InitialContext.doLookup( name );
					log.info( "TransactionManager found at " + name );
					return delegate;
				}
				catch (NamingException e) {
					// try next
					continue;
				}
			}
			log.error( "Cannot find provided TransactionManager in JNDI context" );
			return null;
		}
	}

	/**
	 * Internal constructor; instances created by Neo4j via the entry point
	 * {@link Neo4jExternalTransactionManager.Provider#loadTransactionManager(String, org.neo4j.kernel.impl.transaction.XaDataSourceManager, org.neo4j.kernel.impl.core.KernelPanicEventGenerator, org.neo4j.kernel.impl.transaction.RemoteTxHook, org.neo4j.kernel.impl.util.StringLogger, org.neo4j.kernel.impl.nioneo.store.FileSystemAbstraction, org.neo4j.kernel.impl.transaction.TransactionStateFactory)}
	 */
	private Neo4jExternalTransactionManager(final TransactionManager delegate, final TransactionStateFactory transactionStateFactory,
			XaDataSourceManager xaDataSourceManager, String txLogDir, FileSystemAbstraction fileSystem, Monitors monitors) {
		Contracts.assertNotNull( delegate, "tx manager delegate" );
		Contracts.assertNotNull( xaDataSourceManager, "xa data source manager" );
		Contracts.assertNotNull( transactionStateFactory, "tx state factory" );
		Contracts.assertNotNull( txLogDir, "tx log directory" );
		this.delegate = delegate;
		this.transactionStateFactory = transactionStateFactory;
		this.xaDataSourceManager = xaDataSourceManager;
		this.txLogDir = txLogDir;
		this.fileSystem = fileSystem;
		this.monitors = monitors;
	}

	/**
	 * Here we have to note the state that this current transaction is in to avoid Neo4j from attempting to enlist the
	 * XAResource twice and encountering an exception upon doing so
	 *
	 * @throws NotSupportedException
	 * @throws SystemException
	 */
	@Override
	public void begin() throws NotSupportedException, SystemException {
		delegate.begin();
		Transaction tx = getTransaction();
		txStates.put( tx, transactionStateFactory.create( tx ) );
	}

	@Override
	public void commit() throws RollbackException, HeuristicMixedException, HeuristicRollbackException, SecurityException, IllegalStateException,
			SystemException {
		delegate.commit();
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
	public int getStatus() throws SystemException {
		return delegate.getStatus();
	}

	@Override
	public void setTransactionTimeout(final int seconds) throws SystemException {
		delegate.setTransactionTimeout( seconds );
	}

	@Override
	public Transaction getTransaction() throws SystemException {
		Transaction transaction = delegate.getTransaction();
		return transaction;
	}

	@Override
	public Transaction suspend() throws SystemException {
		return delegate.suspend();
	}

	@Override
	public void resume(final Transaction tobj) throws InvalidTransactionException, IllegalStateException, SystemException {
		delegate.resume( tobj );
	}

	@Override
	public void doRecovery() {
		if ( txLog == null ) {
			openLog();
		}
		if ( recovered ) {
			return;
		}
		try {
			// Assuming here that the last datasource to register is the Neo one
			// Do recovery on start - all Resources should be registered by now
			Iterable<List<TxLog.Record>> knownDanglingRecordList = txLog.getDanglingRecords();
			boolean danglingRecordsFound = knownDanglingRecordList.iterator().hasNext();
			if ( danglingRecordsFound ) {
				log.info( "Unresolved transactions found in " + txLog.getName() + ", recovery started... " );
			}

			// Recover DataSources. Always call due to some internal state using it as a trigger.
			xaDataSourceManager.recover( knownDanglingRecordList.iterator() );

			if ( danglingRecordsFound ) {
				log.info( "Recovery completed, all transactions have been " + "resolved to a consistent state." );
			}

			getTxLog().truncate();
			recovered = true;
		}
		catch (Exception t) {
			throw log.errorDuringRecovery( t );
		}
	}

	private void findPendingDatasources() {
		try {
			Iterable<List<TxLog.Record>> danglingRecordList = txLog.getDanglingRecords();
			for ( List<TxLog.Record> tx : danglingRecordList ) {
				for ( TxLog.Record rec : tx ) {
					if ( rec.getType() == TxLog.BRANCH_ADD ) {
						RecoveredBranchInfo branchId = new RecoveredBranchInfo( rec.getBranchId() );
						if ( branches.containsKey( branchId ) ) {
							continue;
						}
						branches.put( branchId, false );
					}
				}
			}
		}
		catch (IOException e) {
			throw logAndReturn( "Failed to start transaction manager: Unable to recover pending branches.", new TransactionFailureException(
					"Unable to start TM", e ) );
		}
	}

	synchronized TxLog getTxLog() throws IOException {
		if ( txLog.getRecordCount() > maxTxLogRecordCount ) {
			if ( txLog.getName().endsWith( txLog1FileName ) ) {
				txLog.switchToLogFile( new File( txLogDir, txLog2FileName ) );
				changeActiveLog( txLog2FileName );
			}
			else if ( txLog.getName().endsWith( txLog2FileName ) ) {
				txLog.switchToLogFile( new File( txLogDir, txLog1FileName ) );
				changeActiveLog( txLog1FileName );
			}
			else {
				final IOException ex = new IOException( "Unknown txLogFile[" + txLog.getName() + "] not equals to either [" + txLog1FileName + "] or ["
						+ txLog2FileName + "]" );
				throw logAndReturn( "TM error accessing log file", ex );
			}
		}
		return txLog;
	}

	private <E extends Exception> E logAndReturn(String msg, E exception) {
		try {
			log.error( msg, exception );
			return exception;
		}
		catch (Throwable t) {
			return exception;
		}
	}

	private void changeActiveLog(String newFileName) throws IOException {
		// change active log
		StoreChannel fc = fileSystem.open( logSwitcherFileName, "rw" );
		ByteBuffer buf = ByteBuffer.wrap( UTF8.encode( newFileName ) );
		fc.truncate( 0 );
		fc.write( buf );
		fc.force( true );
		fc.close();
	}

	private void openLog() {
		logSwitcherFileName = new File( txLogDir, "active_tx_log" );
		txLog1FileName = "tm_tx_log.1";
		txLog2FileName = "tm_tx_log.2";
		try {
			if ( fileSystem.fileExists( logSwitcherFileName ) ) {
				StoreChannel fc = fileSystem.open( logSwitcherFileName, "rw" );
				byte fileName[] = new byte[256];
				ByteBuffer buf = ByteBuffer.wrap( fileName );
				fc.read( buf );
				fc.close();
				File currentTxLog = new File( txLogDir, UTF8.decode( fileName ).trim() );
				if ( !fileSystem.fileExists( currentTxLog ) ) {
					throw logAndReturn( "TM startup failure", new TransactionFailureException( "Unable to start TM, " + "active tx log file[" + currentTxLog
							+ "] not found." ) );
				}
				txLog = new TxLog( currentTxLog, fileSystem, monitors );
				log.info( "TM opening log: " + currentTxLog );
			}
			else {
				if ( fileSystem.fileExists( new File( txLogDir, txLog1FileName ) ) || fileSystem.fileExists( new File( txLogDir, txLog2FileName ) ) ) {
					throw logAndReturn( "TM startup failure", new TransactionFailureException( "Unable to start TM, "
							+ "no active tx log file found but found either " + txLog1FileName + " or " + txLog2FileName
							+ " file, please set one of them as active or " + "remove them." ) );
				}
				ByteBuffer buf = ByteBuffer.wrap( txLog1FileName.getBytes( "UTF-8" ) );
				StoreChannel fc = fileSystem.open( logSwitcherFileName, "rw" );
				fc.write( buf );
				txLog = new TxLog( new File( txLogDir, txLog1FileName ), fileSystem, monitors );
				log.info( "TM new log: " + txLog1FileName );
				fc.force( true );
				fc.close();
			}
		}
		catch (IOException e) {
			throw logAndReturn( "TM startup failure", new TransactionFailureException( "Unable to start TM", e ) );
		}
	}

	private void closeLog() {
		if ( txLog != null ) {
			try {
				txLog.close();
				txLog = null;
				recovered = false;
			}
			catch (IOException e) {
				log.error( "Unable to close tx log[" + txLog.getName() + "]", e );
			}
		}
		log.info( "TM shutting down" );
	}

	@Override
	public TransactionState getTransactionState() {
		try {
			final TransactionState state = txStates.get( getTransaction() );
			return state != null ? state : TransactionState.NO_STATE;
		}
		catch (SystemException e) {
			throw log.errorReadingTransactionState( e );
		}

	}

	@Override
	public int getEventIdentifier() {
		return 0; // Undocumented what this is supposed to do
	}

	@Override
	public void init() throws Throwable {
		// NOOP, Tx Manager is not managed by Neo4j lifecycle
	}

	@Override
	public void start() throws Throwable {
		openLog();
		findPendingDatasources();
		dataSourceRegistrationListener = new TxManagerDataSourceRegistrationListener();
		xaDataSourceManager.addDataSourceRegistrationListener( dataSourceRegistrationListener );
	}

	private class TxManagerDataSourceRegistrationListener implements DataSourceRegistrationListener {

		@Override
		public void registeredDataSource(XaDataSource ds) {
			branches.put( new RecoveredBranchInfo( ds.getBranchId() ), true );
			boolean everythingRegistered = true;
			for ( boolean dsRegistered : branches.values() ) {
				everythingRegistered &= dsRegistered;
			}
			if ( everythingRegistered ) {
				doRecovery();
			}
		}

		@Override
		public void unregisteredDataSource(XaDataSource ds) {
			branches.put( new RecoveredBranchInfo( ds.getBranchId() ), false );
			boolean everythingUnregistered = true;
			for ( boolean dsRegistered : branches.values() ) {
				everythingUnregistered &= !dsRegistered;
			}
			if ( everythingUnregistered ) {
				closeLog();
			}
		}
	}

	/*
	 * We use a hash map to store the branch ids. byte[] however does not offer a useful implementation of equals() or
	 * hashCode(), so we need a wrapper that does that.
	 */
	private static final class RecoveredBranchInfo {

		final byte[] branchId;

		private RecoveredBranchInfo(byte[] branchId) {
			this.branchId = branchId;
		}

		@Override
		public int hashCode() {
			return Arrays.hashCode( branchId );
		}

		@Override
		public boolean equals(Object obj) {
			if ( obj == null || obj.getClass() != RecoveredBranchInfo.class ) {
				return false;
			}
			return Arrays.equals( branchId, ( (RecoveredBranchInfo) obj ).branchId );
		}
	}

	@Override
	public void stop() throws Throwable {
		xaDataSourceManager.removeDataSourceRegistrationListener( dataSourceRegistrationListener );
		closeLog();
	}

	@Override
	public void shutdown() throws Throwable {
		// NOOP, Tx Manager is not managed by Neo4j lifecycle
	}
}
