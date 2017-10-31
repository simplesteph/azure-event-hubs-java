/*
 * Copyright (c) Microsoft. All rights reserved.
 * Licensed under the MIT license. See LICENSE file in the project root for full license information.
 */

package com.microsoft.azure.eventprocessorhost;

import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

/***
 * If you wish to have EventProcessorHost store leases somewhere other than Azure Storage,
 * you can write your own lease manager using this interface.  
 *
 * The Azure Storage managers use the same storage for both lease and checkpoints, so both
 * interfaces are implemented by the same class. You are free to do the same thing if you have
 * a unified store for both types of data.
 * 
 * This interface does not specify initialization methods because we have no way of knowing what
 * information your implementation will require.
 */
public interface ILeaseManager
{
	/**
	 * Allows a lease manager implementation to specify to PartitionManager how often it should
	 * scan leases and renew them. In order to redistribute leases in a timely fashion after a host
	 * ceases operating, we recommend a relatively short interval, such as ten seconds. Obviously it
	 * should be less than half of the lease length, to prevent accidental expiration.
	 * 
	 * @return  The sleep interval between scans, specified in milliseconds.
	 */
	public int getLeaseRenewIntervalInMilliseconds();
	
	/**
	 * Mostly useful for testing.
	 * 
	 * @return  Duration of a lease before it expires unless renewed.
	 */
	public int getLeaseDurationInMilliseconds();
	
	/**
	 * Does the lease store exist?
	 * 
	 * @return true if it does, false if not
	 * @throws ExceptionWithAction with action EventProcessorHostActionStrings.CHECKING_LEASE_STORE on error
	 */
    public boolean leaseStoreExists() throws ExceptionWithAction;

    /**
     * Create the lease store if it does not exist, do nothing if it does exist.
     * 
     * @return Void so it can be called from lambdas
     * @throws ExceptionWithAction with action EventProcessorHostActionStrings.CREATING_LEASE_STORE on error
     */
    public Void createLeaseStoreIfNotExists() throws ExceptionWithAction;
    
    /**
     * Not used by EventProcessorHost, but a convenient function to have for testing.
     * 
     * @return true if the lease store was deleted successfully, false if not
     */
    public boolean deleteLeaseStore();

    /**
     * Return the lease info for the specified partition. Can return null if no lease has been
     * created in the store for the specified partition.
     * 
     * @param partitionId  id of partition to get lease for
     * @return lease info for the partition, or null
     * @throws ExceptionWithAction with action EventProcessorHostActionStrings.GETTING_LEASE on error
     */
    public Lease getLease(String partitionId) throws ExceptionWithAction;
    
    /**
     * Return the lease info for all partitions.
     * 
     * A typical implementation could just call getLease() on all partitions.
     * 
     * @return  List of lease info.
     */
    public ArrayList<CompletableFuture<Lease>> getAllLeases() throws ExceptionWithAction;

    /**
     * Create in the store the lease info for the given partition, if it does not exist. Do nothing if it does exist
     * in the store already.
     * 
     * @param partitionId  id of partition to create lease info for
     * @return the existing or newly-created lease info for the partition
     * @throws ExceptionWithAction with action EventProcessorHostActionString.CREATING_LEASE on error 
     */
    public Lease createLeaseIfNotExists(String partitionId) throws ExceptionWithAction;

    /**
     * Delete the lease info for the given partition from the store. If there is no stored lease for the given partition,
     * that is treated as success.
     *  
     * @param lease  Lease info for the desired partition as previously obtained from getLease()
     * @return void
     * @throws ExceptionWithAction with action EventProcessorHostActionString.DELETING_LEASE on error 
     */
    public void deleteLease(Lease lease) throws ExceptionWithAction;

    /**
     * Acquire the lease on the desired partition for this EventProcessorHost.
     * 
     * Note that it is legal to acquire a lease that is already owned by another host. Lease-stealing is how
     * partitions are redistributed when additional hosts are started.
     * 
     * @param lease  Lease info for the desired partition as previously obtained from getLease()
     * @return  true if the lease was acquired successfully, false if it failed because another host acquired it.
     * @throws ExceptionWithAction with action EventProcessorHostActionStrings.ACQUIRING_LEASE on error
     */
    public boolean acquireLease(Lease lease) throws ExceptionWithAction;

    /**
     * Renew a lease currently held by this host.
     * 
     * If the lease has been taken by another host (either stolen or after expiration) or explicitly released, it is
     * not possible to renew it. You will have to call getLease() and then acquireLease() again. With the Azure Storage-based
     * default implementation, it IS possible to renew an expired lease (that has not been taken by another host), so other
     * implementations can allow that.
     * 
     * @param lease  Lease to be renewed
     * @return  true if the lease was renewed, false if the lease was lost and could not renewed
     * @throws ExceptionWithAction with action EventProcessorHostActionString.RENEWING_LEASE on error
     */
    public boolean renewLease(Lease lease) throws ExceptionWithAction;

    /**
     * Give up a lease currently held by this host.
     * 
     * If the lease has expired or been taken by another host, releasing it is unnecessary, and will fail if attempted.
     * 
     * @param lease  Lease to be given up
     * @return  true if the lease was released, false if the lease could not be released
     * @throws ExceptionWithAction with action EventProcessorHostActionString.RELEASING_LEASE on error
     */
    public boolean releaseLease(Lease lease) throws ExceptionWithAction;

    /**
     * Update the store with the information in the provided lease.
     * 
     * It is necessary to currently hold a lease in order to update it. If the lease has been stolen, or expired, or
     * released, it cannot be updated. Updating should renew the lease before performing the update to avoid lease
     * expiration during the process.
     * 
     * @param lease  New lease info to be stored
     * @return  true if the updated was performed successfully, false if lease was lost and could not be updated
     * @throws ExceptionWithAction with action EventProcessorHostActionStrings.UPDATING_LEASE on error
     */
    public boolean updateLease(Lease lease) throws ExceptionWithAction;
}
