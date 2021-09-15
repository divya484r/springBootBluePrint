package com.sample.phylon.s3;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.AmazonWebServiceRequest;
import com.amazonaws.HttpMethod;
import com.amazonaws.regions.Region;
import com.amazonaws.services.cloudtrail.model.S3BucketDoesNotExistException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.S3ClientOptions;
import com.amazonaws.services.s3.S3ResponseMetadata;
import com.amazonaws.services.s3.model.*;
import com.amazonaws.util.IOUtils;
import com.google.common.io.ByteStreams;
import com.netflix.config.ConfigurationManager;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.configuration.AbstractConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * A local S3 client where versions, owners, regions, and several other features are ignored.
 */
@edu.umd.cs.findbugs.annotations.SuppressWarnings( "OBL_UNSATISFIED_OBLIGATION" )
public class LocalS3Client implements AmazonS3 {
    private static final Logger LOGGER = LoggerFactory.getLogger(LocalS3Client.class);
    private static final AbstractConfiguration CONFIG = ConfigurationManager.getConfigInstance();

    protected static final String RESCAN_CONFIG_PROPERTY = "com.sample.s3.local.rescan";
    protected static final String WRITE_CONFIG_PROPERTY = "com.sample.s3.local.write";
    protected static final String RESCAN_FREQUENCY_CONFIG_PROPERTY = "com.sample.s3.local.rescan.frequency";

    private Map<String,LocalBucket> buckets = new HashMap<>();

    private String initPath;
    private ScheduledExecutorService fileScanService = Executors.newScheduledThreadPool(1);

    /**
     * Default constructor.
     */
    public LocalS3Client() {
        if (CONFIG.getBoolean(RESCAN_CONFIG_PROPERTY, false)) {
            int rescanFrequencySeconds = CONFIG.getInt(RESCAN_FREQUENCY_CONFIG_PROPERTY, 30);
            fileScanService.scheduleAtFixedRate(new Runnable() {
                @Override
                public void run() {
                    rescanFileSystem();
                }
            }, rescanFrequencySeconds, rescanFrequencySeconds, TimeUnit.SECONDS);
        }
    }

    private void rescanFileSystem() {
        LOGGER.info("Refreshing buckets from file system");
        initializeBuckets();
    }

    /**
     * Constructor that initializes the client with data found at initPath.
     *
     * initPath should point to a directory that contains sub-directories. Each sub-directory will represent
     * an S3 bucket. Every file within any bucket directory or any bucket's sub-directories will represent an
     * asset in S3.
     *
     * @param initPath a path to a directory containing data that should be accessible from the S3 API.
     */
    public LocalS3Client(String initPath) {
        this();
        this.initPath = initPath;
        initializeBuckets();
    }

    /**
     * Scan the given path, creating buckets from any sub-directories in the initPath provided (which must be
     * a directory).
     *
     * @throws IllegalArgumentException if initPath does not point to a directory.
     */
    private void initializeBuckets() {
        if (initPath != null) {
            File root = new File(initPath);
            if (root == null || !root.isDirectory()) {
                throw new IllegalArgumentException("Path to local S3 assets is not a directory.");
            }

            for (String filename : root.list()) {
                File f = new File(root, filename);
                if (f.isDirectory()) {
                    LOGGER.info("Found local S3 bucket called " + f.getName());

                    LocalBucket bucket = new LocalBucket(f.getName());
                    buckets.put(bucket.getName(), bucket);
                    addFilesToBucket(bucket.getName(), f, null);
                }
            }
        } else {
            LOGGER.info("No local S3 assets configured.");
        }

    }

    /**
     * Create an S3 asset for each file inside directory and its sub-directories.
     *
     * @param bucketName the bucket where the asset should be created (which must already exist).
     * @param directory the directory where the S3 assets are located.
     * @param root the path to append to each asset (indicating all parent folders up to this point).
     */
    private void addFilesToBucket(String bucketName, File directory, String root) {
        for (String filename : directory.list()) {
            File file = new File(directory, filename);
            String path = root == null ? file.getName() : root + "/" + file.getName();
            if (file.isDirectory()) {
                addFilesToBucket(bucketName, file, path);
            } else {
                LOGGER.info("Found local S3 asset in bucket " + bucketName + " at key " + path);
                putObject(new PutObjectRequest(bucketName, path, file));
            }
        }
    }

    @Override
    public void setEndpoint(String endpoint) {
        throw new UnsupportedOperationException("This operation is not supported in local S3 implementation.");
    }

    @Override
    public void setRegion(Region region) throws IllegalArgumentException {
        throw new UnsupportedOperationException("Regions are not supported in local S3 implementation.");
    }

    @Override
    public void setS3ClientOptions(S3ClientOptions s3ClientOptions) {
        throw new UnsupportedOperationException("Client Options are not supported in local S3 implementation.");
    }

    @Override
    public void changeObjectStorageClass(String bucketName, String key, StorageClass storageClass) throws AmazonClientException {
        throw new UnsupportedOperationException("Storage classes are not supported in local S3 implementation.");
    }

    @Override
    public void setObjectRedirectLocation(String bucket, String key, String newRedirectLocation) throws AmazonClientException {
        throw new UnsupportedOperationException("Redirects are not supported in local S3 implementation.");
    }

    @Override
    public ObjectListing listObjects(String bucketName) throws AmazonClientException {
        return listObjects(new ListObjectsRequest(bucketName, null, null, null, null));
    }

    @Override
    public ObjectListing listObjects(String bucketName, String prefix) throws AmazonClientException {
        return listObjects(new ListObjectsRequest(bucketName, prefix, null, null, null));
    }

    @Override
    public ObjectListing listObjects(ListObjectsRequest request) throws AmazonClientException {
        List<S3ObjectSummary> summaries = new ArrayList<>();
        LocalBucket bucket = buckets.get(request.getBucketName());

        if (bucket != null) {
            String prefix = request.getPrefix();
            for (LocalS3Object object : bucket.getEntries().values()) {
                // bail if the object's key doesn't start with the prefix
                if (null == prefix || !object.getKey().startsWith(prefix))
                    continue;
                S3ObjectSummary summary = new S3ObjectSummary();
                summary.setBucketName(object.getBucketName());
                summary.setKey(object.getKey());
                summary.setETag(object.getObjectMetadata().getETag());
                summary.setSize(object.getBytes().length);
                summaries.add(summary);
            }
        }

        return new LocalObjectListing(request.getBucketName(), summaries);
    }

    @Override
    public ListObjectsV2Result listObjectsV2(String bucketName) throws AmazonClientException, AmazonServiceException {
        throw new UnsupportedOperationException("Feature not implemented in " + this.getClass().getName());
    }

    @Override
    public ListObjectsV2Result listObjectsV2(String bucketName, String prefix) throws AmazonClientException, AmazonServiceException {
        throw new UnsupportedOperationException("Feature not implemented in " + this.getClass().getName());
    }

    @Override
    public ListObjectsV2Result listObjectsV2(ListObjectsV2Request listObjectsV2Request) throws AmazonClientException, AmazonServiceException {
        throw new UnsupportedOperationException("Feature not implemented in " + this.getClass().getName());
    }

    @Override
    public ObjectListing listNextBatchOfObjects(ObjectListing objectListing) throws AmazonClientException {
        throw new UnsupportedOperationException("Versioning is not supported in local S3 implementation.");
    }

    @Override
    public ObjectListing listNextBatchOfObjects(ListNextBatchOfObjectsRequest listNextBatchOfObjectsRequest) throws AmazonClientException, AmazonServiceException {
        throw new UnsupportedOperationException("Versioning is not supported in local S3 implementation.");
    }

    @Override
    public VersionListing listVersions(String bucketName, String key) throws AmazonClientException {
        throw new UnsupportedOperationException("Versioning is not supported in local S3 implementation.");
    }

    @Override
    public VersionListing listNextBatchOfVersions(VersionListing versionListing) throws AmazonClientException {
        throw new UnsupportedOperationException("Versioning is not supported in local S3 implementation.");
    }

    @Override
    public VersionListing listNextBatchOfVersions(ListNextBatchOfVersionsRequest listNextBatchOfVersionsRequest) throws AmazonClientException, AmazonServiceException {
        throw new UnsupportedOperationException("Versioning is not supported in local S3 implementation.");
    }

    @Override
    public VersionListing listVersions(String bucketName, String prefix, String keyMarker, String versionIdMarker,
                                       String delimiter, Integer maxKeys) throws AmazonClientException {
        throw new UnsupportedOperationException("Versioning is not supported in local S3 implementation.");
    }

    @Override
    public VersionListing listVersions(ListVersionsRequest request) throws AmazonClientException {
        throw new UnsupportedOperationException("Versioning is not supported in local S3 implementation.");
    }

    @Override
    public Owner getS3AccountOwner() throws AmazonClientException {
        return null;
    }

    @Override
    public Owner getS3AccountOwner(GetS3AccountOwnerRequest getS3AccountOwnerRequest) throws AmazonClientException, AmazonServiceException {
        throw new UnsupportedOperationException("Feature not implemented in " + this.getClass().getName());
    }

    @Override
    public boolean doesBucketExist(String bucketName) throws AmazonClientException {
        return buckets.containsKey(bucketName);
    }

    @Override
    public HeadBucketResult headBucket(HeadBucketRequest headBucketRequest) throws AmazonClientException, AmazonServiceException {
        throw new UnsupportedOperationException("Feature not implemented in " + this.getClass().getName());
    }

    @Override
    public List<Bucket> listBuckets() throws AmazonClientException {
        return listBuckets(new ListBucketsRequest());
    }

    @Override
    public List<Bucket> listBuckets(ListBucketsRequest request) throws AmazonClientException {
        List<Bucket> bucketList = new ArrayList<>(buckets.size());
        for (String name : buckets.keySet()) {
            bucketList.add(new Bucket(name));
        }
        return bucketList;
    }

    @Override
    public String getBucketLocation(String bucketName) throws AmazonClientException {
        return getBucketLocation(new GetBucketLocationRequest(bucketName));
    }

    @Override
    public String getBucketLocation(GetBucketLocationRequest request) throws AmazonClientException {
        throw new UnsupportedOperationException("Regions are not supported in local S3 implementation.");
    }

    @Override
    public Bucket createBucket(CreateBucketRequest request) throws AmazonClientException {
        LocalBucket bucket = new LocalBucket(request.getBucketName());
        buckets.put(request.getBucketName(), bucket);
        return bucket;
    }

    @Override
    public Bucket createBucket(String bucketName) throws AmazonClientException {
        return createBucket(new CreateBucketRequest(bucketName));
    }

    @Override
    public Bucket createBucket(String bucketName, com.amazonaws.services.s3.model.Region region) throws AmazonClientException {
        return createBucket(new CreateBucketRequest(bucketName));
    }

    @Override
    public Bucket createBucket(String bucketName, String region) throws AmazonClientException {
        return createBucket(new CreateBucketRequest(bucketName));
    }

    @Override
    public AccessControlList getObjectAcl(String bucketName, String key) throws AmazonClientException {
        throw new UnsupportedOperationException("Access Control Lists are not supported in local S3 implementation.");
    }

    @Override
    public AccessControlList getObjectAcl(String bucketName, String key, String versionId) throws AmazonClientException {
        throw new UnsupportedOperationException("Access Control Lists are not supported in local S3 implementation.");
    }

    @Override
    public AccessControlList getObjectAcl(GetObjectAclRequest getObjectAclRequest) throws AmazonClientException, AmazonServiceException {
        throw new UnsupportedOperationException("Access Control Lists are not supported in local S3 implementation.");
    }

    @Override
    public void setObjectAcl(String bucketName, String key, AccessControlList acl) throws AmazonClientException {
        throw new UnsupportedOperationException("Access Control Lists are not supported in local S3 implementation.");
    }

    @Override
    public void setObjectAcl(String bucketName, String key, CannedAccessControlList acl) throws AmazonClientException {
        throw new UnsupportedOperationException("Access Control Lists are not supported in local S3 implementation.");
    }

    @Override
    public void setObjectAcl(String bucketName, String key, String versionId, AccessControlList acl) throws AmazonClientException {
        throw new UnsupportedOperationException("Access Control Lists are not supported in local S3 implementation.");
    }

    @Override
    public void setObjectAcl(String bucketName, String key, String versionId, CannedAccessControlList acl) throws AmazonClientException {
        throw new UnsupportedOperationException("Access Control Lists are not supported in local S3 implementation.");
    }

    @Override
    public void setObjectAcl(SetObjectAclRequest request) throws AmazonClientException {
        throw new UnsupportedOperationException("Access Control Lists are not supported in local S3 implementation.");
    }

    @Override
    public AccessControlList getBucketAcl(String bucketName) throws AmazonClientException {
        throw new UnsupportedOperationException("Access Control Lists are not supported in local S3 implementation.");
    }

    @Override
    public void setBucketAcl(SetBucketAclRequest request) throws AmazonClientException {
        throw new UnsupportedOperationException("Access Control Lists are not supported in local S3 implementation.");
    }

    @Override
    public AccessControlList getBucketAcl(GetBucketAclRequest request) throws AmazonClientException {
        throw new UnsupportedOperationException("Access Control Lists are not supported in local S3 implementation.");
    }

    @Override
    public void setBucketAcl(String bucketName, AccessControlList accessControlList) throws AmazonClientException {
        throw new UnsupportedOperationException("Access Control Lists are not supported in local S3 implementation.");
    }

    @Override
    public void setBucketAcl(String bucketName, CannedAccessControlList cannedAccessControlList) throws AmazonClientException {
        throw new UnsupportedOperationException("Access Control Lists are not supported in local S3 implementation.");
    }

    @Override
    public ObjectMetadata getObjectMetadata(String bucketName, String key) throws AmazonClientException {
        return getObjectMetadata(new GetObjectMetadataRequest(bucketName, key));
    }

    @Override
    public ObjectMetadata getObjectMetadata(GetObjectMetadataRequest request) throws AmazonClientException {
        S3Object object = getObject(new GetObjectRequest(request.getBucketName(), request.getKey()));
        return object != null ? object.getObjectMetadata() : null;
    }

    @Override
    public S3Object getObject(String bucketName, String key) throws AmazonClientException {
        return getObject(new GetObjectRequest(bucketName, key));
    }

    @Override
    public S3Object getObject(GetObjectRequest request) throws AmazonClientException {
        LocalBucket bucket = buckets.get(request.getBucketName());
        if (bucket == null) {
            throw new S3BucketDoesNotExistException("Bucket doesn't exist. bucket=" + request.getBucketName());
        }

        return bucket.getEntries().get(request.getKey());
    }

    @Override
    public ObjectMetadata getObject(GetObjectRequest getObjectRequest, File file) throws AmazonClientException {
        throw new UnsupportedOperationException("Saving to the file system is not supported in local S3 implementation.");
    }

    @Override
    public String getObjectAsString(String bucketName, String key) throws AmazonServiceException, AmazonClientException {
        throw new UnsupportedOperationException("Feature not implemented in " + this.getClass().getName());
    }

    @Override
    public void deleteBucket(DeleteBucketRequest request) throws AmazonClientException {
        buckets.remove(request.getBucketName());
    }

    @Override
    public void deleteBucket(String bucketName) throws AmazonClientException {
        deleteBucket(new DeleteBucketRequest(bucketName));
    }

    @Override
    public PutObjectResult putObject(PutObjectRequest request) throws AmazonClientException {

        LocalBucket bucket = buckets.get(request.getBucketName());
        if (bucket == null) {
            throw new S3BucketDoesNotExistException("Bucket doesn't exist. bucket=" + request.getBucketName());
        }

        InputStream stream = request.getInputStream();
        if (stream == null) {
            try {
                stream = new FileInputStream(request.getFile());
            } catch (FileNotFoundException e) {
                LOGGER.error("Error putting file in S3.", e);
            }
        }

        LocalS3Object object = new LocalS3Object();
        object.setObjectContent(stream);
        object.setBucketName(request.getBucketName());
        object.setKey(request.getKey());

        ObjectMetadata metadata = request.getMetadata() != null ? request.getMetadata() : new ObjectMetadata();
        String md5 = DigestUtils.md5Hex(object.getBytes());
        metadata.setContentMD5(md5);
        metadata.setHeader("ETag", md5);
        object.setObjectMetadata(metadata);

        bucket.getEntries().put(request.getKey(), object);

        if (CONFIG.getBoolean(WRITE_CONFIG_PROPERTY, false) && initPath != null){
            File outputFile = new File(this.initPath + File.separator + bucket.getName() + File.separator + request.getKey());

            try (BufferedOutputStream outputStream = new BufferedOutputStream(new FileOutputStream(outputFile));
                 BufferedInputStream inputStream = new BufferedInputStream(object.getObjectContent())) {

                ByteStreams.copy(inputStream, outputStream);
                outputStream.flush();

            } catch (IOException e) {
                LOGGER.error(String.format("Problem persisting file: '%s'", outputFile.getName()), e);
            }
        }

        PutObjectResult result = new PutObjectResult();
        result.setContentMd5(metadata.getContentMD5());
        result.setETag(metadata.getETag());
        result.setExpirationTime(metadata.getExpirationTime());
        result.setExpirationTimeRuleId(metadata.getExpirationTimeRuleId());
        result.setVersionId(metadata.getVersionId());

        return result;
    }

    @Override
    public PutObjectResult putObject(String bucketName, String key, File file) throws AmazonClientException {
        return putObject(new PutObjectRequest(bucketName, key, file));
    }

    @Override
    public PutObjectResult putObject(String bucketName, String key, InputStream inputStream, ObjectMetadata objectMetadata) throws AmazonClientException {
        return putObject(new PutObjectRequest(bucketName, key, inputStream, objectMetadata));
    }

    @Override
    public PutObjectResult putObject(String bucketName, String key, String content) throws AmazonServiceException, AmazonClientException {
        final ObjectMetadata objectMetadata = new ObjectMetadata();
        objectMetadata.setContentEncoding(StandardCharsets.UTF_8.name());
        return putObject(new PutObjectRequest(bucketName, key, new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)), objectMetadata));
    }

    @Override
    public CopyObjectResult copyObject(String sourceBucketName, String sourceKey, String destinationBucketName, String destinationKey) throws AmazonClientException {
        return copyObject(new CopyObjectRequest(sourceBucketName, sourceKey, destinationBucketName, destinationKey));
    }

    @Override
    public CopyObjectResult copyObject(CopyObjectRequest request) throws AmazonClientException {
        CopyObjectResult result = null;
        LocalS3Object sourceObject = (LocalS3Object) getObject(new GetObjectRequest(request.getSourceBucketName(), request.getSourceKey()));
        LocalBucket destinationBucket = buckets.get(request.getDestinationBucketName());
        if (destinationBucket == null) {
            throw new S3BucketDoesNotExistException("Destination bucket doesn't exist. bucket=" + request.getDestinationBucketName());
        }

        if (sourceObject != null) {
            LocalS3Object destinationObject = sourceObject.clone();
            destinationObject.setBucketName(request.getDestinationBucketName());
            destinationObject.setKey(request.getDestinationKey());
            destinationBucket.getEntries().put(request.getDestinationKey(), destinationObject);
            result = new CopyObjectResult();
            result.setETag(destinationObject.getObjectMetadata().getETag());
        }
        return result;
    }

    @Override
    public CopyPartResult copyPart(CopyPartRequest request) throws AmazonClientException {
        throw new UnsupportedOperationException("copyPart is not supported in local S3 implementation.");
    }

    @Override
    public void deleteObject(String bucketName, String key) throws AmazonClientException {
        deleteObject(new DeleteObjectRequest(bucketName, key));
    }

    @Override
    public void deleteObject(DeleteObjectRequest request) throws AmazonClientException {
        LocalBucket bucket = buckets.get(request.getBucketName());
        if (bucket == null) {
            throw new S3BucketDoesNotExistException("Bucket doesn't exist. bucket=" + request.getBucketName());
        } else {
            bucket.getEntries().remove(request.getKey());

            if (CONFIG.getBoolean(WRITE_CONFIG_PROPERTY, false)) {
                File fileToDelete = new File(this.initPath + File.separator + bucket.getName() + File.separator + request.getKey());
                if (!fileToDelete.delete()) {
                    LOGGER.error(String.format("Failed to delete file: '%s'", fileToDelete.getName()));
                }
            }
        }
    }

    @Override
    public DeleteObjectsResult deleteObjects(DeleteObjectsRequest request) throws AmazonClientException {
        List<DeleteObjectsResult.DeletedObject> deleted = new ArrayList<>(request.getKeys().size());
        for (DeleteObjectsRequest.KeyVersion key : request.getKeys()) {
            deleteObject(new DeleteObjectRequest(request.getBucketName(), key.getKey()));

            DeleteObjectsResult.DeletedObject object = new DeleteObjectsResult.DeletedObject();
            object.setKey(key.getKey());

            deleted.add(new DeleteObjectsResult.DeletedObject());
        }
        return new DeleteObjectsResult(deleted);
    }

    @Override
    public void deleteVersion(String bucketName, String key, String version) throws AmazonClientException {
        deleteVersion(new DeleteVersionRequest(bucketName, key, version));
    }

    @Override
    public void deleteVersion(DeleteVersionRequest request) throws AmazonClientException {
        throw new UnsupportedOperationException("Versioning is not supported in local S3 implementation.");
    }

    @Override
    public BucketLoggingConfiguration getBucketLoggingConfiguration(String bucketName) throws AmazonClientException {
        // Bucket logging is disabled in this implementation
        return new BucketLoggingConfiguration(null, null);
    }

    @Override
    public BucketLoggingConfiguration getBucketLoggingConfiguration(GetBucketLoggingConfigurationRequest getBucketLoggingConfigurationRequest) throws AmazonClientException, AmazonServiceException {
        // Bucket logging is disabled in this implementation
        return new BucketLoggingConfiguration(null, null);
    }

    @Override
    public void setBucketLoggingConfiguration(SetBucketLoggingConfigurationRequest request) throws AmazonClientException {
        throw new UnsupportedOperationException("Bucket Logging is not supported in local S3 implementation.");
    }

    @Override
    public BucketVersioningConfiguration getBucketVersioningConfiguration(String bucketName) throws AmazonClientException {
        return new BucketVersioningConfiguration(BucketVersioningConfiguration.OFF);
    }

    @Override
    public BucketVersioningConfiguration getBucketVersioningConfiguration(GetBucketVersioningConfigurationRequest getBucketVersioningConfigurationRequest) throws AmazonClientException, AmazonServiceException {
        return new BucketVersioningConfiguration(BucketVersioningConfiguration.OFF);
    }

    @Override
    public void setBucketVersioningConfiguration(SetBucketVersioningConfigurationRequest request) throws AmazonClientException {
        throw new UnsupportedOperationException("Versioning is not supported in local S3 implementation.");
    }

    @Override
    public BucketLifecycleConfiguration getBucketLifecycleConfiguration(String bucketName) {
        throw new UnsupportedOperationException("Bucket Lifecycles are not supported in local S3 implementation.");
    }

    @Override
    public BucketLifecycleConfiguration getBucketLifecycleConfiguration(GetBucketLifecycleConfigurationRequest getBucketLifecycleConfigurationRequest) {
        throw new UnsupportedOperationException("Bucket Lifecycles are not supported in local S3 implementation.");
    }

    @Override
    public void setBucketLifecycleConfiguration(String bucketName, BucketLifecycleConfiguration bucketLifecycleConfiguration) {
        throw new UnsupportedOperationException("Bucket Lifecycles are not supported in local S3 implementation.");
    }

    @Override
    public void setBucketLifecycleConfiguration(SetBucketLifecycleConfigurationRequest request) {
        throw new UnsupportedOperationException("Bucket Lifecycles are not supported in local S3 implementation.");
    }

    @Override
    public void deleteBucketLifecycleConfiguration(String bucketName) {
        deleteBucketLifecycleConfiguration(new DeleteBucketLifecycleConfigurationRequest(bucketName));
    }

    @Override
    public void deleteBucketLifecycleConfiguration(DeleteBucketLifecycleConfigurationRequest deleteBucketLifecycleConfigurationRequest) {
        // This operation is allowed because the lifecycle never existed.
    }

    @Override
    public BucketCrossOriginConfiguration getBucketCrossOriginConfiguration(String bucketName) {
        throw new UnsupportedOperationException("CORS is not supported in local S3 implementation.");
    }

    @Override
    public BucketCrossOriginConfiguration getBucketCrossOriginConfiguration(GetBucketCrossOriginConfigurationRequest getBucketCrossOriginConfigurationRequest) {
        throw new UnsupportedOperationException("CORS is not supported in local S3 implementation.");
    }

    @Override
    public void setBucketCrossOriginConfiguration(String bucketName, BucketCrossOriginConfiguration bucketCrossOriginConfiguration) {
        throw new UnsupportedOperationException("CORS is not supported in local S3 implementation.");
    }

    @Override
    public void setBucketCrossOriginConfiguration(SetBucketCrossOriginConfigurationRequest request) {
        throw new UnsupportedOperationException("CORS is not supported in local S3 implementation.");
    }

    @Override
    public void deleteBucketCrossOriginConfiguration(String bucketName) {
        deleteBucketCrossOriginConfiguration(new DeleteBucketCrossOriginConfigurationRequest(bucketName));
    }

    @Override
    public void deleteBucketCrossOriginConfiguration(DeleteBucketCrossOriginConfigurationRequest request) {
        // This operation is allowed because the configuration never existed.
    }

    @Override
    public BucketTaggingConfiguration getBucketTaggingConfiguration(String bucketName) {
        return new BucketTaggingConfiguration(new ArrayList<TagSet>(0));
    }

    @Override
    public BucketTaggingConfiguration getBucketTaggingConfiguration(GetBucketTaggingConfigurationRequest getBucketTaggingConfigurationRequest) {
        return new BucketTaggingConfiguration(new ArrayList<TagSet>(0));
    }

    @Override
    public void setBucketTaggingConfiguration(String bucketName, BucketTaggingConfiguration bucketTaggingConfiguration) {
        setBucketTaggingConfiguration(new SetBucketTaggingConfigurationRequest(bucketName, bucketTaggingConfiguration));
    }

    @Override
    public void setBucketTaggingConfiguration(SetBucketTaggingConfigurationRequest request) {
        throw new UnsupportedOperationException("Bucket Tagging is not supported in local S3 implementation.");
    }

    @Override
    public void deleteBucketTaggingConfiguration(String bucketName) {
        deleteBucketTaggingConfiguration(new DeleteBucketTaggingConfigurationRequest(bucketName));
    }

    @Override
    public void deleteBucketTaggingConfiguration(DeleteBucketTaggingConfigurationRequest request) {
        // This operation is allowed because the configuration never existed.
    }

    @Override
    public BucketNotificationConfiguration getBucketNotificationConfiguration(String bucketName) throws AmazonClientException {
        return new BucketNotificationConfiguration();
    }

    @Override
    public BucketNotificationConfiguration getBucketNotificationConfiguration(GetBucketNotificationConfigurationRequest getBucketNotificationConfigurationRequest) throws AmazonClientException, AmazonServiceException {
        return new BucketNotificationConfiguration();
    }

    @Override
    public void setBucketNotificationConfiguration(SetBucketNotificationConfigurationRequest request) throws AmazonClientException {
        throw new UnsupportedOperationException("Bucket Notifications are not supported in local S3 implementation.");
    }

    @Override
    public void setBucketNotificationConfiguration(String bucketName, BucketNotificationConfiguration bucketNotificationConfiguration) throws AmazonClientException {
        setBucketNotificationConfiguration(new SetBucketNotificationConfigurationRequest(bucketName, bucketNotificationConfiguration));
    }

    @Override
    public BucketWebsiteConfiguration getBucketWebsiteConfiguration(String bucketName) throws AmazonClientException {
        throw new UnsupportedOperationException("Bucket-as-a-Website is not supported in local S3 implementation.");
    }

    @Override
    public BucketWebsiteConfiguration getBucketWebsiteConfiguration(GetBucketWebsiteConfigurationRequest request) throws AmazonClientException {
        throw new UnsupportedOperationException("Bucket-as-a-Website is not supported in local S3 implementation.");
    }

    @Override
    public void setBucketWebsiteConfiguration(String bucketName, BucketWebsiteConfiguration bucketWebsiteConfiguration) throws AmazonClientException {
        throw new UnsupportedOperationException("Bucket-as-a-Website is not supported in local S3 implementation.");
    }

    @Override
    public void setBucketWebsiteConfiguration(SetBucketWebsiteConfigurationRequest request) throws AmazonClientException {
        throw new UnsupportedOperationException("Bucket-as-a-Website is not supported in local S3 implementation.");
    }

    @Override
    public void deleteBucketWebsiteConfiguration(String bucketName) throws AmazonClientException {
        deleteBucketWebsiteConfiguration(new DeleteBucketWebsiteConfigurationRequest(bucketName));
    }

    @Override
    public void deleteBucketWebsiteConfiguration(DeleteBucketWebsiteConfigurationRequest request) throws AmazonClientException {
        // This operation is allowed because the configuration never existed.
    }

    @Override
    public BucketPolicy getBucketPolicy(String bucketName) throws AmazonClientException {
        return getBucketPolicy(new GetBucketPolicyRequest(bucketName));
    }

    @Override
    public BucketPolicy getBucketPolicy(GetBucketPolicyRequest request) throws AmazonClientException {
        return new BucketPolicy();
    }

    @Override
    public void setBucketPolicy(String bucketName, String policyText) throws AmazonClientException {
        setBucketPolicy(new SetBucketPolicyRequest(bucketName, policyText));
    }

    @Override
    public void setBucketPolicy(SetBucketPolicyRequest request) throws AmazonClientException {
        throw new UnsupportedOperationException("Bucket Policies are not supported in local S3 implementation.");
    }

    @Override
    public void deleteBucketPolicy(String bucketName) throws AmazonClientException {
        deleteBucketPolicy(new DeleteBucketPolicyRequest(bucketName));
    }

    @Override
    public void deleteBucketPolicy(DeleteBucketPolicyRequest request) throws AmazonClientException {
        // This operation is allowed because the policy never existed.
    }

    @Override
    public URL generatePresignedUrl(String bucketName, String key, Date expiration) throws AmazonClientException {
        throw new UnsupportedOperationException("Presigned URLs are not supported in local S3 implementation.");
    }

    @Override
    public URL generatePresignedUrl(String bucketName, String key, Date expiration, HttpMethod httpMethod) throws AmazonClientException {
        throw new UnsupportedOperationException("Presigned URLs are not supported in local S3 implementation.");
    }

    @Override
    public URL generatePresignedUrl(GeneratePresignedUrlRequest request) throws AmazonClientException {
        throw new UnsupportedOperationException("Presigned URLs are not supported in local S3 implementation.");
    }

    @Override
    public InitiateMultipartUploadResult initiateMultipartUpload(InitiateMultipartUploadRequest request) throws AmazonClientException {
        throw new UnsupportedOperationException("Multipart uploads are not supported in local S3 implementation.");
    }

    @Override
    public UploadPartResult uploadPart(UploadPartRequest request) throws AmazonClientException {
        throw new UnsupportedOperationException("Multipart uploads are not supported in local S3 implementation.");
    }

    @Override
    public PartListing listParts(ListPartsRequest request) throws AmazonClientException {
        return null;
    }

    @Override
    public void abortMultipartUpload(AbortMultipartUploadRequest request) throws AmazonClientException {
        // There was no multipart upload to abort
    }

    @Override
    public CompleteMultipartUploadResult completeMultipartUpload(CompleteMultipartUploadRequest request) throws AmazonClientException {
        throw new UnsupportedOperationException("Multipart uploads are not supported in local S3 implementation.");
    }

    @Override
    public MultipartUploadListing listMultipartUploads(ListMultipartUploadsRequest request) throws AmazonClientException {
        throw new UnsupportedOperationException("Multipart uploads are not supported in local S3 implementation.");
    }

    @Override
    public S3ResponseMetadata getCachedResponseMetadata(AmazonWebServiceRequest request) {
        // Cached response meta data is never available in this implementation
        return null;
    }

    @Override
    public void restoreObject(RestoreObjectRequest request) throws AmazonServiceException {
        throw new UnsupportedOperationException("Classes are not supported in local S3 implementation.");
    }

    @Override
    public void restoreObject(String bucketName, String key, int expirationInDays) throws AmazonServiceException {
        restoreObject(new RestoreObjectRequest(bucketName, key, expirationInDays));
    }

    @Override
    public void enableRequesterPays(String bucketName) throws AmazonClientException {
        throw new UnsupportedOperationException("Requester Pays is not supported in local S3 implementation.");
    }

    @Override
    public void disableRequesterPays(String bucketName) throws AmazonClientException {
        // It's already disabled
    }

    @Override
    public boolean isRequesterPaysEnabled(String bucketName) throws AmazonClientException {
        return false;
    }

    @Override
    public void setBucketReplicationConfiguration(String bucketName, BucketReplicationConfiguration configuration) throws AmazonServiceException, AmazonClientException {
        throw new UnsupportedOperationException("Feature not implemented in " + this.getClass().getName());
    }

    @Override
    public void setBucketReplicationConfiguration(SetBucketReplicationConfigurationRequest setBucketReplicationConfigurationRequest) throws AmazonServiceException, AmazonClientException {
        throw new UnsupportedOperationException("Feature not implemented in " + this.getClass().getName());
    }

    @Override
    public BucketReplicationConfiguration getBucketReplicationConfiguration(String bucketName) throws AmazonServiceException, AmazonClientException {
        throw new UnsupportedOperationException("Feature not implemented in " + this.getClass().getName());
    }

    @Override
    public BucketReplicationConfiguration getBucketReplicationConfiguration(GetBucketReplicationConfigurationRequest getBucketReplicationConfigurationRequest) throws AmazonServiceException, AmazonClientException {
        throw new UnsupportedOperationException("Feature not implemented in " + this.getClass().getName());
    }

    @Override
    public void deleteBucketReplicationConfiguration(String bucketName) throws AmazonServiceException, AmazonClientException {
        throw new UnsupportedOperationException("Feature not implemented in " + this.getClass().getName());
    }

    @Override
    public void deleteBucketReplicationConfiguration(DeleteBucketReplicationConfigurationRequest request) throws AmazonServiceException, AmazonClientException {
        throw new UnsupportedOperationException("Feature not implemented in " + this.getClass().getName());
    }

    @Override
    public boolean doesObjectExist(String bucketName, String objectName) throws AmazonServiceException, AmazonClientException {
        return buckets.containsKey(bucketName) && buckets.get(bucketName).getEntries().containsKey(objectName);
    }

    @Override
    public BucketAccelerateConfiguration getBucketAccelerateConfiguration(String bucket) throws AmazonServiceException, AmazonClientException {
        throw new UnsupportedOperationException("Feature not implemented in " + this.getClass().getName());
    }

    @Override
    public BucketAccelerateConfiguration getBucketAccelerateConfiguration(GetBucketAccelerateConfigurationRequest getBucketAccelerateConfigurationRequest) throws AmazonServiceException, AmazonClientException {
        throw new UnsupportedOperationException("Feature not implemented in " + this.getClass().getName());
    }

    @Override
    public void setBucketAccelerateConfiguration(String bucketName, BucketAccelerateConfiguration accelerateConfiguration) throws AmazonServiceException, AmazonClientException {
        throw new UnsupportedOperationException("Feature not implemented in " + this.getClass().getName());
    }

    @Override
    public void setBucketAccelerateConfiguration(SetBucketAccelerateConfigurationRequest setBucketAccelerateConfigurationRequest) throws AmazonServiceException, AmazonClientException {
        throw new UnsupportedOperationException("Feature not implemented in " + this.getClass().getName());
    }

    @Override
    public com.amazonaws.services.s3.model.Region getRegion() {
        return com.amazonaws.services.s3.model.Region.US_West;
    }

    @Override
    public URL getUrl(String bucketName, String key) {
        throw new UnsupportedOperationException("Feature not implemented in " + this.getClass().getName());
    }

    /**
     * An implementation of {@link Bucket} that exposes the assets within as a Map.
     */
    private static class LocalBucket extends Bucket {
        private Map<String, LocalS3Object> entries = new HashMap<>();

        public LocalBucket(String name) {
            super(name);
        }

        public Map<String, LocalS3Object> getEntries() {
            return entries;
        }
    }

    /**
     * An implementation of {@link ObjectListing} that allows the summaries to
     * be set directly in the constructor.
     */
    private static class LocalObjectListing extends ObjectListing {
        private List<S3ObjectSummary> summaries;

        public LocalObjectListing(String bucketName, List<S3ObjectSummary> summaries) {
            super();
            this.summaries = summaries;
            this.setBucketName(bucketName);
        }

        @Override
        public List<S3ObjectSummary> getObjectSummaries() {
            return summaries;
        }

    }

    /**
     * An implementation of {@link S3Object} that stores the contents as a byte
     * array and makes it accessible.
     */
    private static class LocalS3Object extends S3Object implements Cloneable {
        private byte[] contents;

        public LocalS3Object() {
        }

        /**
         * Copy constructor.
         *
         * @param from
         */
        private LocalS3Object(LocalS3Object from) {
            super();
            setBytes(from.getBytes());
            setBucketName(from.getBucketName());
            setKey(from.getKey());
            setRedirectLocation(from.getRedirectLocation());
            setObjectMetadata(from.getObjectMetadata().clone());
        }

        public byte[] getBytes() {
            return contents;
        }

        public void setBytes(byte[] contents) {
            this.contents = contents;
        }

        @Override
        public S3ObjectInputStream getObjectContent()
        {
            return new S3ObjectInputStream(new ByteArrayInputStream(contents), null);
        }

        @Override
        public void setObjectContent(S3ObjectInputStream objectContent)
        {
            setObjectContent((InputStream) objectContent);
        }

        @Override
        public void setObjectContent(InputStream objectContent)
        {
            try {
                setBytes(IOUtils.toByteArray(objectContent));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public LocalS3Object clone() {
            return new LocalS3Object(this);
        }
    }

}